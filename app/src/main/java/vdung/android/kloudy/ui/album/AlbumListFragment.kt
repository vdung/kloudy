package vdung.android.kloudy.ui.album

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.get
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import dagger.android.support.DaggerFragment
import vdung.android.kloudy.BR
import vdung.android.kloudy.R
import vdung.android.kloudy.data.model.DirectoryQueryResult
import vdung.android.kloudy.databinding.AlbumGridCellBinding
import vdung.android.kloudy.databinding.AlbumListFragmentBinding
import vdung.android.kloudy.di.GlideApp
import vdung.android.kloudy.ui.common.DataBindingPagedListAdapter
import vdung.android.kloudy.ui.common.DataBindingViewHolder
import javax.inject.Inject

class AlbumListFragment : DaggerFragment() {

    private lateinit var binding: AlbumListFragmentBinding
    private lateinit var viewModel: AlbumViewModel

    @Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = AlbumListFragmentBinding.inflate(inflater, container, false).also {
            it.setLifecycleOwner(this)
        }

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(requireActivity(), viewModelFactory).get()

        val spanSize = resources.getInteger(R.integer.span_size)
        val albumAdapter = AlbumListAdapter()
        binding.recyclerView.apply {
            adapter = albumAdapter
            layoutManager = GridLayoutManager(requireActivity(), spanSize)
        }

        viewModel.apply {
            albums.observe(viewLifecycleOwner, Observer {
                albumAdapter.submitList(it)
            })

            albumClickEvent.observe(viewLifecycleOwner, Observer {
                val direction = AlbumListFragmentDirections.actionShowAlbum(it)
                findNavController().navigate(direction)
            })
        }
    }

    companion object {
        val diffCallback = object : DiffUtil.ItemCallback<DirectoryQueryResult>() {
            override fun areItemsTheSame(oldItem: DirectoryQueryResult, newItem: DirectoryQueryResult): Boolean {
                return oldItem.directory == newItem.directory
            }

            override fun areContentsTheSame(oldItem: DirectoryQueryResult, newItem: DirectoryQueryResult): Boolean {
                return oldItem == newItem
            }
        }
    }


    private inner class AlbumListAdapter : DataBindingPagedListAdapter<DirectoryQueryResult, AlbumGridCellBinding>(diffCallback) {

        override fun onBindViewHolder(holder: DataBindingViewHolder<AlbumGridCellBinding>, position: Int) {
            super.onBindViewHolder(holder, position)
            holder.binding.setVariable(BR.eventListener, viewModel)
            val item = getItem(position) ?: return
            GlideApp.with(holder.itemView)
                    .load(viewModel.thumbnailUrl(item.fileEntry))
                    .into(holder.binding.imageView)
        }

        override fun getLayoutId(position: Int): Int {
            return R.layout.album_grid_cell
        }
    }
}

