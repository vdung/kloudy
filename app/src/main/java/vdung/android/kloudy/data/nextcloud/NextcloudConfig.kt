package vdung.android.kloudy.data.nextcloud

import android.net.Uri
import vdung.android.kloudy.data.model.FileEntry
import vdung.android.kloudy.data.user.User

class NextcloudConfig(
        val user: User
) {

    val baseUri = Uri.parse(user.server)
    val ignoredMimeTypes = listOf(
            "video/quicktime",
            "video/x-msvideo",
            "video/x-ms-wmv"
    )

    fun fullUri(encodedPath: String): Uri {
        return baseUri.buildUpon().encodedPath(encodedPath).build()
    }

    fun thumbnailUri(url: String, size: Int = 256): Uri {
        val pathSegments = Uri.parse(url.removePrefix(baseUri.toString())).pathSegments

        return Uri.withAppendedPath(baseUri, "index.php/apps/files/api/v1/thumbnail/$size/$size/")
                .buildUpon()
                .apply {
                    for (segment in pathSegments.subList(4, pathSegments.size)) {
                        appendPath(segment)
                    }
                }
                .build()
    }

    fun avatarUri(size: Int = 128): Uri {
        return Uri.withAppendedPath(baseUri, "index.php/avatar/${user.username}/$size")
    }

    fun galleryPreviewUri(fileId: Int, width: Int = 400, height: Int = 200): Uri {
        return Uri.withAppendedPath(baseUri, "index.php/apps/gallery/preview/$fileId")
                .buildUpon()
                .appendQueryParameter("width", width.toString())
                .appendQueryParameter("height", height.toString())
                .build()
    }

    fun previewUri(fileId: Int, width: Int = 512, height: Int = 512): Uri {
        return Uri.withAppendedPath(baseUri, "index.php/core/preview")
                .buildUpon()
                .appendQueryParameter("fileId", fileId.toString())
                .appendQueryParameter("x", width.toString())
                .appendQueryParameter("y", height.toString())
                .appendQueryParameter("a", "1")
                .build()
    }

    fun preferredPreviewUri(fileEntry: FileEntry, width: Int = 512, height: Int = 512): Uri {
        return previewUri(fileEntry.fileId, width, height)
    }
}