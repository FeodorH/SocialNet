package com.example.socialnet.core.utils

import java.util.UUID

object IdGenerator {
    fun generate() : String =
        UUID.randomUUID().toString()
}