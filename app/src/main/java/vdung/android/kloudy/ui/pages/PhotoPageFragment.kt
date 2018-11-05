package vdung.android.kloudy.ui.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.SharedElementCallback
import androidx.core.view.ViewCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.get
import dagger.android.support.DaggerFragment
import vdung.android.kloudy.data.model.FileEntry
import vdung.android.kloudy.databinding.PhotoPageFragmentBinding
import vdung.android.kloudy.di.GlideApp
import javax.inject.Inject

class PhotoPageFragment : DaggerFragment() {

    companion object {
        private const val ARG_FILE_ENTRY = "ARG_FILE_ENTRY"

        fun newInstance(fileEntry: FileEntry) = PhotoPageFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_FILE_ENTRY, fileEntry)
            }
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: PagerViewModel

    private lateinit var binding: PhotoPageFragmentBinding

    internal val sharedElementCallback = object : SharedElementCallback() {
        override fun onMapSharedElements(names: MutableList<String>, sharedElements: MutableMap<String, View>) {
            val key = names[0]
            sharedElements[key] = binding.photoView
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = PhotoPageFragmentBinding.inflate(inflater, container, false).also {
            it.setLifecycleOwner(this)
        }

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = ViewModelProviders.of(requireActivity(), viewModelFactory).get()

        val fileEntry: FileEntry = arguments!!.getParcelable(ARG_FILE_ENTRY)!!
        val thumbnailUrl = viewModel.thumbnailUrl(fileEntry)

        binding.apply {
            ViewCompat.setTransitionName(photoView, fileEntry.url)
            GlideApp.with(photoView)
                    .load(fileEntry.url)
                    .thumbnail(GlideApp.with(photoView).load(thumbnailUrl).onlyRetrieveFromCache(true))
                    .apply {
                        requireActivity()
                                .let { it as? OnPagedLoadedListener }
                                ?.let {
                                    addListener(it.toRequestListener(fileEntry))
                                }
                    }
                    .into(photoView)
        }
    }
}
