package com.example.messenger.firebase

import com.example.messenger.models.Message
import com.example.messenger.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class FirebaseManager {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    private val storage = FirebaseStorage.getInstance().reference

    // Auth Methods
    suspend fun registerUser(
        email: String,
        password: String,
        username: String
    ): Result<String> = try {
        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
        val userId = authResult.user?.uid ?: throw Exception("User ID is null")

        val user = User(
            uid = userId,
            username = username,
            email = email
        )

        database.child("users").child(userId).setValue(user).await()
        Result.success(userId)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun loginUser(email: String, password: String): Result<String> = try {
        val authResult = auth.signInWithEmailAndPassword(email, password).await()
        val userId = authResult.user?.uid ?: throw Exception("User ID is null")
        Result.success(userId)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun logout() {
        auth.signOut()
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    fun isLoggedIn(): Boolean = auth.currentUser != null

    // User Methods
    suspend fun getUser(userId: String): Result<User> = try {
        val snapshot = database.child("users").child(userId).get().await()
        val user = snapshot.getValue(User::class.java) ?: throw Exception("User not found")
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getAllUsers(): Result<List<User>> = try {
        val snapshot = database.child("users").get().await()
        val users = mutableListOf<User>()
        snapshot.children.forEach { child ->
            val user = child.getValue(User::class.java)
            user?.let { users.add(it) }
        }
        Result.success(users)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Message Methods
    suspend fun sendMessage(message: Message): Result<String> = try {
        val messageId = database.child("messages").push().key ?: throw Exception("Message ID is null")
        val messageWithId = message.copy(id = messageId)
        database.child("messages").child(messageId).setValue(messageWithId).await()
        Result.success(messageId)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getMessages(
        currentUserId: String,
        otherUserId: String
    ): Result<List<Message>> = try {
        val snapshot = database.child("messages").get().await()
        val messages = mutableListOf<Message>()
        snapshot.children.forEach { child ->
            val message = child.getValue(Message::class.java)
            message?.let {
                if ((it.senderId == currentUserId && it.recipientId == otherUserId) ||
                    (it.senderId == otherUserId && it.recipientId == currentUserId)
                ) {
                    messages.add(it)
                }
            }
        }
        messages.sortBy { it.timestamp }
        Result.success(messages)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Upload Media
    suspend fun uploadMedia(
        userId: String,
        fileName: String,
        fileData: ByteArray,
        mediaType: String
    ): Result<String> = try {
        val ref = storage.child("users").child(userId).child(mediaType).child(fileName)
        ref.putBytes(fileData).await()
        val downloadUrl = ref.downloadUrl.await().toString()
        Result.success(downloadUrl)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
