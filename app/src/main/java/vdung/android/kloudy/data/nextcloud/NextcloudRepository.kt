package vdung.android.kloudy.data.nextcloud

import android.net.Uri
import androidx.paging.PagedList
import androidx.paging.RxPagedListBuilder
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import okio.Buffer
import okio.ForwardingSource
import okio.Okio
import org.reactivestreams.Publisher
import vdung.android.kloudy.data.NetworkResourcePublisher
import vdung.android.kloudy.data.Result
import vdung.android.kloudy.data.fetch
import vdung.android.kloudy.data.model.DirectoryQueryResult
import vdung.android.kloudy.data.model.FileEntry
import vdung.android.kloudy.data.model.FileEntryDao
import vdung.android.kloudy.data.model.FileMetadataDao
import vdung.kodav.*
import vdung.kodav.okhttp.searchRequest
import java.io.File

class NextcloudRepository(
        private val service: NextcloudService,
        private val fileEntryDao: FileEntryDao,
        private val fileMetadataDao: FileMetadataDao,
        private val config: NextcloudConfig,
        private val cacheDir: File
) : FileEntryDao by fileEntryDao, FileMetadataDao by fileMetadataDao {

    data class Download(
            val bytesRead: Long,
            val contentLength: Long,
            val file: File?
    )

    init {
        Prop.register(FileId)
    }

    fun fetchAllEntries(path: String = "", depth: Int = Scope.DEPTH_INFINITY) = object : NetworkResourcePublisher<PagedList<FileEntry>, Unit, MultiStatus>() {
        override fun localData(): Publisher<PagedList<FileEntry>> {
            return RxPagedListBuilder(fileEntryDao.getFilesSortedByLastModifiedDescending(), PagedList.Config.Builder().setPageSize(50).setPrefetchDistance(150).build())
                    .setBoundaryCallback(object : PagedList.BoundaryCallback<FileEntry>() {
                        override fun onZeroItemsLoaded() {
                            fetch()
                        }
                    })
                    .buildFlowable(BackpressureStrategy.LATEST)
        }

        override fun shouldFetch(arg: Unit, previousResult: PagedList<FileEntry>) = true

        override fun fetchFromNetwork(arg: Unit): Publisher<MultiStatus> = service.search(createSearchRequest(path, depth)).toFlowable().subscribeOn(Schedulers.io())

        override fun saveNetworkResult(networkData: MultiStatus) = updateDatabase(multiStatusToFileEntries(networkData))
    }

    fun albums(): Publisher<PagedList<DirectoryQueryResult>> {
        return RxPagedListBuilder(fileMetadataDao.getAllDirectories(), 20).buildFlowable(BackpressureStrategy.LATEST)
    }

    fun albumFiles(directory: String, initialLoadPosition: Int = 0): Publisher<PagedList<FileEntry>> {
        return RxPagedListBuilder(fileMetadataDao.getFilesInDirectory(directory), 20)
                .setInitialLoadKey(initialLoadPosition)
                .buildFlowable(BackpressureStrategy.LATEST)
    }

    fun allFiles(initialLoadPosition: Int = 0): Publisher<PagedList<FileEntry>> {
        return RxPagedListBuilder(fileEntryDao.getFilesSortedByLastModifiedDescending(), 20)
                .setInitialLoadKey(initialLoadPosition)
                .buildFlowable(BackpressureStrategy.LATEST)
    }

    fun downloadFile(fileEntry: FileEntry): Publisher<Result<Download>> {
        val fileUri = Uri.parse(fileEntry.url)
        val name = fileEntry.name ?: fileUri.lastPathSegment
        val directory = File(cacheDir, fileUri.path!!.removeSuffix(name))
        directory.mkdirs()

        val file = File(directory, name)

        return service.downloadFile(fileEntry.url)
                .toFlowable()
                .flatMap { body ->
                    Flowable.using({ Okio.buffer(Okio.sink(file)) }, { sink ->
                        Flowable.create<Result<Download>>({ emitter ->
                            val contentLength = body.contentLength()
                            val source = object : ForwardingSource(Okio.buffer(body.source())) {
                                var totalBytesRead = 0L

                                override fun read(sink: Buffer, byteCount: Long): Long {
                                    val bytesRead = super.read(sink, byteCount)
                                    totalBytesRead += if (bytesRead != -1L) bytesRead else 0
                                    emitter.onNext(Result.Pending(Download(totalBytesRead, contentLength, null)))

                                    return bytesRead
                                }
                            }
                            sink.writeAll(source)
                            emitter.onNext(Result.Success(Download(source.totalBytesRead, contentLength, file)))
                            emitter.onComplete()
                        }, BackpressureStrategy.LATEST)
                    }, { it.close() })
                }
                .onErrorReturn { Result.Error(it, Download(0, -1, null)) }
    }

    private fun updateDatabase(fileEntries: List<FileEntry>) {
        fileEntryDao.upsertAndDeleteInvalidEntries(fileMetadataDao, fileEntries)
    }

    private fun createSearchRequest(path: String, depth: Int) = searchRequest {
        basicSearch {
            select {
                prop {
                    -DisplayName.tag
                    -GetLastModified.tag
                    -GetContentLength.tag
                    -GetContentType.tag
                    -FileId.tag
                }
            }
            from {
                scope {
                    href("/files/${config.user.username}$path")
                    depth(depth)
                }
            }
            where {
                and {
                    or {
                        like {
                            prop {
                                -GetContentType.tag
                            }
                            literal("image/%")
                        }
                        like {
                            prop {
                                -GetContentType.tag
                            }
                            literal("video/%")
                        }
                    }
                    for (mimeType in config.ignoredMimeTypes) {
                        not {
                            eq {
                                prop {
                                    -GetContentType.tag
                                }
                                literal(mimeType)
                            }
                        }
                    }
                    gt {
                        prop {
                            -Xml.Tag("http://owncloud.org/ns", "size")
                        }
                        literal(1024 * 100)
                    }
                }
            }
        }
    }

    private fun multiStatusToFileEntries(multiStatus: MultiStatus) =
            multiStatus.responses
                    .asSequence()
                    .mapNotNull {
                        it.propStat("HTTP/1.1 200 OK")
                                ?.let { propStat ->
                                    val url = config.fullUri(it.href!!)
                                    propStat.propValue<String, GetContentType>(GetContentType.tag)?.let { contentType ->
                                        FileEntry(propStat.propValue(FileId.tag)!!, url.toString(), contentType, propStat.propValue(DisplayName.tag), propStat.propValue(GetLastModified.tag)!!)
                                    }
                                }
                    }
                    .toList()
}

private fun Response.propStat(status: String) = propStats.firstOrNull { it.status == status }
