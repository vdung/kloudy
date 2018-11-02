package vdung.android.kloudy.ui.album


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import androidx.core.view.ViewCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.get
import androidx.navigation.ActivityNavigator
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.evernote.android.state.State
import com.evernote.android.state.StateSaver
import dagger.android.support.DaggerFragment
import vdung.android.kloudy.BR
import vdung.android.kloudy.R
import vdung.android.kloudy.data.model.FileEntry
import vdung.android.kloudy.databinding.AlbumFragmentBinding
import vdung.android.kloudy.databinding.AlbumPhotoCellBinding
import vdung.android.kloudy.di.GlideApp
import vdung.android.kloudy.ui.main.OnActivityReenterListener
import vdung.android.kloudy.ui.pages.PagerActivityDirections
import vdung.android.kloudy.ui.common.DataBindingPagedListAdapter
import vdung.android.kloudy.ui.common.DataBindingViewHolder
import vdung.android.kloudy.ui.common.Transitions
import vdung.android.kloudy.ui.common.executePendingTransaction
import javax.inject.Inject

class AlbumFragment : DaggerFragment(), OnActivityReenterListener {

    private lateinit var binding: AlbumFragmentBinding
    private lateinit var viewModel: AlbumViewModel

    @Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory

    private val directory: String by lazy {
        AlbumFragmentArgs.fromBundle(arguments).directory
    }

    @State
    var currentEntry = 0

    private var hasPendingTransition = false

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        (requireActivity() as? OnActivityReenterListener.Host)?.addListener(this)
    }

    override fun onDetach() {
        (requireActivity() as? OnActivityReenterListener.Host)?.removeListener(this)
        super.onDetach()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StateSaver.restoreInstanceState(this, savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = AlbumFragmentBinding.inflate(inflater, container, false).also {
            it.setLifecycleOwner(this)
        }
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(requireActivity(), viewModelFactory).get()

        val spanSize = resources.getInteger(R.integer.span_size)
        val albumAdapter = AlbumAdapter()
        binding.recyclerView.apply {
            adapter = albumAdapter
            layoutManager = GridLayoutManager(requireActivity(), spanSize)
        }

        viewModel.apply {
            albumFiles(directory).observe(viewLifecycleOwner, Observer {
                albumAdapter.submitList(it)
            })

            entryClickEvent.observe(viewLifecycleOwner, Observer {
                showEntry(it)
            })
        }

        requireActivity().setExitSharedElementCallback(Transitions.to(binding.recyclerView, { currentEntry }) { viewHolder, names ->
            viewHolder.let { it as? DataBindingViewHolder<AlbumPhotoCellBinding> }
                    ?.run { mapOf(names[0] to binding.imageView) }
                    ?: emptyMap()
        })

        if (hasPendingTransition) {
            executePendingTransition()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        StateSaver.saveInstanceState(this, outState)
    }

    override fun onActivityReenter(resultCode: Int, data: Intent?) {
        data?.apply {
            requireActivity().supportPostponeEnterTransition()
            currentEntry = getIntExtra("CURRENT_PAGE", currentEntry)

            if (::binding.isInitialized) {
                executePendingTransition()
            } else {
                hasPendingTransition = true
            }
        }
    }

    private fun executePendingTransition() {
        binding.recyclerView.run {
            executePendingTransaction(requireActivity(), currentEntry)
            hasPendingTransition = false
        }
    }

    private fun showEntry(fileId: Int) {
        binding.apply {
            recyclerView.findViewHolderForItemId(fileId.toLong())
                    ?.let { it as? DataBindingViewHolder<AlbumPhotoCellBinding> }
                    ?.run {
                        currentEntry = adapterPosition
                        exitTransition = TransitionInflater.from(requireContext())
                                .inflateTransition(R.transition.fade_transition)
                                .apply {
                                    excludeTarget(binding.imageView, true)
                                }

                        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                                requireActivity(),
                                Pair(binding.imageView, binding.imageView.transitionName)
                        )
                        val extras = ActivityNavigator.Extras(options)
                        val direction = PagerActivityDirections.actionShowPhoto(directory, adapterPosition)
                        findNavController().navigate(direction, extras)
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

    private inner class AlbumAdapter : DataBindingPagedListAdapter<FileEntry, AlbumPhotoCellBinding>(diffCallback) {

        init {
            setHasStableIds(true)
        }

        override fun onBindViewHolder(holder: DataBindingViewHolder<AlbumPhotoCellBinding>, position: Int) {
            super.onBindViewHolder(holder, position)
            holder.binding.setVariable(BR.eventListener, viewModel)
            val item = getItem(position) ?: return

            ViewCompat.setTransitionName(holder.binding.imageView, item.url)

            GlideApp.with(holder.itemView)
                    .load(viewModel.thumbnailUrl(item))
                    .into(holder.binding.imageView)
        }

        override fun getLayoutId(position: Int): Int {
            return R.layout.album_photo_cell
        }

        override fun getItemId(position: Int): Long {
            return getItem(position)?.fileId?.toLong() ?: RecyclerView.NO_ID
        }
    }
}