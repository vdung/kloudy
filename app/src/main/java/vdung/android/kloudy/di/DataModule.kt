package vdung.android.kloudy.di

import android.content.Context
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
import vdung.kodav.retrofit.WebDavConverterFactory
import javax.inject.Singleton

@Module
class DataModule {

    @Provides
    fun provideCallFactory(database: KloudyDatabase): Call.Factory = OkHttpClient.Builder()
            .addInterceptor(UserAuthenticator(database.userDao()))
            .build()

    @Provides
    fun provideRetrofit(): Retrofit.Builder {
        return Retrofit.Builder()
                .addConverterFactory(WebDavConverterFactory())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
    }

    @Provides
    fun provideNextcloudConfig(database: KloudyDatabase): NextcloudConfig {
        val user = database.userDao().getActiveUser() ?: throw IllegalStateException("no user")

        return NextcloudConfig(user)
    }

    @Provides
    fun provideNextcloudRepository(database: KloudyDatabase, config: NextcloudConfig, builder: Retrofit.Builder, callFactory: Call.Factory): NextcloudRepository {
        val service = builder
                .callFactory(callFactory)
                .baseUrl(config.user.server)
                .build()
                .create(NextcloudService::class.java)

        return NextcloudRepository(service, database.fileDao(), config)
    }

    @Provides
    @Singleton
    fun provideDatabase(context: Context): KloudyDatabase {
        return Room.databaseBuilder(context, KloudyDatabase::class.java, "kloudy").allowMainThreadQueries().build()
    }

    @Provides
    fun provideExoDataSource(context: Context, database: KloudyDatabase): com.google.android.exoplayer2.upstream.DataSource.Factory {
        val user = database.userDao().getActiveUser() ?: throw IllegalStateException("no user")
        val dataSourceFactory = DefaultHttpDataSourceFactory(Util.getUserAgent(context, "Kloudy"))
        dataSourceFactory.defaultRequestProperties.set("Authorization", Credentials.basic(user.username, user.password))

        return dataSourceFactory
    }
}