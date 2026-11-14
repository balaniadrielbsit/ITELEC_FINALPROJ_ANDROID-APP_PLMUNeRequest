package com.example.plmundocumentrequest

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboard(
    name: String,
    onLogout: () -> Unit,
    onRequestDocument: () -> Unit,
    onMyRequests: () -> Unit,
    onNotifications: () -> Unit,
    onRequirements: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var unreadCount by remember { mutableStateOf(0) }

    // Real-time listener for unread notifications
    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotBlank()) {
            db.collection("notifications")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("isRead", false)
                .addSnapshotListener { snapshot, _ ->
                    unreadCount = snapshot?.size() ?: 0
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Student Dashboard") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.Filled.ExitToApp,
                            contentDescription = "Logout"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Welcome, $name!",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "What would you like to do today?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            DashboardCard(
                title = "Request a Document",
                subtitle = "Submit a new document request online",
                icon = Icons.Filled.Add,
                onClick = onRequestDocument,
                badgeCount = 0
            )

            DashboardCard(
                title = "My Requests",
                subtitle = "Track the status of your document requests",
                icon = Icons.Filled.List,
                onClick = onMyRequests,
                badgeCount = 0
            )

            DashboardCard(
                title = "Notifications",
                subtitle = "View updates about your requests",
                icon = Icons.Filled.Notifications,
                onClick = onNotifications,
                badgeCount = unreadCount
            )

            DashboardCard(
                title = "List of Requirements",
                subtitle = "See what you need to prepare before requesting",
                icon = Icons.Filled.Checklist,
                onClick = onRequirements,
                badgeCount = 0
            )
        }
    }
}

@Composable
fun DashboardCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    badgeCount: Int
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (badgeCount > 0) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ) {
                    Text(badgeCount.toString())
                }
            }
        }
    }
}