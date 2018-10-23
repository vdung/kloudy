package vdung.android.kloudy.ui.widget

import android.util.SparseArray
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView

class HeaderAdapter<VH : RecyclerView.ViewHolder>(
        private val adapter: RecyclerView.Adapter<VH>
) : RecyclerView.Adapter<VH>() {

    interface HeaderInfo<VH : RecyclerView.ViewHolder> {
        val itemId: Long
            get() = RecyclerView.NO_ID

        val itemViewType: Int

        fun bindTo(viewHolder: VH)
    }

    private var headers = SparseArray<HeaderInfo<VH>>()

    init {
        adapter.registerAdapterDataObserver(ForwardObserver())
    }

    fun setHeaders(newHeaders: SparseArray<HeaderInfo<VH>>) {
        val oldHeaders = headers
        headers = newHeaders.clone()
        notifyHeadersChanged(oldHeaders, newHeaders)
    }

    private fun notifyHeadersChanged(oldHeaders: SparseArray<HeaderInfo<VH>>, newHeaders: SparseArray<HeaderInfo<VH>>) {
        DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldHeaders.keyAt(oldItemPosition) == newHeaders.keyAt(newItemPosition)
            }

            override fun getOldListSize(): Int {
                return oldHeaders.size()
            }

            override fun getNewListSize(): Int {
                return newHeaders.size()
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldHeaders.valueAt(oldItemPosition) == newHeaders.valueAt(newItemPosition)
            }

        }).dispatchUpdatesTo(object : ListUpdateCallback {
            override fun onChanged(position: Int, count: Int, payload: Any?) {
                for (i in position until position + count) {
                    notifyItemChanged(oldHeaders.keyAt(i), payload)
                }
            }

            override fun onMoved(fromPosition: Int, toPosition: Int) {
                notifyItemMoved(oldHeaders.keyAt(fromPosition), newHeaders.keyAt(toPosition))
            }

            override fun onInserted(position: Int, count: Int) {
                for (i in position until position + count) {
                    notifyItemInserted(newHeaders.keyAt(i))
                }
            }

            override fun onRemoved(position: Int, count: Int) {
                for (i in position until position + count) {
                    notifyItemRemoved(oldHeaders.keyAt(i))
                }
            }
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return adapter.onCreateViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        headers.get(position)?.bindTo(holder)
                ?: adapter.onBindViewHolder(holder, getAdapterPosition(position))
    }

    override fun onViewRecycled(holder: VH) {
        adapter.onViewRecycled(holder)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        adapter.onAttachedToRecyclerView(recyclerView)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        adapter.onDetachedFromRecyclerView(recyclerView)
    }

    override fun onViewAttachedToWindow(holder: VH) {
        adapter.onViewAttachedToWindow(holder)
    }

    override fun onViewDetachedFromWindow(holder: VH) {
        adapter.onViewDetachedFromWindow(holder)
    }

    override fun onFailedToRecycleView(holder: VH): Boolean {
        return adapter.onFailedToRecycleView(holder)
    }

    override fun getItemCount(): Int {
        return headers.size() + adapter.itemCount
    }

    override fun getItemViewType(position: Int): Int {
        return headers.get(position)?.itemViewType
                ?: adapter.getItemViewType(getAdapterPosition(position))
    }

    override fun getItemId(position: Int): Long {
        return headers.get(position)?.itemId ?: getAdapterPosition(position).let {
            if (it < adapter.itemCount) adapter.getItemId(it) else RecyclerView.NO_ID
        }
    }

    fun getAdapterPosition(position: Int): Int {
        val numHeadersBefore = (0 until headers.size()).count { headers.keyAt(it) <= position }
        return position - numHeadersBefore
    }

    fun getActualPosition(position: Int): Int {
        val numHeadersBefore = (0 until headers.size()).count { headers.keyAt(it) - it <= position }
        return position + numHeadersBefore
    }

    private inner class ForwardObserver : RecyclerView.AdapterDataObserver() {


        override fun onChanged() {
            notifyDataSetChanged()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            notifyItemRangeChanged(getActualPosition(positionStart), itemCount, payload)
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            notifyItemRangeInserted(getActualPosition(positionStart), itemCount)
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            notifyItemRangeRemoved(getActualPosition(positionStart), itemCount)
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            if (itemCount != 1) {
                throw IllegalArgumentException("itemCount != 1 is not supported")
            }
            notifyItemMoved(getActualPosition(fromPosition), getActualPosition(toPosition))
        }
    }
}