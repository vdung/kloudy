package vdung.android.kloudy.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import vdung.android.kloudy.BR

class DataBindingViewHolder<T : ViewDataBinding>(val binding: T) : RecyclerView.ViewHolder(binding.root) {
    fun bind(item: Any) {
        binding.setVariable(BR.item, item)
    }
}

abstract class DataBindingAdapter<T : Any, VDB : ViewDataBinding> : RecyclerView.Adapter<DataBindingViewHolder<VDB>>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataBindingViewHolder<VDB> {
        val inflater = LayoutInflater.from(parent.context)
        val binding = DataBindingUtil.inflate<VDB>(inflater, viewType, parent, false)
        return DataBindingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DataBindingViewHolder<VDB>, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemViewType(position: Int): Int {
        return getLayoutId(position)
    }

    abstract fun getLayoutId(position: Int): Int
    abstract fun getItem(position: Int): T
}