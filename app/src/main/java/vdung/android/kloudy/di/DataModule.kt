package vdung.android.kloudy.di

import android.content.Context
import android.preference.PreferenceManager
import androidx.room.Room
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.util.Util
import dagger.Module
import dagger.Provides
import okhttp3.Call
import okhttp3.Credentials
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import vdung.android.kloudy.data.KloudyDatabase
import vdung.android.kloudy.data.nextcloud.NextcloudConfig
import vdung.android.kloudy.data.nextcloud.NextcloudRepository
import vdung.android.kloudy.data.nextcloud.NextcloudService
import vdung.android.kloudy.data.retrofit.UserAuthenticator
import vdung.android.kloudy.data.user.KeyStoreWrapper
import vdung.android.kloudy.data.user.UserRepository
import vdung.kodav.retrofit.WebDavConverterFactory
import java.io.File
import javax.inject.Singleton

@Module
class DataModule {

    @Provides
    fun provideCallFactory(userRepository: UserRepository): Call.Factory = OkHttpClient.Builder()
            .addInterceptor(UserAuthenticator(userRepository))
            .build()

    @Provides
    fun provideRetrofit(): Retrofit.Builder {
        return Retrofit.Builder()
                .addConverterFactory(WebDavConverterFactory())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
    }

    @Provides
    @Singleton
    fun provideUserRepository(context: Context): UserRepository {
        val keyStorage = KeyStoreWrapper(context)
        return UserRepository(keyStorage, PreferenceManager.getDefaultSharedPreferences(context))
    }

    @Provides
    fun provideNextcloudConfig(userRepository: UserRepository): NextcloudConfig {
        val user = userRepository.getUser() ?: throw IllegalStateException("no user")

        return NextcloudConfig(user)
    }

    @Provides
    fun provideNextcloudRepository(context: Context, database: KloudyDatabase, config: NextcloudConfig, builder: Retrofit.Builder, callFactory: Call.Factory): NextcloudRepository {
        val service = builder
                .callFactory(callFactory)
                .baseUrl(config.user.server)
                .build()
                .create(NextcloudService::class.java)
        
        return NextcloudRepository(service, database.fileDao(), database.fileMetadataDao(), config, File(context.externalCacheDir, "downloads"))
    }

    @Provides
    @Singleton
    fun provideDatabase(context: Context): KloudyDatabase {
        return Room.databaseBuilder(context, KloudyDatabase::class.java, "kloudy").build()
    }

    @Provides
    fun provideExoDataSource(context: Context, userRepository: UserRepository): com.google.android.exoplayer2.upstream.DataSource.Factory {
        val user = userRepository.getUser() ?: throw IllegalStateException("no user")

        val dataSourceFactory = DefaultHttpDataSourceFactory(Util.getUserAgent(context, "Kloudy"))
        dataSourceFactory.defaultRequestProperties.set("Authorization", Credentials.basic(user.username, user.password))

        return dataSourceFactory
    }
}