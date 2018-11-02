package vdung.android.kloudy.ui.album

import androidx.lifecycle.ViewModel
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import vdung.android.kloudy.di.FragmentScoped
import vdung.android.kloudy.di.ViewModelKey

@Module
abstract class AlbumModule {

    @FragmentScoped
    @ContributesAndroidInjector
    abstract fun albumListFragment(): AlbumListFragment

    @FragmentScoped
    @ContributesAndroidInjector
    abstract fun albumFragment(): AlbumFragment


    @Binds
    @IntoMap
    @ViewModelKey(AlbumViewModel::class)
    abstract fun bindViewModel(viewModel: AlbumViewModel): ViewModel

}