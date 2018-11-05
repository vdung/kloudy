package vdung.android.kloudy.ui.settings

import android.content.Intent
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import vdung.android.kloudy.R
import vdung.android.kloudy.ui.login.LoginActivity

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
        findPreference("user").setOnPreferenceClickListener {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            true
        }
    }
}