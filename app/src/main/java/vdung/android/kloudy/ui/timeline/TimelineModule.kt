package vdung.android.kloudy.ui.timeline

import androidx.lifecycle.ViewModel
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import vdung.android.kloudy.di.FragmentScoped
import vdung.android.kloudy.di.ViewModelKey

@Module
abstract class TimelineModule {

    @FragmentScoped
    @ContributesAndroidInjector
    abstract fun timelineFragment(): TimelineFragment


    @Binds
    @IntoMap
    @ViewModelKey(TimelineViewModel::class)
    abstract fun bindViewModel(viewModel: TimelineViewModel): ViewModel

}