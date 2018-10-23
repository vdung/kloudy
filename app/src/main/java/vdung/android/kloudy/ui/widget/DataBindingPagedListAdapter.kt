package vdung.android.kloudy.ui.widget

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil

abstract class DataBindingPagedListAdapter<T, VDB : ViewDataBinding>(diffUtilCallback: DiffUtil.ItemCallback<T>) : PagedListAdapter<T, DataBindingViewHolder<VDB>>(diffUtilCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataBindingViewHolder<VDB> {
        val inflater = LayoutInflater.from(parent.context)
        val binding = DataBindingUtil.inflate<VDB>(inflater, viewType, parent, false)
        return DataBindingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DataBindingViewHolder<VDB>, position: Int) {
        getItem(position)?.let { holder.bind(it!!) }
    }

    override fun getItemViewType(position: Int): Int {
        return getLayoutId(position)
    }

    abstract fun getLayoutId(position: Int): Int
}