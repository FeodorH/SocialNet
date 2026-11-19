package com.example.socialnet.data.di

import com.example.socialnet.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CloudinaryConfigModule {

    @Provides
    @Singleton
    @Named("cloudinaryCloudName")
    fun provideCloudinaryCloudName(): String = BuildConfig.CLOUDINARY_CLOUD_NAME

    @Provides
    @Singleton
    @Named("cloudinaryUploadPreset")
    fun provideCloudinaryUploadPreset(): String = BuildConfig.CLOUDINARY_UPLOAD_PRESET
}