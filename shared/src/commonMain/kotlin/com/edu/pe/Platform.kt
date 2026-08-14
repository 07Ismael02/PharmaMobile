package com.edu.pe

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform