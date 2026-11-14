package com.example.plmundocumentrequest

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale

data class NotificationItem(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val isRead: Boolean = false,
    val createdAt: Long = 0L
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var notifications by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showClearDialog by remember { mutableStateOf(false) }

    // We don't need an isClearing loading state anymore since we delete instantly
    LaunchedEffect(Unit) {
        db.collection("notifications")
            .whereEqualTo("userId", currentUserId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    isLoading = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val freshList = snapshot.documents.map { doc ->
                        NotificationItem(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            message = doc.getString("message") ?: "",
                            isRead = doc.getBoolean("isRead") ?: false,
                            createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                        )
                    }
                    notifications = freshList.map { fresh ->
                        val local = notifications.find { it.id == fresh.id }
                        if (local?.isRead == true) fresh.copy(isRead = true) else fresh
                    }
                    isLoading = false
                }
            }
    }

    fun markAsRead(notificationId: String) {
        notifications = notifications.map {
            if (it.id == notificationId) it.copy(isRead = true) else it
        }
        db.collection("notifications").document(notificationId)
            .update("isRead", true)
    }

    fun clearReadNotifications() {
        val readIds = notifications.filter { it.isRead }.map { it.id }
        if (readIds.isEmpty()) {
            showClearDialog = false
            return
        }

        // 1. INSTANTLY remove them from the UI
        notifications = notifications.filter { !it.isRead }
        showClearDialog = false

        // 2. Delete from Firestore in the background
        val batch = db.batch()
        readIds.forEach { id ->
            val docRef = db.collection("notifications").document(id)
            batch.delete(docRef)
        }
        batch.commit()
    }

    val hasReadNotifications = notifications.any { it.isRead }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (hasReadNotifications) {
                        TextButton(onClick = { showClearDialog = true }) {
                            Text(
                                text = "Clear Notifications",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                notifications.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("🔔 No notifications yet", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "You'll see updates here when your request status changes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(notifications, key = { it.id }) { notif ->
                            NotificationCard(
                                notif = notif,
                                onClick = { markAsRead(notif.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Notifications?") },
            text = { Text("This will permanently remove all notifications that you have already read. Unread notifications will remain safe.") },
            confirmButton = {
                TextButton(
                    onClick = { clearReadNotifications() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear Read Notifications")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun NotificationCard(notif: NotificationItem, onClick: () -> Unit) {
    val formattedDate = remember(notif.createdAt) {
        if (notif.createdAt > 0) {
            SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
                .format(java.util.Date(notif.createdAt))
        } else "—"
    }

    val cardColor = if (notif.isRead) {
        Color(0xFFE0E0E0)  // Soft gray for read
    } else {
        MaterialTheme.colorScheme.primaryContainer  // Highlight for unread
    }

    val titleWeight = if (notif.isRead) FontWeight.Normal else FontWeight.Bold

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (!notif.isRead) onClick()
            },
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = notif.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = titleWeight
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = notif.message,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "• $formattedDate",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}