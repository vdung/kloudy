package vdung.android.kloudy.ui.pages

import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.ViewModel
import androidx.paging.PagedList
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import vdung.android.kloudy.data.Result
import vdung.android.kloudy.data.model.FileEntry
import vdung.android.kloudy.data.nextcloud.NextcloudConfig
import vdung.android.kloudy.data.nextcloud.NextcloudRepository
import javax.inject.Inject

class PagerViewModel @Inject constructor(
        private val nextcloudRepository: NextcloudRepository,
        private val nextcloudConfig: NextcloudConfig,
        arguments: Args
) : ViewModel() {

    data class Args(
            val currentPosition: Int = 0,
            val directory: String = ""
    )

    val initialPosition = arguments.currentPosition

    val fileEntriesResult: LiveData<PagedList<FileEntry>> by lazy {
        if (arguments.directory == "") {
            return@lazy LiveDataReactiveStreams.fromPublisher(nextcloudRepository.allFiles(initialPosition))
        } else {
            return@lazy LiveDataReactiveStreams.fromPublisher(nextcloudRepository.albumFiles(arguments.directory, initialPosition))
        }
    }

    val fileEntries: List<FileEntry?> get() = fileEntriesResult.value ?: emptyList()

    fun thumbnailUrl(fileEntry: FileEntry): String {
        return if (fileEntry.contentType.startsWith("image")) {
            nextcloudConfig.previewUri(fileEntry.fileId).toString()
        } else {
            nextcloudConfig.thumbnailUri(fileEntry.url).toString()
        }
    }

    fun downloadFile(fileEntry: FileEntry): LiveData<Result<NextcloudRepository.Download>> {
        return LiveDataReactiveStreams.fromPublisher(
                Flowable.fromPublisher(nextcloudRepository.downloadFile(fileEntry))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
        )
    }
}