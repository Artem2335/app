package com.example.messenger.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.messenger.R
import com.example.messenger.databinding.ActivityChatBinding
import com.example.messenger.firebase.FirebaseManager
import com.example.messenger.models.Message
import com.example.messenger.models.User
import com.example.messenger.ui.adapters.MessageAdapter
import com.example.messenger.ui.adapters.UsersAdapter
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val firebaseManager = FirebaseManager()
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var usersAdapter: UsersAdapter
    private var currentUser: User? = null
    private var selectedUser: User? = null

    private val photoPickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                uploadAndSendMedia(uri, "photo")
            }
        }

    private val videoPickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                uploadAndSendMedia(uri, "video")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userId = firebaseManager.getCurrentUserId()
        if (userId == null) {
            navigateToAuth()
            return
        }

        loadCurrentUser(userId)
        setupAdapters()
        setupListeners()
        loadUsers()
    }

    private fun loadCurrentUser(userId: String) {
        lifecycleScope.launch {
            val result = firebaseManager.getUser(userId)
            result.onSuccess { user ->
                currentUser = user
                messageAdapter = MessageAdapter(userId)
            }
            result.onFailure { error ->
                showError(error.message ?: "Failed to load user")
            }
        }
    }

    private fun setupAdapters() {
        // Users adapter
        usersAdapter = UsersAdapter { user ->
            selectUser(user)
        }
        binding.usersRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity)
            adapter = usersAdapter
        }

        // Messages adapter (will be set after loading current user)
        binding.messagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
        }
    }

    private fun setupListeners() {
        binding.apply {
            // Send message button
            sendButton.setOnClickListener {
                sendTextMessage()
            }

            // Photo picker
            photoButton.setOnClickListener {
                photoPickerLauncher.launch("image/*")
            }

            // Video picker
            videoButton.setOnClickListener {
                videoPickerLauncher.launch("video/*")
            }

            // Logout button
            logoutButton.setOnClickListener {
                logout()
            }
        }
    }

    private fun loadUsers() {
        lifecycleScope.launch {
            val result = firebaseManager.getAllUsers()
            result.onSuccess { users ->
                val otherUsers = users.filter { it.uid != currentUser?.uid }
                usersAdapter.setUsers(otherUsers)
            }
            result.onFailure { error ->
                showError(error.message ?: "Failed to load users")
            }
        }
    }

    private fun selectUser(user: User) {
        selectedUser = user
        binding.apply {
            headerTitle.text = user.username
            usersContainer.visibility = View.GONE
            chatContainer.visibility = View.VISIBLE
        }
        loadMessages()
    }

    private fun loadMessages() {
        val current = currentUser ?: return
        val selected = selectedUser ?: return

        lifecycleScope.launch {
            val result = firebaseManager.getMessages(current.uid, selected.uid)
            result.onSuccess { messages ->
                messageAdapter.setMessages(messages)
                binding.messagesRecyclerView.apply {
                    adapter = messageAdapter
                    scrollToPosition(messageAdapter.itemCount - 1)
                }
            }
            result.onFailure { error ->
                showError(error.message ?: "Failed to load messages")
            }
        }
    }

    private fun sendTextMessage() {
        val text = binding.messageInput.text.toString().trim()
        if (text.isEmpty()) return

        val message = Message(
            senderId = currentUser?.uid ?: return,
            senderName = currentUser?.username ?: "Unknown",
            recipientId = selectedUser?.uid ?: return,
            content = text,
            mediaType = "text"
        )

        binding.messageInput.setText("")
        lifecycleScope.launch {
            val result = firebaseManager.sendMessage(message)
            result.onFailure { error ->
                showError(error.message ?: "Failed to send message")
            }
            loadMessages()
        }
    }

    private fun uploadAndSendMedia(uri: Uri, mediaType: String) {
        val current = currentUser ?: return
        val selected = selectedUser ?: return

        lifecycleScope.launch {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val fileData = inputStream?.readBytes() ?: return@launch
                inputStream.close()

                val fileName = "${System.currentTimeMillis()}_${mediaType}"
                val result = firebaseManager.uploadMedia(
                    current.uid,
                    fileName,
                    fileData,
                    mediaType
                )

                result.onSuccess { mediaUrl ->
                    val message = Message(
                        senderId = current.uid,
                        senderName = current.username,
                        recipientId = selected.uid,
                        content = "Sent a $mediaType",
                        mediaUrl = mediaUrl,
                        mediaType = mediaType
                    )
                    val sendResult = firebaseManager.sendMessage(message)
                    sendResult.onSuccess {
                        loadMessages()
                    }
                    sendResult.onFailure { error ->
                        showError(error.message ?: "Failed to send media")
                    }
                }
                result.onFailure { error ->
                    showError(error.message ?: "Failed to upload media")
                }
            } catch (e: Exception) {
                showError("Error: ${e.message}")
            }
        }
    }

    private fun logout() {
        lifecycleScope.launch {
            firebaseManager.logout()
            navigateToAuth()
        }
    }

    private fun navigateToAuth() {
        val intent = Intent(this, AuthActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
