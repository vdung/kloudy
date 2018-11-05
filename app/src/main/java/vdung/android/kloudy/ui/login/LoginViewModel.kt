package vdung.android.kloudy.ui.login

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Function4
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import okhttp3.Credentials
import org.reactivestreams.Publisher
import retrofit2.Retrofit
import vdung.android.kloudy.data.Result
import vdung.android.kloudy.data.nextcloud.NextcloudService
import vdung.android.kloudy.data.user.User
import vdung.android.kloudy.data.user.UserRepository
import javax.inject.Inject

class LoginViewModel @Inject constructor(
        private val userRepository: UserRepository,
        private val retrofitBuilder: Retrofit.Builder
) : ViewModel() {

    private val initialUser = userRepository.getUser() ?: User.NONE

    private val serverProcessor = BehaviorProcessor.createDefault(initialUser.server)
    val server = serverProcessor.toLiveData()
    fun onServerChange(server: String) {
        serverProcessor.onNext(server)
    }

    private val serverValidationPublisher = serverProcessor
            .map { it ->
                String
                when {
                    !Patterns.WEB_URL.matcher(it).matches() -> ""
                    !it.endsWith("/") -> "URL must end with '/'"
                    else -> ""
                }
            }
    val serverValidation = serverValidationPublisher.toLiveData()

    private val usernameProcessor = BehaviorProcessor.createDefault(initialUser.username)
    val username = usernameProcessor.toLiveData()
    fun onUsernameChange(server: String) {
        usernameProcessor.onNext(server)
    }

    private val passwordProcessor = BehaviorProcessor.createDefault("")
    var password = passwordProcessor.toLiveData()
    fun onPasswordChange(server: String) {
        passwordProcessor.onNext(server)
    }

    private val loginProcessor = PublishProcessor.create<Unit>()
    private val loginFlow = Flowable.switchOnNext(loginProcessor
            .withLatestFrom<String, String, String, Publisher<Result<User>>>(serverProcessor, usernameProcessor, passwordProcessor, Function4 { _, server, username, password ->
                val service = retrofitBuilder
                        .baseUrl(server)
                        .build()
                        .create(NextcloudService::class.java)

                service.ping(Credentials.basic(username, password))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .toFlowable()
                        .map { Result.Success(User(username = username, password = password, server = server)) as Result<User> }
                        .onErrorReturn { Result.Error(it, User.NONE) }
                        .startWith(Result.Pending(User.NONE))
            }))
            .doOnNext {
                when (it) {
                    is Result.Success -> {
                        userRepository.setUser(it.value)
                    }
                }
            }

    val loginResult: LiveData<Result<User>> = loginFlow.toLiveData()

    val isLoginAvailable = Flowable.combineLatest<String, String, String, Result<User>, Boolean>(
            serverValidationPublisher,
            usernameProcessor,
            passwordProcessor,
            loginFlow.startWith(Result.Success(initialUser)),
            Function4 { serverValidation, username, password, result ->
                serverValidation.isEmpty() && username.isNotEmpty() && password.isNotEmpty() && result !is Result.Pending
            }
    ).toLiveData()

    private val disposable = CompositeDisposable()

    override fun onCleared() {
        super.onCleared()
        disposable.clear()
    }

    fun login() {
        loginProcessor.onNext(Unit)
    }
}
