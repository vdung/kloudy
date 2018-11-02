package vdung.android.kloudy.ui.pages

import androidx.lifecycle.ViewModel
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import vdung.android.kloudy.di.FragmentScoped
import vdung.android.kloudy.di.ViewModelKey

@Module(includes = [PhotoViewModelModule::class])
abstract class PhotoModule {

    @FragmentScoped
    @ContributesAndroidInjector
    abstract fun photoPageFragment(): PhotoPageFragment

    @FragmentScoped
    @ContributesAndroidInjector
    abstract fun videoPageFragment(): VideoPageFragment

    @Binds
    @IntoMap
    @ViewModelKey(PagerViewModel::class)
    abstract fun bindViewModel(viewModel: PagerViewModel): ViewModel
}

@Module
class PhotoViewModelModule {

    @Provides
    fun provideArgs(activity: PagerActivity): PagerViewModel.Args {
        val args = PagerActivityArgs.fromBundle(activity.intent.extras)
        return PagerViewModel.Args(
                args.position,
                args.directory
        )
    }

}