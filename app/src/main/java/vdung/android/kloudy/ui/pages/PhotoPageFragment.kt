package vdung.android.kloudy.ui.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import dagger.android.support.DaggerFragment
import vdung.android.kloudy.databinding.PhotoPageFragmentBinding
import vdung.android.kloudy.di.GlideApp
import vdung.android.kloudy.ui.timeline.TimelineViewModel
import vdung.android.kloudy.ui.widget.FragmentStartTransitionListener
import javax.inject.Inject

class PhotoPageFragment : DaggerFragment() {

    companion object {
        private const val ARG_POSITION = "ARG_POSITION"

        fun newInstance(position: Int) = PhotoPageFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_POSITION, position)
            }
        }
    }

    private lateinit var viewModel: TimelineViewModel
    internal lateinit var binding: PhotoPageFragmentBinding

    @Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = PhotoPageFragmentBinding.inflate(inflater, container, false).also {
            it.setLifecycleOwner(this)
        }

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(requireActivity(), viewModelFactory).get(TimelineViewModel::class.java)

        val position = arguments!!.getInt(ARG_POSITION)

        val photo = viewModel.fileEntries[position]
        binding.apply {
            ViewCompat.setTransitionName(photoView, photo.url)
            GlideApp.with(binding.root).run {
                load(photo.url)
                        .thumbnail(
                                load(viewModel.thumbnailUrl(photo))
                                        .onlyRetrieveFromCache(true)
                                        .apply {
                                            parentFragment?.let {
                                                listener(FragmentStartTransitionListener(it))
                                            }
                                        }
                        )
                        .into(photoView)
            }
        }
    }
}