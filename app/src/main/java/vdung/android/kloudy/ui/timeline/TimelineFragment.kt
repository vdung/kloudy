package vdung.android.kloudy.ui.timeline

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.transition.TransitionInflater
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.SharedElementCallback
import androidx.core.view.ViewCompat
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView.NO_ID
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import dagger.android.support.DaggerFragment
import vdung.android.kloudy.R
import vdung.android.kloudy.data.Result
import vdung.android.kloudy.data.model.FileEntry
import vdung.android.kloudy.data.nextcloud.NextcloudConfig
import vdung.android.kloudy.databinding.MainGridCellBinding
import vdung.android.kloudy.databinding.TimelineFragmentBinding
import vdung.android.kloudy.di.GlideApp
import vdung.android.kloudy.ui.pages.PagerFragment
import vdung.android.kloudy.ui.widget.DataBindingPagedListAdapter
import vdung.android.kloudy.ui.widget.DataBindingViewHolder
import vdung.android.kloudy.ui.widget.HeaderAdapter
import javax.inject.Inject

class TimelineFragment : DaggerFragment() {
    companion object {
        fun newInstance() = TimelineFragment()
    }

    internal lateinit var timelineViewModel: TimelineViewModel
    private lateinit var binding: TimelineFragmentBinding
    private lateinit var headerAdapter: HeaderAdapter<DataBindingViewHolder<ViewDataBinding>>

    @Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    internal lateinit var nextcloudConfig: NextcloudConfig

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = TimelineFragmentBinding.inflate(inflater, container, false).also {
            it.setLifecycleOwner(this)
        }

        postponeEnterTransition()

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        timelineViewModel = ViewModelProviders.of(requireActivity(), viewModelFactory).get(TimelineViewModel::class.java)

        setExitSharedElementCallback(object : SharedElementCallback() {
            override fun onMapSharedElements(names: MutableList<String>, sharedElements: MutableMap<String, View>) {
                binding.recyclerView.findViewHolderForAdapterPosition(headerAdapter.getActualPosition(timelineViewModel.currentPage))
                        ?.let {
                            it as? DataBindingViewHolder<*>
                        }
                        ?.let {
                            it.binding as? MainGridCellBinding
                        }
                        ?.run {
                            println(timelineViewModel.currentPage)
                            sharedElements[names[0]] = imageView
                        }
            }
        })

        val spanSize = 3
        val timelineAdapter = TimelineAdapter(this)
        headerAdapter = HeaderAdapter(timelineAdapter).apply {
            setHasStableIds(true)
        }
        binding.apply {
            viewModel = timelineViewModel

            recyclerView.apply {
                adapter = headerAdapter
                layoutManager = GridLayoutManager(activity, spanSize).apply {
                    spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                        override fun getSpanSize(position: Int): Int {
                            val headers = timelineViewModel.loadFilesResult.value?.second
                                    ?: return 1

                            return if (headers.indexOfKey(position) >= 0) spanSize
                            else 1
                        }
                    }
                }
                addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
                    override fun onLayoutChange(v: View?, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {
                        removeOnLayoutChangeListener(this)

                        val layoutManager = layoutManager ?: return

                        val actualPosition = headerAdapter.getActualPosition(timelineViewModel.currentPage)
                        val viewAtPosition = layoutManager.findViewByPosition(actualPosition)
                        if (viewAtPosition == null || layoutManager.isViewPartiallyVisible(viewAtPosition, false, true)) {
                            recyclerView.post {
                                layoutManager.scrollToPosition(actualPosition)
                            }
                        }
                    }
                })
            }
        }

        val lifecycleOwner = viewLifecycleOwner
        timelineViewModel.apply {
            loadFilesResult.observe(lifecycleOwner, Observer { result ->
                when (result) {
                    is Result.Error<*> -> {
                        Toast.makeText(activity, result.error.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        val headers = result.second
                        val headerInfos = SparseArray<HeaderAdapter.HeaderInfo<DataBindingViewHolder<ViewDataBinding>>>(headers.size())
                        for (i in 0 until headers.size()) {
                            headerInfos.put(headers.keyAt(i), Header(headers.valueAt(i)))
                        }

                        timelineAdapter.submitList(result.first.value)
                        headerAdapter.setHeaders(headerInfos)
                    }
                }
            })

            entryClickEvent.observe(lifecycleOwner, Observer {
                showDetail(it)
            })
        }
    }

    private fun showDetail(fileId: Int) {
        binding.recyclerView.findViewHolderForItemId(fileId.toLong())
                ?.let { it as? DataBindingViewHolder<*> }
                ?.also { timelineViewModel.currentPage = headerAdapter.getAdapterPosition(it.adapterPosition) }
                ?.let { it.binding as? MainGridCellBinding }
                ?.run {
                    fragmentManager?.let {
                        val detailFragment = PagerFragment.newInstance().apply {
                            sharedElementEnterTransition = TransitionInflater.from(this@TimelineFragment.requireContext()).inflateTransition(R.transition.detail_transition)
                            sharedElementReturnTransition = TransitionInflater.from(this@TimelineFragment.requireContext()).inflateTransition(R.transition.detail_transition)
                        }

                        exitTransition = TransitionInflater.from(requireContext())
                                .inflateTransition(R.transition.fade_transition)
                                .apply {
                                    excludeTarget(imageView, true)
                                }

                        it.beginTransaction()
                                .setReorderingAllowed(true)
                                .addSharedElement(imageView, imageView.transitionName)
                                .replace(R.id.container, detailFragment)
                                .addToBackStack(null)
                                .commit()
                    }
                }
    }
}

class Header(val title: String, override val itemViewType: Int = R.layout.main_grid_header) : HeaderAdapter.HeaderInfo<DataBindingViewHolder<ViewDataBinding>> {

    override val itemId: Long = -title.hashCode().toLong()

    override fun bindTo(viewHolder: DataBindingViewHolder<ViewDataBinding>) {
        viewHolder.bind(this)
    }
}

private class TimelineAdapter constructor(
        private val fragment: TimelineFragment
) : DataBindingPagedListAdapter<FileEntry, ViewDataBinding>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<FileEntry>() {
            override fun areItemsTheSame(oldItem: FileEntry, newItem: FileEntry): Boolean {
                return oldItem.fileId == newItem.fileId
            }

            override fun areContentsTheSame(oldItem: FileEntry, newItem: FileEntry): Boolean {
                return oldItem == newItem
            }
        }
    }

    init {
        setHasStableIds(true)
    }

    override fun onBindViewHolder(holder: DataBindingViewHolder<ViewDataBinding>, position: Int) {
        super.onBindViewHolder(holder, position)
        val cell = getItem(position)
        if (holder.binding is MainGridCellBinding && cell is FileEntry) {
            holder.binding.apply {
                ViewCompat.setTransitionName(imageView, cell.url)

                eventListener = fragment.timelineViewModel

                GlideApp.with(holder.itemView)
                        .load(fragment.timelineViewModel.thumbnailUrl(cell))
                        .also {
                            if (position == fragment.timelineViewModel.currentPage) {
                                it.listener(object : RequestListener<Drawable> {
                                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                                        fragment.startPostponedEnterTransition()
                                        return false
                                    }

                                    override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                                        fragment.startPostponedEnterTransition()
                                        return false
                                    }
                                })
                            }
                        }
                        .into(imageView)
            }
        }
    }

    override fun getItemId(position: Int): Long {
        return getItem(position)?.fileId?.toLong() ?: NO_ID
    }

    override fun getLayoutId(position: Int): Int {
        return R.layout.main_grid_cell
    }
}