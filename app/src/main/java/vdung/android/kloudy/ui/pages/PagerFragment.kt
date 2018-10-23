package vdung.android.kloudy.ui.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.SharedElementCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.ViewPager
import dagger.android.support.DaggerFragment
import vdung.android.kloudy.databinding.PagerFragmentBinding
import vdung.android.kloudy.ui.timeline.TimelineViewModel
import javax.inject.Inject

class PagerFragment : DaggerFragment() {
    companion object {
        fun newInstance() = PagerFragment()
    }

    internal lateinit var viewModel: TimelineViewModel
    private lateinit var binding: PagerFragmentBinding

    @Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = PagerFragmentBinding.inflate(inflater, container, false).also {
            it.setLifecycleOwner(this)
        }

        postponeEnterTransition()

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(requireActivity(), viewModelFactory).get(TimelineViewModel::class.java)

        val photoAdapter = PhotoPagerAdapter(this)
        setEnterSharedElementCallback(object : SharedElementCallback() {
            override fun onMapSharedElements(names: MutableList<String>, sharedElements: MutableMap<String, View>) {
                val position = viewModel.currentPage
                photoAdapter.instantiateItem(binding.pager, position)
                        .let { it as? PhotoPageFragment }
                        ?.let {
                            sharedElements[names[0]] = it.binding.photoView
                        }
            }
        })

        binding.pager.apply {
            adapter = photoAdapter
            currentItem = viewModel.currentPage

            addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
                override fun onPageSelected(position: Int) {
                    viewModel.currentPage = position
                }
            })
        }
    }
}

private class PhotoPagerAdapter(
        val fragment: PagerFragment
) : FragmentStatePagerAdapter(fragment.childFragmentManager) {
    override fun getItem(position: Int): Fragment {
        val item = fragment.viewModel.fileEntries[position]
        return when {
            item.contentType.startsWith("image") -> PhotoPageFragment.newInstance(position)
            item.contentType.startsWith("video") -> VideoPageFragment.newInstance(position)
            else -> throw IllegalStateException()
        }
    }

    override fun getCount(): Int {
        return fragment.viewModel.fileEntries.size
    }
}
