package com.tinta.reader

import android.app.Application
import android.content.Context
import androidx.room.Room
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.tinta.reader.data.local.SettingsStore
import com.tinta.reader.data.local.TintaDatabase
import com.tinta.reader.data.remote.api.ManhwaApi
import com.tinta.reader.data.repository.CatalogRepository
import com.tinta.reader.data.repository.LibraryRepository
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/** Container dependensi sederhana (tanpa framework DI). */
class AppContainer(context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    // Satu klien dipakai API dan Coil agar header API key (bila ada) ikut ke gambar.
    val okHttp: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)   // scraping sumber bisa lambat
        .writeTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(Interceptor { chain ->
            val key = BuildConfig.API_KEY
            val req = if (key.isNotEmpty()) chain.request().newBuilder().header("x-api-key", key).build() else chain.request()
            chain.proceed(req)
        })
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            }
        }
        .build()

    private val api: ManhwaApi = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(okHttp)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(ManhwaApi::class.java)

    private val db = Room.databaseBuilder(context, TintaDatabase::class.java, "tinta.db").build()

    val catalog = CatalogRepository(api)
    val library = LibraryRepository(db.bookmarks(), db.history(), db.recentSearches())
    val settings = SettingsStore(context)
}

class TintaApp : Application(), ImageLoaderFactory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .okHttpClient(container.okHttp)
        .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.20).build() }
        .diskCache { DiskCache.Builder().directory(cacheDir.resolve("images")).maxSizeBytes(250L * 1024 * 1024).build() }
        .crossfade(false)
        .respectCacheHeaders(false) // server mengirim max-age 1 hari; cache lokal tetap dipakai
        .build()
}
