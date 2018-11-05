package vdung.android.kloudy.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import io.reactivex.Flowable
import vdung.android.kloudy.data.user.User
import vdung.android.kloudy.data.user.UserRepository
import javax.inject.Inject

class MainViewModel @Inject constructor(
        private val userRepository: UserRepository
) : ViewModel() {

    val currentUser: LiveData<User> get() = Flowable.fromPublisher(userRepository.currentUser()).distinctUntilChanged().toLiveData()
}