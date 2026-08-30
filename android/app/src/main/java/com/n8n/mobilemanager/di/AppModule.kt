package com.n8n.mobilemanager.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.n8n.mobilemanager.BuildConfig
import com.n8n.mobilemanager.data.local.ApiKeyCipher
import com.n8n.mobilemanager.data.local.InstanceDao
import com.n8n.mobilemanager.data.local.N8nDatabase
import com.n8n.mobilemanager.data.local.PreferencesManager
import com.n8n.mobilemanager.data.model.N8nInstance
import com.n8n.mobilemanager.data.remote.N8nApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Module Hilt pour l'injection de dépendances
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ==================== Database ====================
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): N8nDatabase {
        return Room.databaseBuilder(
            context,
            N8nDatabase::class.java,
            N8nDatabase.DATABASE_NAME
        )
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    db.query("SELECT id, apiKey FROM instances").use { cursor ->
                        val idColumn = cursor.getColumnIndexOrThrow("id")
                        val apiKeyColumn = cursor.getColumnIndexOrThrow("apiKey")
                        while (cursor.moveToNext()) {
                            val id = cursor.getLong(idColumn)
                            val storedApiKey = cursor.getString(apiKeyColumn)
                            if (!ApiKeyCipher.isEncrypted(storedApiKey)) {
                                runCatching {
                                    db.execSQL(
                                        "UPDATE instances SET apiKey = ? WHERE id = ?",
                                        arrayOf(ApiKeyCipher.encrypt(storedApiKey), id)
                                    )
                                }
                            }
                        }
                    }
                }
            })
            .build()
    }
    
    @Provides
    @Singleton
    fun provideInstanceDao(database: N8nDatabase): InstanceDao {
        return database.instanceDao()
    }

    // ==================== Preferences ====================
    
    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager {
        return PreferencesManager(context)
    }

    // ==================== Network ====================
    
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
            redactHeader("Authorization")
            redactHeader("Cookie")
            redactHeader("Set-Cookie")
            redactHeader("X-N8N-API-KEY")
        }
    }
    
    @Provides
    @Singleton
    fun provideApiServiceFactory(loggingInterceptor: HttpLoggingInterceptor): ApiServiceFactory {
        return ApiServiceFactory(loggingInterceptor)
    }
}

/**
 * Factory pour créer des instances de l'API service avec différentes configurations
 */
class ApiServiceFactory(
    private val loggingInterceptor: HttpLoggingInterceptor
) {
    
    private val retrofitCache = mutableMapOf<String, Retrofit>()
    
    fun create(instance: N8nInstance): N8nApiService {
        val baseUrl = instance.baseUrl.trimEnd('/') + "/"
        val cacheKey = "${baseUrl}:${instance.apiKey}"
        
        val retrofit = synchronized(retrofitCache) {
            retrofitCache.getOrPut(cacheKey) {
                val authInterceptor = Interceptor { chain ->
                    val request = chain.request().newBuilder()
                        .addHeader("X-N8N-API-KEY", instance.apiKey)
                        .addHeader("Accept", "application/json")
                        .addHeader("Content-Type", "application/json")
                        .build()
                    chain.proceed(request)
                }

                val okHttpClient = OkHttpClient.Builder()
                    .addInterceptor(authInterceptor)
                    .addInterceptor(loggingInterceptor)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(45, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build()

                Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
            }
        }
        
        return retrofit.create(N8nApiService::class.java)
    }

    fun createWithCookie(baseUrl: String, cookie: String? = null): N8nApiService {
        val finalBaseUrl = baseUrl.trimEnd('/') + "/"
        // Pas de cache pour les sessions temporaires
        
        val authInterceptor = Interceptor { chain ->
            val builder = chain.request().newBuilder()
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
            
            if (cookie != null) {
                builder.addHeader("Cookie", cookie)
            }
            
            chain.proceed(builder.build())
        }
        
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        
        return Retrofit.Builder()
            .baseUrl(finalBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(N8nApiService::class.java)
    }
    
    fun clearCache() {
        synchronized(retrofitCache) {
            retrofitCache.clear()
        }
    }
}
