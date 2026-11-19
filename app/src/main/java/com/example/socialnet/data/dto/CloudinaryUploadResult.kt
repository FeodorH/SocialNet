package com.example.socialnet.data.dto

data class CloudinaryUploadResult(
    val secureUrl: String,
    val publicId: String,
    val deleteToken: String?
)