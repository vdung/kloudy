package vdung.android.kloudy.ui.pages

import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.evernote.android.state.State
import com.evernote.android.state.StateSaver
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import dagger.android.support.DaggerFragment
import vdung.android.kloudy.databinding.VideoPageFragmentBinding
import vdung.android.kloudy.di.GlideApp
import vdung.android.kloudy.ui.timeline.TimelineViewModel
import vdung.android.kloudy.ui.widget.FragmentStartTransitionListener
import javax.inject.Inject

class VideoPageFragment : DaggerFragment() {

    companion object {
        private const val ARG_POSITION = "ARG_POSITION"

        fun newInstance(position: Int) = VideoPageFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_POSITION, position)
            }
        }
    }

    private lateinit var viewModel: TimelineViewModel
    internal lateinit var binding: VideoPageFragmentBinding

    @Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    internal lateinit var exoDataSourceFactory: DataSource.Factory
    private var isPlayerInitialized = false
    private var isVisibleToUser = false

    @State
    var currentPosition: Long = 0
    @State
    var playWhenReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StateSaver.restoreInstanceState(this, savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = VideoPageFragmentBinding.inflate(inflater, container, false).also {
            it.setLifecycleOwner(this)
        }

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = ViewModelProviders.of(requireActivity(), viewModelFactory).get(TimelineViewModel::class.java)

        if (isVisibleToUser && !isPlayerInitialized) {
            initializePlayer()
        }

        val position = arguments!!.getInt(ARG_POSITION)
        val photo = viewModel.fileEntries[position]
        val url = Uri.parse(photo.url)

        ViewCompat.setTransitionName(binding.videoPlayer, url.toString())
        GlideApp.with(binding.videoPlayer)
                .load(viewModel.thumbnailUrl(photo))
                .onlyRetrieveFromCache(true)
                .listener(FragmentStartTransitionListener(parentFragment!!))
                .into(object : CustomViewTarget<PlayerView, Drawable>(binding.videoPlayer) {
                    override fun onLoadFailed(errorDrawable: Drawable?) {}

                    override fun onResourceCleared(placeholder: Drawable?) {}

                    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                        binding.videoPlayer.useArtwork = true
                        binding.videoPlayer.defaultArtwork = resource
                    }
                })
    }

    override fun onPause() {
        super.onPause()
        if (isPlayerInitialized) {
            binding.videoPlayer.player.playWhenReady = false
            binding.videoPlayer.onPause()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isPlayerInitialized) {
            binding.videoPlayer.onResume()
            binding.videoPlayer.player.playWhenReady = playWhenReady
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (isPlayerInitialized) {
            currentPosition = binding.videoPlayer.player.currentPosition
            playWhenReady = binding.videoPlayer.player.playWhenReady
        }

        StateSaver.saveInstanceState(this, outState)
    }

    override fun onDestroyView() {
        if (isPlayerInitialized) {
            releasePlayer()
        }
        super.onDestroyView()
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        this.isVisibleToUser = isVisibleToUser
        if (::viewModel.isInitialized) {
            when {
                isVisibleToUser && !isPlayerInitialized -> initializePlayer()
                !isVisibleToUser && isPlayerInitialized -> releasePlayer()
            }
        }
    }

    private fun initializePlayer() {
        val position = arguments!!.getInt(ARG_POSITION)
        val photo = viewModel.fileEntries[position]
        val url = Uri.parse(photo.url)

        val videoSource = ExtractorMediaSource.Factory(exoDataSourceFactory).createMediaSource(url)

        val exoPlayer = ExoPlayerFactory.newSimpleInstance(requireContext())
        exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
        exoPlayer.prepare(videoSource)
        exoPlayer.seekTo(currentPosition)
        exoPlayer.playWhenReady = playWhenReady

        binding.videoPlayer.player = exoPlayer
        binding.videoPlayer.onPause()

        isPlayerInitialized = true
    }

    private fun releasePlayer() {
        binding.videoPlayer.run {
            player.release()
            player = null
        }

        isPlayerInitialized = false
    }
}