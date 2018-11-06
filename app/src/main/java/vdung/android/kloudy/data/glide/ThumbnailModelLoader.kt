package vdung.android.kloudy.data.glide

import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.load.model.stream.BaseGlideUrlLoader
import vdung.android.kloudy.data.nextcloud.NextcloudConfig
import java.io.InputStream
import javax.inject.Inject

class ThumbnailModelLoader(
        private val nextcloudConfig: NextcloudConfig,
        urlLoader: ModelLoader<GlideUrl, InputStream>
) : BaseGlideUrlLoader<Thumbnail>(urlLoader) {
    override fun getUrl(model: Thumbnail, width: Int, height: Int, options: Options): String {
        return nextcloudConfig.previewUri(model.fileEntry.fileId, width, height).toString()
    }

    override fun handles(model: Thumbnail): Boolean {
        return true
    }

    class Factory @Inject constructor(
            private val nextcloudConfig: NextcloudConfig
    ) : ModelLoaderFactory<Thumbnail, InputStream> {
        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<Thumbnail, InputStream> {
            val urlLoader = multiFactory.build(GlideUrl::class.java, InputStream::class.java)
            return ThumbnailModelLoader(nextcloudConfig, urlLoader)
        }

        override fun teardown() {}
    }
}