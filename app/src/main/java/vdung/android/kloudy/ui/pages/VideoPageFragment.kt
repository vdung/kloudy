package vdung.android.kloudy.ui.pages

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.SharedElementCallback
import androidx.core.view.ViewCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.get
import com.evernote.android.state.State
import com.evernote.android.state.StateSaver
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.video.VideoListener
import dagger.android.support.DaggerFragment
import vdung.android.kloudy.data.model.FileEntry
import vdung.android.kloudy.databinding.VideoPageFragmentBinding
import vdung.android.kloudy.di.GlideApp
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

    @Inject
    internal lateinit var exoDataSourceFactory: DataSource.Factory
    @Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: PagerViewModel

    private lateinit var binding: VideoPageFragmentBinding

    private var isPlayerInitialized = false
    private var isVisibleToUser = false

    @State
    var playWhenReady = false

    internal val sharedElementCallback = object : SharedElementCallback() {

        private var playerVisibility: Int = 0

        override fun onMapSharedElements(names: MutableList<String>, sharedElements: MutableMap<String, View>) {
            val key = names[0]
            sharedElements[key] = binding.videoPreview
        }

        override fun onSharedElementStart(sharedElementNames: MutableList<String>?, sharedElements: MutableList<View>?, sharedElementSnapshots: MutableList<View>?) {
            super.onSharedElementStart(sharedElementNames, sharedElements, sharedElementSnapshots)
            playerVisibility = binding.videoPlayer.visibility
            binding.videoPlayer.visibility = View.INVISIBLE
        }

        override fun onSharedElementEnd(sharedElementNames: MutableList<String>?, sharedElements: MutableList<View>?, sharedElementSnapshots: MutableList<View>?) {
            super.onSharedElementEnd(sharedElementNames, sharedElements, sharedElementSnapshots)
            binding.videoPlayer.visibility = playerVisibility
        }
    }

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

        viewModel = ViewModelProviders.of(requireActivity(), viewModelFactory).get()

        val fileEntry: FileEntry = arguments!!.getParcelable(ARG_FILE_ENTRY)!!
        val url = Uri.parse(fileEntry.url)

        binding.run {
            ViewCompat.setTransitionName(videoPreview, url.toString())

            playVideo.setOnClickListener {
                videoPreviewFrame.visibility = View.INVISIBLE
                videoPlayer.visibility = View.VISIBLE
                videoPlayer.player.playWhenReady = true
            }
        }

        if (isVisibleToUser && !isPlayerInitialized) {
            initializePlayer()
        }
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
        exoPlayer.seekTo(0)
        exoPlayer.playWhenReady = playWhenReady
        exoPlayer.addVideoListener(object : VideoListener {
            override fun onSurfaceSizeChanged(width: Int, height: Int) {
                exoPlayer.removeVideoListener(this)
                val previewUri = viewModel.thumbnailUrl(fileEntry, width, height)
                GlideApp.with(binding.videoPreview)
                        .load(previewUri)
                        .apply {
                            requireActivity()
                                    .let { it as? OnPagedLoadedListener }
                                    ?.let {
                                        addListener(it.toRequestListener(fileEntry))
                                    }
                        }
                        .into(binding.videoPreview)
            }
        })

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