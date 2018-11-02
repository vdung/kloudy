package vdung.android.kloudy.ui.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import dagger.android.support.DaggerFragment
import vdung.android.kloudy.data.model.FileEntry
import vdung.android.kloudy.databinding.PhotoPageFragmentBinding
import vdung.android.kloudy.di.GlideApp

class PhotoPageFragment : DaggerFragment() {

    companion object {
        private const val ARG_FILE_ENTRY = "ARG_FILE_ENTRY"
        private const val ARG_THUMBNAIL_URL = "ARG_THUMBNAIL_URL"

        fun newInstance(fileEntry: FileEntry, thumbnailUrl: String?) = PhotoPageFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_FILE_ENTRY, fileEntry)
                putString(ARG_THUMBNAIL_URL, thumbnailUrl)
            }
        }
    }

    internal lateinit var binding: PhotoPageFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = PhotoPageFragmentBinding.inflate(inflater, container, false).also {
            it.setLifecycleOwner(this)
        }

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val fileEntry: FileEntry = arguments!!.getParcelable(ARG_FILE_ENTRY)!!
        val thumbnailUrl = arguments!!.getString(ARG_THUMBNAIL_URL)

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
