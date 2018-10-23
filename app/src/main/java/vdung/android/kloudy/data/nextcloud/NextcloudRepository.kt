package vdung.android.kloudy.data.nextcloud

import android.net.Uri
import androidx.paging.PagedList
import androidx.paging.RxPagedListBuilder
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import org.reactivestreams.Publisher
import org.xmlpull.v1.XmlPullParser
import vdung.android.kloudy.data.NetworkResourcePublisher
import vdung.android.kloudy.data.fetch
import vdung.android.kloudy.data.model.FileEntry
import vdung.android.kloudy.data.model.FileEntryDao
import vdung.android.kloudy.data.model.FileEntryWithDate
import vdung.android.kloudy.data.model.User
import vdung.kodav.*
import vdung.kodav.okhttp.searchRequest

data class FileId(override val value: Int?) : Prop<Int> {
    companion object : Prop.Parser<FileId> {
        override val tag = Xml.Tag("http://owncloud.org/ns", "fileid")

        override fun parse(parser: XmlPullParser) = FileId(Xml.parseText(parser)?.toInt())
    }
}

class NextcloudRepository(
        private val service: NextcloudService,
        private val fileEntryDao: FileEntryDao,
        private val config: NextcloudConfig
): FileEntryDao by fileEntryDao {

    init {
        Prop.register(FileId)
    }

    fun pagedEntries(path: String = "", depth: Int = Scope.DEPTH_INFINITY) = object : NetworkResourcePublisher<PagedList<FileEntry>, Unit, MultiStatus>() {
        override fun localData(): Publisher<PagedList<FileEntry>> {
            return RxPagedListBuilder(fileEntryDao.filesSortedDescendingByLastModified(), PagedList.Config.Builder().setPageSize(50).setPrefetchDistance(150).build())
                    .setBoundaryCallback(object : PagedList.BoundaryCallback<FileEntry>() {
                        override fun onZeroItemsLoaded() {
                            fetch()
                        }
                    })
                    .buildFlowable(BackpressureStrategy.LATEST)
        }

        override fun shouldFetch(arg: Unit, previousResult: PagedList<FileEntry>) = true

        override fun fetchFromNetwork(arg: Unit): Publisher<MultiStatus> = service.search(createSearchRequest(path, depth)).toFlowable().subscribeOn(Schedulers.io())

        override fun saveNetworkResult(networkData: MultiStatus) = updateDatabase(multiStatusToPhotoList(networkData))
    }

    private fun updateDatabase(fileEntries: List<FileEntry>) {
        fileEntryDao.upsertAndDeleteInvalidEntries(fileEntries)
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
                        eq {
                            prop {
                                -GetContentType.tag
                            }
                            literal("video/mp4")
                        }
                        eq {
                            prop {
                                -GetContentType.tag
                            }
                            literal("video/m4v")
                        }
                        eq {
                            prop {
                                -GetContentType.tag
                            }
                            literal("video/mkv")
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

    private fun multiStatusToPhotoList(multiStatus: MultiStatus, thumbnailSize: Int = 256) =
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
