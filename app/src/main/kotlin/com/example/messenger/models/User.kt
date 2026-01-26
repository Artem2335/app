package com.example.messenger.models

data class User(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
