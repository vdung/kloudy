package vdung.android.kloudy.ui.main

import android.content.Intent
import android.os.Bundle
import android.util.SparseArray
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import com.evernote.android.state.State
import dagger.android.support.DaggerAppCompatActivity
import vdung.android.kloudy.R
import vdung.android.kloudy.data.user.User
import vdung.android.kloudy.ui.login.LoginFragment
import javax.inject.Inject

class MainActivity : DaggerAppCompatActivity(), OnActivityReenterListener.Host {

    private val reenterHost = OnActivityReenterListener.HostDelegate()

    private lateinit var viewModel: MainViewModel
    @Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory

    @State
    var isShowingMainUI = false
    @State
    var fragmentStates = SparseArray<Fragment.SavedState>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(MainViewModel::class.java)
        viewModel.currentUser.observe(this, Observer { user ->
            when (user) {
                User.NONE -> showLoginUI()
                else -> {
                    if (savedInstanceState == null && !isShowingMainUI) {
                        showMainUI()
                    }
                }
            }
        })
    }

    override fun onSupportNavigateUp() = findNavController(R.id.main_content).navigateUp()

    override fun onBackPressed() {
        if (!findNavController(R.id.main_content).navigateUp()) {
            super.onBackPressed()
        }
    }

    override fun onActivityReenter(resultCode: Int, data: Intent?) {
        super.onActivityReenter(resultCode, data)
        reenterHost.onActivityReenter(resultCode, data)
    }

    override fun addListener(listener: OnActivityReenterListener) {
        reenterHost.addListener(listener)
    }

    override fun removeListener(listener: OnActivityReenterListener) {
        reenterHost.removeListener(listener)
    }

    private fun showMainUI() {
        isShowingMainUI = true
        showFragment(MainFragment.newInstance())

    }

    private fun showLoginUI() {
        isShowingMainUI = false
        showFragment(LoginFragment.newInstance())
    }

    private fun showFragment(fragment: Fragment, config: (FragmentTransaction.() -> Unit)? = null) {
        supportFragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .apply {
                    config?.let { apply(it) }
                }
                .commitNow()
    }
}
