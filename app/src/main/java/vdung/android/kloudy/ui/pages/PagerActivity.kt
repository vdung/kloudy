package vdung.android.kloudy.ui.pages

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import androidx.appcompat.widget.ActionMenuView
import androidx.core.app.SharedElementCallback
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.*
import androidx.paging.PagedList
import androidx.viewpager.widget.ViewPager
import com.evernote.android.state.State
import com.evernote.android.state.StateSaver
import com.google.android.material.snackbar.Snackbar
import dagger.android.support.DaggerAppCompatActivity
import vdung.android.kloudy.R
import vdung.android.kloudy.data.Result
import vdung.android.kloudy.data.model.FileEntry
import vdung.android.kloudy.data.nextcloud.NextcloudRepository
import vdung.android.kloudy.databinding.PagerActivityBinding
import javax.inject.Inject


class PagerActivity : DaggerAppCompatActivity(), ActionMenuView.OnMenuItemClickListener, OnPagedLoadedListener {
    @Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory

    private val handler = Handler()
    private val hideUIRunnable = Runnable {
        hideSystemUI()
    }

    private lateinit var viewModel: PagerViewModel
    private lateinit var binding: PagerActivityBinding

    @State
    var currentPage: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportPostponeEnterTransition()

        StateSaver.restoreInstanceState(this, savedInstanceState)

        viewModel = ViewModelProviders.of(this, viewModelFactory).get()
        if (savedInstanceState == null) {
            currentPage = viewModel.initialPosition
        }

        binding = DataBindingUtil.setContentView(this, R.layout.pager_activity)
        binding.apply {
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            menuInflater.inflate(R.menu.file_actions, bottomToolbar.menu)
            bottomToolbar.setOnMenuItemClickListener(this@PagerActivity)
        }
        viewModel.fileEntriesResult.observe({ lifecycle }) { it ->
            binding.apply {
                pager.adapter = PagerAdapter().also { adapter ->
                    setEnterSharedElementCallback(object : SharedElementCallback() {
                        override fun onMapSharedElements(names: MutableList<String>, sharedElements: MutableMap<String, View>) {
                            val key = names[0]
                            adapter.instantiateItem(pager, pager.currentItem)
                                    .let {
                                        when (it) {
                                            is PhotoPageFragment -> sharedElements[key] = it.binding.photoView
                                            is VideoPageFragment -> sharedElements[key] = it.binding.videoPlayer.videoSurfaceView
                                        }
                                    }
                        }
                    })
                    it.addWeakCallback(null, adapter.callback)
                }

                pager.currentItem = currentPage
                it.loadAround(viewModel.initialPosition)
                title = it[viewModel.initialPosition]?.url?.let { Uri.parse(it).lastPathSegment }

                pager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
                    override fun onPageSelected(position: Int) {
                        currentPage = position
                        setResult(Activity.RESULT_OK, Intent().putExtra("CURRENT_PAGE", position))

                        it.loadAround(position)
                        title = it[position]?.url?.let { Uri.parse(it).lastPathSegment }
                        invalidateOptionsMenu()
                    }
                })
            }
        }

        window.decorView.apply {
            setOnSystemUiVisibilityChangeListener { flags ->
                toggleImmersive(flags)
            }
        }
        setupSystemUI()
        toggleImmersive(window.decorView.systemUiVisibility)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        StateSaver.saveInstanceState(this, outState)
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        val fileEntry = viewModel.fileEntries[binding.pager.currentItem] ?: return false
        return when (item.itemId) {
            R.id.action_share -> {
                downloadFile(fileEntry) {
                    val intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, it)
                        type = fileEntry.contentType
                    }
                    startActivity(Intent.createChooser(intent, getString(R.string.action_share)))
                }
                true
            }
            R.id.action_open_in -> {
                downloadFile(fileEntry) {
                    val intent = Intent(Intent.ACTION_VIEW, it).apply {
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addCategory(Intent.CATEGORY_DEFAULT)
                    }
                    startActivity(Intent.createChooser(intent, getString(R.string.action_open_in)))
                }
                true
            }
            R.id.action_use_as -> {
                downloadFile(fileEntry) {
                    val intent = Intent(Intent.ACTION_ATTACH_DATA, it).apply {
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addCategory(Intent.CATEGORY_DEFAULT)
                    }
                    startActivity(Intent.createChooser(intent, getString(R.string.action_use_as)))
                }
                true
            }
            else -> false
        }
    }

    private fun downloadFile(fileEntry: FileEntry, callback: (Uri) -> Unit) {
        val snackBar = Snackbar.make(binding.content, R.string.message_downloading_file, Snackbar.LENGTH_INDEFINITE).apply {
            show()
        }

        viewModel.downloadFile(fileEntry).apply {
            observe(LifecycleOwner { lifecycle }, object : Observer<Result<NextcloudRepository.Download>> {
                override fun onChanged(result: Result<NextcloudRepository.Download>?) {
                    when (result) {
                        is Result.Success -> {
                            removeObserver(this)
                            val uri = FileProvider.getUriForFile(this@PagerActivity, getString(R.string.file_provider), result.value.file!!)
                            callback(uri)
                            snackBar.dismiss()
                        }
                        is Result.Error -> {
                            removeObserver(this)
                            snackBar.setText(result.error.message!!)
                            handler.postDelayed({
                                snackBar.dismiss()
                            }, 3000)
                        }
                    }
                }

            })
        }
    }

    override fun onPageLoaded(fileEntry: FileEntry) {
        if (fileEntry == viewModel.fileEntries[viewModel.initialPosition]) {
            setupPostponedTransition()
        }
    }

    private fun toggleImmersive(flags: Int) {
        val visible = flags and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION == 0
        if (visible) {
            supportActionBar?.show()
            binding.bottomToolbar.visibility = View.VISIBLE
            handler.postDelayed(hideUIRunnable, 3000)
        } else {
            supportActionBar?.hide()
            binding.bottomToolbar.visibility = View.GONE
        }
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_IMMERSIVE)
    }

    private fun setupSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    private fun setupPostponedTransition() {
        binding.pager.viewTreeObserver.apply {
            addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    removeOnPreDrawListener(this)
                    supportStartPostponedEnterTransition()
                    return true
                }
            })
        }
    }

    private inner class PagerAdapter : FragmentStatePagerAdapter(supportFragmentManager) {

        val callback: PagedList.Callback = PagerCallback()

        override fun getItem(position: Int): Fragment {
            val item = viewModel.fileEntries[position]
            return when {
                item == null -> PlaceholderPageFragment()
                item.contentType.startsWith("image") -> PhotoPageFragment.newInstance(item, viewModel.thumbnailUrl(item))
                item.contentType.startsWith("video") -> VideoPageFragment.newInstance(item)
                else -> throw IllegalStateException()
            }
        }

        override fun getItemPosition(`object`: Any): Int {
            return when (`object`) {
                is PlaceholderPageFragment -> POSITION_NONE
                else -> super.getItemPosition(`object`)
            }
        }

        override fun getCount(): Int {
            return viewModel.fileEntries.size
        }

        private inner class PagerCallback : PagedList.Callback() {
            override fun onChanged(position: Int, count: Int) = notifyDataSetChanged()

            override fun onInserted(position: Int, count: Int) = notifyDataSetChanged()

            override fun onRemoved(position: Int, count: Int) = notifyDataSetChanged()
        }
    }
}


