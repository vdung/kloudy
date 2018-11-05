package vdung.android.kloudy.ui.timeline

import android.util.SparseArray
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.ViewModel
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import vdung.android.kloudy.data.Result
import vdung.android.kloudy.data.fetch
import vdung.android.kloudy.data.model.FileEntry
import vdung.android.kloudy.data.nextcloud.NextcloudConfig
import vdung.android.kloudy.data.nextcloud.NextcloudRepository
import javax.inject.Inject

class TimelineViewModel @Inject constructor(
        private val nextcloudRepository: NextcloudRepository,
        private val nextcloudConfig: NextcloudConfig
) : ViewModel(), TimelineEventListener {

    private val fileEntriesResource = nextcloudRepository.fetchAllEntries()
    private val entryClickProcessor = PublishProcessor.create<Int>()

    val isRefreshing = LiveDataReactiveStreams.fromPublisher(Flowable.fromPublisher(fileEntriesResource).map { it is Result.Pending })
    val loadFilesResult = LiveDataReactiveStreams.fromPublisher(
            Flowable.fromPublisher(fileEntriesResource)
                    .observeOn(Schedulers.io())
                    .filter { it is Result.Success }
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
                    .observeOn(AndroidSchedulers.mainThread())
    )

    val entryClickEvent get() = LiveDataReactiveStreams.fromPublisher(entryClickProcessor)

    var currentPage = 0

    fun thumbnailUrl(fileEntry: FileEntry): String {
        return nextcloudConfig.preferredPreviewUri(fileEntry).toString()
    }

    fun refresh() {
        fileEntriesResource.fetch()
    }

    override fun onFileEntryClick(itemId: Int) {
        entryClickProcessor.onNext(itemId)
    }
}

interface TimelineEventListener {
    fun onFileEntryClick(itemId: Int)
}