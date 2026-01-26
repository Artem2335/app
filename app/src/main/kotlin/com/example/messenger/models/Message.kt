package com.example.messenger.models

data class Message(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val recipientId: String = "",
    val content: String = "",
    val mediaUrl: String? = null,
    val mediaType: String? = null, // "photo", "video", "text"
    val timestamp: Long = System.currentTimeMillis()
)
