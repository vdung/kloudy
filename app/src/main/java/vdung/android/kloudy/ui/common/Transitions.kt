package vdung.android.kloudy.ui.common

import android.view.View
import android.view.ViewTreeObserver
import androidx.core.app.SharedElementCallback
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView

object Transitions {

    interface ElementProvider {
        fun getSharedElements(names: List<String>): Map<String, View>
    }

    fun to(elementProvider: ElementProvider): SharedElementCallback {
        return object : SharedElementCallback() {
            override fun onMapSharedElements(names: MutableList<String>, sharedElements: MutableMap<String, View>) {
                sharedElements.putAll(elementProvider.getSharedElements(names))
            }
        }
    }

    fun to(recyclerView: RecyclerView, getPosition: () -> Int, getViews: (RecyclerView.ViewHolder, List<String>) -> Map<String, View>): SharedElementCallback {
        return to(recyclerView.asElementProvider(getPosition, getViews))
    }
}

fun RecyclerView.asElementProvider(getPosition: () -> Int, getViews: (RecyclerView.ViewHolder, List<String>) -> Map<String, View>): Transitions.ElementProvider {
    return object : Transitions.ElementProvider {
        override fun getSharedElements(names: List<String>): Map<String, View> {
            val viewHolder = findViewHolderForAdapterPosition(getPosition()) ?: return emptyMap()
            return getViews(viewHolder, names)
        }
    }
}

fun RecyclerView.executePendingTransaction(activity: FragmentActivity, position: Int) {
    layoutManager?.run {
        val viewAtPosition = findViewByPosition(position)
        if (viewAtPosition == null || isViewPartiallyVisible(viewAtPosition, false, true)) {
            scrollToPosition(position)
        }
    }

    viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
        override fun onPreDraw(): Boolean {
            viewTreeObserver.removeOnPreDrawListener(this)
            activity.supportStartPostponedEnterTransition()
            return true
        }
    })
}