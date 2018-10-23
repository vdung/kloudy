package vdung.android.kloudy.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import vdung.android.kloudy.ui.login.LoginModule
import vdung.android.kloudy.ui.main.MainActivity
import vdung.android.kloudy.ui.main.MainModule
import vdung.android.kloudy.ui.pages.PhotoModule
import vdung.android.kloudy.ui.timeline.TimelineModule


@Module
abstract class ActivityBindingModule {

    @ActivityScoped
    @ContributesAndroidInjector(modules = [
        TimelineModule::class,
        PhotoModule::class,
        LoginModule::class,
        MainModule::class
    ])
    internal abstract fun mainActivity(): MainActivity

}