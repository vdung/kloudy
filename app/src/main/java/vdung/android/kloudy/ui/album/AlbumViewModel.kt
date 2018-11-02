package vdung.android.kloudy.ui.album

import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.ViewModel
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
        return@lazy LiveDataReactiveStreams.fromPublisher(nextcloudRepository.albums())
    }

    val albumClickEvent get() = LiveDataReactiveStreams.fromPublisher(albumClickProcessor)
    val entryClickEvent get() = LiveDataReactiveStreams.fromPublisher(entryClickProcessor)

    fun albumFiles(directory: String) = LiveDataReactiveStreams.fromPublisher(nextcloudRepository.albumFiles(directory))

    fun thumbnailUrl(fileEntry: FileEntry): String {
        return nextcloudConfig.preferedPreviewUri(fileEntry).toString()
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