package vdung.android.kloudy.ui.login

import android.util.Patterns
import androidx.databinding.Bindable
import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import okhttp3.Credentials
import retrofit2.Retrofit
import vdung.android.kloudy.BR
import vdung.android.kloudy.data.nextcloud.NextcloudService
import vdung.android.kloudy.data.user.User
import vdung.android.kloudy.data.user.UserRepository
import vdung.android.kloudy.ui.common.ObservableViewModel
import javax.inject.Inject

class LoginViewModel @Inject constructor(
        private val userRepository: UserRepository,
        private val retrofitBuilder: Retrofit.Builder
) : ObservableViewModel() {
    var server: String = ""
        @Bindable get
        set (value) {
            field = value
            notifyPropertyChanged(BR.loginValid)
            notifyPropertyChanged(BR.serverError)
        }

    val serverError: String?
        @Bindable get() = when {
            !Patterns.WEB_URL.matcher(server).matches() -> ""
            !server.endsWith("/") -> "URL must end with '/'"
            else -> null
        }

    var username: String = ""
        @Bindable get
        set (value) {
            field = value
            notifyPropertyChanged(BR.loginValid)
        }

    var password: String = ""
        @Bindable get
        set (value) {
            field = value
            notifyPropertyChanged(BR.loginValid)
        }

    val isLoginValid: Boolean
        @Bindable get() = serverError == null && username.isNotEmpty() && password.isNotEmpty()

    private val loginErrorProcessor = PublishProcessor.create<Throwable>()
    val loginErrorEvent: LiveData<Throwable> = LiveDataReactiveStreams.fromPublisher(loginErrorProcessor)

    private val userProcessor = PublishProcessor.create<User>()
    val user: LiveData<User> = LiveDataReactiveStreams.fromPublisher(userProcessor)

    private val disposable = CompositeDisposable()

    init {
        disposable.add(userProcessor
                .observeOn(Schedulers.io())
                .subscribe({
                    userRepository.setUser(it)
                }, {
                    loginErrorProcessor.onNext(it)
                })
        )
    }

    override fun onCleared() {
        super.onCleared()
        disposable.clear()
    }

    fun login() {
        val service = retrofitBuilder
                .baseUrl(server)
                .build()
                .create(NextcloudService::class.java)

        disposable.add(service.ping(Credentials.basic(username, password))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    userProcessor.onNext(User(
                            username = username, password = password, server = server
                    ))
                }, {
                    loginErrorProcessor.onNext(it)
                }))
    }
}
