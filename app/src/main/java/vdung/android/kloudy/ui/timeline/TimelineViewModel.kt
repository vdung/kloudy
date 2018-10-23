package vdung.android.kloudy.ui.timeline

import android.util.SparseArray
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.ViewModel
import io.reactivex.Flowable
import io.reactivex.processors.PublishProcessor
import vdung.android.kloudy.data.Result
import vdung.android.kloudy.data.fetch
import vdung.android.kloudy.data.model.FileEntry
import vdung.android.kloudy.data.nextcloud.NextcloudConfig
import vdung.android.kloudy.data.nextcloud.NextcloudRepository
import javax.inject.Inject

class TimelineViewModel @Inject constructor(
        private val nextcloudRepository: NextcloudRepository,
        private val nextcloudConfig: NextcloudConfig
) : ViewModel(), CellEventListener {

    private val fileEntriesResource = nextcloudRepository.pagedEntries()
    private val imageViewClickProcessor = PublishProcessor.create<Int>()

    val isRefreshing = LiveDataReactiveStreams.fromPublisher(Flowable.fromPublisher(fileEntriesResource).map { it is Result.Pending })
    val loadFilesResult = LiveDataReactiveStreams.fromPublisher(
            Flowable.fromPublisher(fileEntriesResource)
                    .map {
                        val groups = nextcloudRepository.groupFilesByMonth()
                        val headers = SparseArray<String>()
                        if (groups.isNotEmpty()) {
                            headers.put(0, groups[0].month)
                            for (i in 1 until groups.size) {
                                headers.put(headers.keyAt(i - 1) + groups[i - 1].count + 1, groups[i].month)
                            }
                        }

                        return@map Pair(it, headers)
                    }
    )

    val entryClickEvent
        get() = LiveDataReactiveStreams.fromPublisher(imageViewClickProcessor)

    var currentPage = 0
    val fileEntries: List<FileEntry> get() = loadFilesResult.value?.first?.value ?: emptyList()

    fun thumbnailUrl(fileEntry: FileEntry): String {
        return nextcloudConfig.thumbnailUri(fileEntry.url).toString()
    }

    fun refresh() {
        fileEntriesResource.fetch()
    }

    override fun onImageViewClick(itemId: Int) {
        imageViewClickProcessor.onNext(itemId)
    }
}

interface CellEventListener {
    fun onImageViewClick(itemId: Int)
}