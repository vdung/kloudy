package vdung.android.kloudy.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.ViewModel
import vdung.android.kloudy.data.user.User
import vdung.android.kloudy.data.user.UserRepository
import javax.inject.Inject

class NoUserException : Exception()

class MainViewModel @Inject constructor(
        userRepository: UserRepository
) : ViewModel() {

    val currentUser: LiveData<User> = LiveDataReactiveStreams.fromPublisher(userRepository.currentUser())
}