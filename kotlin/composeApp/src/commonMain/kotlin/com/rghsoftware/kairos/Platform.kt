package com.rghsoftware.kairos

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
