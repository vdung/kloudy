package vdung.android.kloudy.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.Toolbar
import androidx.core.view.get
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.evernote.android.state.State
import com.evernote.android.state.StateSaver
import dagger.android.support.DaggerFragment
import vdung.android.kloudy.R
import vdung.android.kloudy.databinding.MainFragmentBinding
import vdung.android.kloudy.ui.settings.SettingsActivity

class MainFragment : DaggerFragment(), Toolbar.OnMenuItemClickListener {
    internal lateinit var binding: MainFragmentBinding

    @State
    var currentPage = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StateSaver.restoreInstanceState(this, savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = MainFragmentBinding.inflate(inflater, container, false).also {
            it.setLifecycleOwner(this)
        }

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.apply {
            toolbar.inflateMenu(R.menu.main_menu)
            toolbar.setOnMenuItemClickListener(this@MainFragment)

            bottomNavigation.setOnNavigationItemSelectedListener {
                navigateToView(it.itemId)
                return@setOnNavigationItemSelectedListener true
            }

            if (savedInstanceState == null) {
                navigateToView(bottomNavigation.menu[0].itemId)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        StateSaver.saveInstanceState(this, outState)
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.navigation_settings -> {
                startActivity(Intent(requireActivity(), SettingsActivity::class.java))
                true
            }
            else -> false
        }
    }

    private fun navigateToView(menuItemId: Int) {
        val graphId = when (menuItemId) {
            R.id.navigation_timeline -> R.navigation.nav_graph_timeline
            R.id.navigation_album -> R.navigation.nav_graph_album
            else -> throw IllegalArgumentException()
        }
        childFragmentManager.apply {
            NavHostFragment.create(graphId).let { navHost ->
                beginTransaction()
                        .replace(R.id.main_content, navHost)
                        .setPrimaryNavigationFragment(navHost)
                        .runOnCommit {
                            binding.toolbar.setupWithNavController(navHost.navController)
                        }
                        .commit()
            }

            currentPage = menuItemId
        }
    }

    companion object {
        fun newInstance() = MainFragment()
    }
}
