package com.n8n.mobilemanager.di

import android.content.Context
import androidx.room.Room
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
            .fallbackToDestructiveMigration()
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
            level = HttpLoggingInterceptor.Level.BODY
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
        
        val retrofit = retrofitCache.getOrPut(cacheKey) {
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
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()
            
            Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        
        return retrofit.create(N8nApiService::class.java)
    }
    
    fun clearCache() {
        retrofitCache.clear()
    }
}
