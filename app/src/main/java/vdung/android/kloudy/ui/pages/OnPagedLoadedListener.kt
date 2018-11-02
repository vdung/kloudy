package vdung.android.kloudy.ui.pages

import android.graphics.drawable.Drawable
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import vdung.android.kloudy.data.model.FileEntry

interface OnPagedLoadedListener {
    fun onPageLoaded(fileEntry: FileEntry)
}

fun OnPagedLoadedListener.toRequestListener(fileEntry: FileEntry): RequestListener<Drawable> {
    return object : RequestListener<Drawable> {
        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
            onPageLoaded(fileEntry)
            return false
        }

        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
            onPageLoaded(fileEntry)
            return false
        }
    }
}