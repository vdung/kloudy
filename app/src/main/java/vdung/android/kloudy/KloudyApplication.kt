package vdung.android.kloudy

import dagger.android.AndroidInjector
import dagger.android.support.DaggerApplication
import vdung.android.kloudy.di.DaggerApplicationComponent
import vdung.android.kloudy.di.GlideComponent
import javax.inject.Inject

class KloudyApplication : DaggerApplication() {

    @Inject
    internal lateinit var glideComponentBuilder: GlideComponent.Builder

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return DaggerApplicationComponent.builder().create(this)
    }
}