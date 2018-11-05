package vdung.android.kloudy.ui.login

import android.app.Activity
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import dagger.android.support.DaggerAppCompatActivity
import vdung.android.kloudy.data.Result
import javax.inject.Inject

class LoginActivity : DaggerAppCompatActivity() {

    private lateinit var viewModel: LoginViewModel
    @Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(android.R.id.content, LoginFragment.newInstance())
                    .commitNow()
        }

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(LoginViewModel::class.java)
        viewModel.loginResult.observe(this, Observer {
            when (it) {
                is Result.Success -> {
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
        })
    }
}