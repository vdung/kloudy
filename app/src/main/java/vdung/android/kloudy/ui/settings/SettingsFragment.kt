package vdung.android.kloudy.ui.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import vdung.android.kloudy.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
    }
}