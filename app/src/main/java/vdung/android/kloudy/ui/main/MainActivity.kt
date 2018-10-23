package vdung.android.kloudy.ui.main

import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.evernote.android.state.State
import dagger.android.support.DaggerAppCompatActivity
import vdung.android.kloudy.R
import vdung.android.kloudy.data.model.User
import vdung.android.kloudy.ui.Result
import vdung.android.kloudy.ui.login.LoginFragment
import vdung.android.kloudy.ui.timeline.TimelineFragment
import javax.inject.Inject

class MainActivity : DaggerAppCompatActivity() {

    private lateinit var viewModel: MainViewModel
    @Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory

    @State
    var isShowingMainUI = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(MainViewModel::class.java)
        viewModel.currentUser.observe(this, Observer { result ->
            when (result) {
                is Result.Success<User> -> {
                    if (savedInstanceState == null && !isShowingMainUI) {
                        showMainUI()
                    }
                }
                else -> {
                    showLoginUI()
                }
            }
        })
    }

    private fun showMainUI() {
        isShowingMainUI = true
        supportFragmentManager.beginTransaction()
                .replace(R.id.container, TimelineFragment.newInstance())
                .commitNow()
    }

    private fun showLoginUI() {
        isShowingMainUI = false
        supportFragmentManager.beginTransaction()
                .replace(R.id.container, LoginFragment.newInstance())
                .commitNow()
    }

}
