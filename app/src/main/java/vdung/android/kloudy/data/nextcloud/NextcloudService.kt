package vdung.android.kloudy.data.nextcloud

import io.reactivex.Single
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Header
import vdung.kodav.MultiStatus

interface NextcloudService {

    @HTTP(method = "SEARCH", path = "remote.php/dav/", hasBody = true)
    fun search(@Body body: RequestBody): Single<MultiStatus>

    @GET("index.php/apps/files/")
    fun ping(@Header("Authorization") credentials: String): Single<ResponseBody>
}