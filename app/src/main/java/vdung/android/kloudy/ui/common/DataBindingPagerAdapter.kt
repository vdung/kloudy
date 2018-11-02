package vdung.android.kloudy.ui.common

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.viewpager.widget.PagerAdapter
import vdung.android.kloudy.BR

abstract class DataBindingPagerAdapter<Item, VDB : ViewDataBinding> : PagerAdapter() {
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val layoutId = getLayoutId(position)
        val inflater = LayoutInflater.from(container.context)
        val binding = DataBindingUtil.inflate<VDB>(inflater, layoutId, container, true)

        onBindItem(binding, position)

        return binding
    }

    open fun onBindItem(binding: VDB, position: Int) {
        binding.setVariable(BR.item, getItem(position))
    }

    override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
        @Suppress("UNCHECKED_CAST")
        (obj as VDB).let {
            container.removeView(it.root)
            it.unbind()
        }
    }

    override fun getItemPosition(`object`: Any): Int {
        return POSITION_NONE
    }

    override fun isViewFromObject(view: View, obj: Any): Boolean {
        @Suppress("UNCHECKED_CAST")
        return view == (obj as VDB).root
    }

    abstract fun getLayoutId(position: Int): Int
    abstract fun getItem(position: Int): Item
}