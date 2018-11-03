package vdung.android.kloudy.ui.timeline

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import androidx.core.view.ViewCompat
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.get
import androidx.navigation.ActivityNavigator
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.android.support.DaggerFragment
import vdung.android.kloudy.R
import vdung.android.kloudy.data.model.FileEntry
import vdung.android.kloudy.data.nextcloud.NextcloudConfig
import vdung.android.kloudy.databinding.TimelineFragmentBinding
import vdung.android.kloudy.databinding.TimelineGridCellBinding
import vdung.android.kloudy.di.GlideApp
import vdung.android.kloudy.ui.main.OnActivityReenterListener
import vdung.android.kloudy.ui.pages.PagerActivityDirections
import vdung.android.kloudy.ui.common.*
import javax.inject.Inject

class TimelineFragment : DaggerFragment(), OnActivityReenterListener {

    private lateinit var viewModel: TimelineViewModel
    private lateinit var binding: TimelineFragmentBinding

    @Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    internal lateinit var nextcloudConfig: NextcloudConfig

    private var hasPendingTransition = false

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        getParent<OnActivityReenterListener.Host>()?.addListener(this)
    }

    override fun onDetach() {
        getParent<OnActivityReenterListener.Host>()?.removeListener(this)
        super.onDetach()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(requireActivity(), viewModelFactory).get()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = TimelineFragmentBinding.inflate(inflater, container, false).also {
            it.setLifecycleOwner(this)
        }

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val spanSize = resources.getInteger(R.integer.span_size)
        val timelineAdapter = TimelineAdapter()
        val lifecycleOwner = viewLifecycleOwner
        val headerAdapter = HeaderAdapter(timelineAdapter).apply {
            setHasStableIds(true)
        }
        val gridLayoutManager = GridLayoutManager(activity, spanSize).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    val headers = this@TimelineFragment.viewModel.loadFilesResult.value?.second
                            ?: return 1

                    return if (headers.indexOfKey(position) >= 0) spanSize
                    else 1
                }
            }
        }

        binding.apply {
            setLifecycleOwner(viewLifecycleOwner)
            viewModel = this@TimelineFragment.viewModel.apply {
                loadFilesResult.observe(lifecycleOwner, Observer { result ->
                    val headers = result.second
                    val headerInfos = SparseArray<HeaderAdapter.HeaderInfo<DataBindingViewHolder<ViewDataBinding>>>(headers.size())
                    for (i in 0 until headers.size()) {
                        headerInfos.put(headers.keyAt(i), Header(headers.valueAt(i)))
                    }

                    timelineAdapter.submitList(result.first.value) {
                        headerAdapter.setHeaders(headerInfos)
                    }
                })

                entryClickEvent.observe(lifecycleOwner, Observer {
                    showDetail(headerAdapter, it)
                })

                isRefreshing.observe(lifecycleOwner, Observer {
                    swipeRefreshLayout.isRefreshing = it
                })
            }

            recyclerView.apply {
                adapter = headerAdapter
                layoutManager = gridLayoutManager
            }

            requireActivity().setExitSharedElementCallback(Transitions.to(recyclerView, { headerAdapter.getActualPosition(this@TimelineFragment.viewModel.currentPage) }) { viewHolder, names ->
                viewHolder.let { it as? DataBindingViewHolder<*> }
                        ?.let { it.binding as? TimelineGridCellBinding }
                        ?.run {
                            mapOf(names[0] to imageView)
                        }
                        ?: emptyMap()
            })
        }

        if (hasPendingTransition) {
            executePendingTransition()
        }
    }

    override fun onActivityReenter(resultCode: Int, data: Intent?) {
        data?.apply {
            requireActivity().supportPostponeEnterTransition()
            viewModel.currentPage = getIntExtra("CURRENT_PAGE", viewModel.currentPage)

            if (::binding.isInitialized) {
                executePendingTransition()
            } else {
                hasPendingTransition = true
            }
        }
    }

    private fun executePendingTransition() {
        val headerAdapter = binding.recyclerView.adapter as? HeaderAdapter ?: return

        val position = headerAdapter.getActualPosition(viewModel.currentPage)

        binding.recyclerView.run {
            executePendingTransaction(requireActivity(), position)
            hasPendingTransition = false
        }
    }

    private inline fun <reified VH : RecyclerView.ViewHolder> showDetail(headerAdapter: HeaderAdapter<VH>, fileId: Int) {
        binding.apply {
            recyclerView.findViewHolderForItemId(fileId.toLong())
                    ?.let { it as? DataBindingViewHolder<*> }
                    ?.run {
                        val position = headerAdapter.getAdapterPosition(adapterPosition)
                        binding.let { it as? TimelineGridCellBinding }
                                ?.run {
                                    this@TimelineFragment.viewModel.currentPage = position

                                    val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                                            requireActivity(),
                                            Pair(imageView, imageView.transitionName)
                                    )
                                    val extras = ActivityNavigator.Extras(options)
                                    val direction = PagerActivityDirections.actionShowPhoto("", position)
                                    findNavController().navigate(direction, extras)
                                }
                    }
        }
    }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<FileEntry>() {
            override fun areItemsTheSame(oldItem: FileEntry, newItem: FileEntry): Boolean {
                return oldItem.fileId == newItem.fileId
            }

            override fun areContentsTheSame(oldItem: FileEntry, newItem: FileEntry): Boolean {
                return oldItem == newItem
            }
        }
    }


    private inner class TimelineAdapter : DataBindingPagedListAdapter<FileEntry, ViewDataBinding>(diffCallback) {

        init {
            setHasStableIds(true)
        }

        override fun onBindViewHolder(holder: DataBindingViewHolder<ViewDataBinding>, position: Int) {
            super.onBindViewHolder(holder, position)
            val cell = getItem(position)
            if (holder.binding is TimelineGridCellBinding && cell is FileEntry) {
                holder.binding.apply {
                    ViewCompat.setTransitionName(imageView, cell.url)

                    videoIndicator.visibility = if (cell.contentType.startsWith("video")) View.VISIBLE else View.GONE

                    eventListener = viewModel

                    GlideApp.with(holder.itemView)
                            .load(viewModel.thumbnailUrl(cell))
                            .into(imageView)
                }
            }
        }

        override fun getItemId(position: Int): Long {
            return getItem(position)?.fileId?.toLong() ?: RecyclerView.NO_ID
        }

        override fun getLayoutId(position: Int): Int {
            return R.layout.timeline_grid_cell
        }
    }
}

class Header(val title: String, override val itemViewType: Int = R.layout.timeline_grid_header) : HeaderAdapter.HeaderInfo<DataBindingViewHolder<ViewDataBinding>> {

    override val itemId: Long = -title.hashCode().toLong()

    override fun bindTo(viewHolder: DataBindingViewHolder<ViewDataBinding>) {
        viewHolder.bind(this)
    }
}