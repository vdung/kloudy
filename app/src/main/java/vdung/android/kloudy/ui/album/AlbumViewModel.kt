package vdung.android.kloudy.ui.album

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import androidx.paging.PagedList
import io.reactivex.processors.PublishProcessor
import vdung.android.kloudy.data.model.DirectoryQueryResult
import vdung.android.kloudy.data.model.FileEntry
import vdung.android.kloudy.data.nextcloud.NextcloudConfig
import vdung.android.kloudy.data.nextcloud.NextcloudRepository
import javax.inject.Inject

class AlbumViewModel @Inject constructor(
        private val nextcloudRepository: NextcloudRepository,
        private val nextcloudConfig: NextcloudConfig
) : ViewModel(), AlbumEventListener {

    private val albumClickProcessor = PublishProcessor.create<String>()
    private val entryClickProcessor = PublishProcessor.create<Int>()

    val albums: LiveData<PagedList<DirectoryQueryResult>> by lazy {
        return@lazy nextcloudRepository.albums().toLiveData()
    }

    val albumClickEvent get() = albumClickProcessor.toLiveData()
    val entryClickEvent get() = entryClickProcessor.toLiveData()

    fun albumFiles(directory: String) = nextcloudRepository.albumFiles(directory).toLiveData()

    fun thumbnailUrl(fileEntry: FileEntry): String {
        return nextcloudConfig.preferredPreviewUri(fileEntry).toString()
    }

    override fun onAlbumClick(directory: String) {
        albumClickProcessor.onNext(directory)
    }

    override fun onEntryClick(fileId: Int) {
        entryClickProcessor.onNext(fileId)
    }
}

interface AlbumEventListener {
    fun onAlbumClick(directory: String)
    fun onEntryClick(fileId: Int)
}