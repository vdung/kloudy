package vdung.android.kloudy.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.ViewModel
import vdung.android.kloudy.data.KloudyDatabase
import vdung.android.kloudy.data.model.User
import vdung.android.kloudy.ui.Result
import java.lang.Exception
import javax.inject.Inject

class NoUserException : Exception()

class MainViewModel @Inject constructor(
        database: KloudyDatabase
) : ViewModel() {

    private val userDao = database.userDao()

    val currentUser: LiveData<Result<User>> = LiveDataReactiveStreams.fromPublisher(
            userDao.getUsers().map { users ->
                users.firstOrNull { it.isCurrent }?.let {
                    Result.Success(it) as Result<User>
                } ?: Result.Error(NoUserException())
            }
    )
}