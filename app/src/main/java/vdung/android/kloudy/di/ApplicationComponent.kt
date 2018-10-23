package vdung.android.kloudy.di

import android.content.Context
import dagger.Component
import dagger.Module
import dagger.Provides
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import vdung.android.kloudy.KloudyApplication
import javax.inject.Singleton

@Singleton
@Component(modules = [
    ApplicationModule::class,
    AndroidSupportInjectionModule::class,
    ActivityBindingModule::class,
    ViewModelModule::class,
    GlideDaggerModule::class,
    DataModule::class
])
interface ApplicationComponent : AndroidInjector<KloudyApplication> {
    @Component.Builder
    abstract class Builder : AndroidInjector.Builder<KloudyApplication>()
}

@Module
class ApplicationModule {

    @Provides
    fun provideContext(application: KloudyApplication): Context {
        return application.applicationContext
    }
}