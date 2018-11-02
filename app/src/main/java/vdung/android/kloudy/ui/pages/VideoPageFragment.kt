package vdung.android.kloudy.ui.pages

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import com.evernote.android.state.State
import com.evernote.android.state.StateSaver
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import dagger.android.support.DaggerFragment
import vdung.android.kloudy.data.model.FileEntry
import vdung.android.kloudy.databinding.VideoPageFragmentBinding
import javax.inject.Inject

class VideoPageFragment : DaggerFragment() {

    companion object {
        private const val ARG_FILE_ENTRY = "ARG_FILE_ENTRY"

        fun newInstance(fileEntry: FileEntry) = VideoPageFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_FILE_ENTRY, fileEntry)
            }
        }
    }

    internal lateinit var binding: VideoPageFragmentBinding

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

        if (isVisibleToUser && !isPlayerInitialized) {
            initializePlayer()
        }

        val fileEntry: FileEntry = arguments!!.getParcelable(ARG_FILE_ENTRY)!!
        val url = Uri.parse(fileEntry.url)

        ViewCompat.setTransitionName(binding.videoPlayer, url.toString())
        requireActivity().let { it as? OnPagedLoadedListener }?.onPageLoaded(fileEntry)
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
        if (::exoDataSourceFactory.isInitialized) {
            when {
                isVisibleToUser && !isPlayerInitialized -> initializePlayer()
                !isVisibleToUser && isPlayerInitialized -> releasePlayer()
            }
        }
    }

    private fun initializePlayer() {
        val fileEntry: FileEntry = arguments!!.getParcelable(ARG_FILE_ENTRY)!!
        val url = Uri.parse(fileEntry.url)

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