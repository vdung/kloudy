package vdung.android.kloudy.ui.login

import androidx.lifecycle.ViewModel
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import vdung.android.kloudy.di.FragmentScoped
import vdung.android.kloudy.di.ViewModelKey

@Module
abstract class LoginModule {
    @FragmentScoped
    @ContributesAndroidInjector
    abstract fun fragment(): LoginFragment

    @Binds
    @IntoMap
    @ViewModelKey(LoginViewModel::class)
    abstract fun bindViewModel(viewModel: LoginViewModel): ViewModel
}