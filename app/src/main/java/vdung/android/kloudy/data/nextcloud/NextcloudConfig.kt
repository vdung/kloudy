package vdung.android.kloudy.data.nextcloud

import android.net.Uri
import vdung.android.kloudy.data.model.User

class NextcloudConfig(val user: User) {

    val baseUri = Uri.parse(user.server)

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
}