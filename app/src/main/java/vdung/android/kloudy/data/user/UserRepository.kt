package vdung.android.kloudy.data.user

import android.content.SharedPreferences
import android.util.Base64
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.Disposables
import org.reactivestreams.Publisher
import vdung.android.kloudy.data.marshall
import vdung.android.kloudy.data.unmarshall

class UserRepository(private val keyStoreWrapper: KeyStoreWrapper, private val sharedPreferences: SharedPreferences) {

    companion object {
        @JvmStatic
        private val PREFERENCE_KEY = "user"
    }

    private object Lock

    fun setUser(user: User) {
        synchronized(Lock) {
            sharedPreferences.edit()
                    .putString(PREFERENCE_KEY, Base64.encodeToString(keyStoreWrapper.encrypt(user.marshall()), 0))
                    .apply()
        }
    }

    fun getUser(): User? {
        return sharedPreferences.getString(PREFERENCE_KEY, null)?.let {
            unmarshall(keyStoreWrapper.decrypt(Base64.decode(it, 0)))
        }
    }

    fun currentUser(): Publisher<User> {
        return Flowable.create<User>({ emitter ->
            val observer = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == PREFERENCE_KEY) {
                    emitter.onNext(getUser() ?: User.NONE)
                }
            }

            sharedPreferences.registerOnSharedPreferenceChangeListener(observer)
            emitter.setDisposable(Disposables.fromAction {
                sharedPreferences.unregisterOnSharedPreferenceChangeListener(observer)
            })
        }, BackpressureStrategy.LATEST).startWith(getUser() ?: User.NONE)
    }

}