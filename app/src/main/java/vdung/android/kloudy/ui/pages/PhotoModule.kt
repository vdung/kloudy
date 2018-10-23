package vdung.android.kloudy.ui.pages

import dagger.Module
import dagger.android.ContributesAndroidInjector
import vdung.android.kloudy.di.FragmentScoped

@Module
abstract class PhotoModule {

    @FragmentScoped
    @ContributesAndroidInjector
    abstract fun photoFragment(): PagerFragment

    @FragmentScoped
    @ContributesAndroidInjector
    abstract fun photoPageFragment(): PhotoPageFragment

    @FragmentScoped
    @ContributesAndroidInjector
    abstract fun videoPageFragment(): VideoPageFragment
}