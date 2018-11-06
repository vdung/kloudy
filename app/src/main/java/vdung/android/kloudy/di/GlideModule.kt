package vdung.android.kloudy.di

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import dagger.Module
import dagger.Subcomponent
import okhttp3.Call
import vdung.android.kloudy.KloudyApplication
import vdung.android.kloudy.data.glide.Thumbnail
import vdung.android.kloudy.data.glide.ThumbnailModelLoader
import java.io.InputStream
import javax.inject.Inject


@GlideModule
class CloudGalleryGlideModule : AppGlideModule() {
    @Inject
    lateinit var callFactory: Call.Factory
    @Inject
    lateinit var modelLoaderFactory: ThumbnailModelLoader.Factory

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        val application = context.applicationContext
        if (application is KloudyApplication) {
            application.glideComponentBuilder.build().inject(this)
        }
        registry.replace(GlideUrl::class.java, InputStream::class.java, OkHttpUrlLoader.Factory(callFactory))
        registry.append(Thumbnail::class.java, InputStream::class.java, modelLoaderFactory)
    }
}

@Subcomponent
interface GlideComponent {
    fun inject(module: CloudGalleryGlideModule)
    @Subcomponent.Builder
    interface Builder {
        fun build(): GlideComponent
    }
}

@Module(subcomponents = [GlideComponent::class])
abstract class GlideDaggerModule
