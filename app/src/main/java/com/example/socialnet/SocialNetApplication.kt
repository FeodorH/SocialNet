package com.example.socialnet

import android.app.Application
import com.cloudinary.android.MediaManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SocialNetApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        require(BuildConfig.CLOUDINARY_CLOUD_NAME.isNotBlank()) {
            "CLOUDINARY_CLOUD_NAME can`t contains in local.properties"
        }
        require(BuildConfig.CLOUDINARY_UPLOAD_PRESET.isNotBlank()) {
            "CLOUDINARY_UPLOAD_PRESET can`t contains in local.properties"
        }

        MediaManager.init(
            this,
            mapOf(
                "cloud_name" to BuildConfig.CLOUDINARY_CLOUD_NAME,
                "upload_preset" to BuildConfig.CLOUDINARY_UPLOAD_PRESET,
            ),
        )
    }
}