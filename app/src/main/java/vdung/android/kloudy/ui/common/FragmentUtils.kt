package vdung.android.kloudy.ui.common

import androidx.fragment.app.Fragment

inline fun <reified T> Fragment.getParent(): T? {
    return parentFragment?.let { it as? T } ?: activity?.let { it as? T }
}