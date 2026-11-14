package com.example.plmundocumentrequest

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale

data class DocumentRequest(
    val id: String = "",
    val documentType: String = "",
    val purpose: String = "",
    val status: String = "",
    val requestCode: String = "",
    val userName: String = "",
    val studentId: String = "",
    val dateRequested: Long = 0L,
    val hiddenFromStudent: Boolean = false // NEW: Tells the student's app to hide this
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRequestsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var requests by remember { mutableStateOf<List<DocumentRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var requestToCancel by remember { mutableStateOf<DocumentRequest?>(null) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        db.collection("requests")
            .whereEqualTo("userId", currentUserId)
            .orderBy("dateRequested", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    isLoading = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val freshList = snapshot.documents.map { doc ->
                        DocumentRequest(
                            id = doc.id,
                            documentType = doc.getString("documentType") ?: "",
                            purpose = doc.getString("purpose") ?: "",
                            status = doc.getString("status") ?: "PENDING",
                            requestCode = doc.getString("requestCode") ?: "—",
                            userName = doc.getString("userName") ?: "",
                            studentId = doc.getString("studentId") ?: "",
                            dateRequested = doc.getTimestamp("dateRequested")?.toDate()?.time ?: 0L,
                            hiddenFromStudent = doc.getBoolean("hiddenFromStudent") ?: false
                        )
                    }

                    // Merge optimistic updates (keep instantly cancelled items as CANCELLED)
                    requests = freshList.map { fresh ->
                        val local = requests.find { it.id == fresh.id }
                        if (local?.status == "CANCELLED") fresh.copy(status = "CANCELLED") else fresh
                    }
                    isLoading = false
                }
            }
    }

    fun cancelRequest(request: DocumentRequest) {
        requests = requests.map {
            if (it.id == request.id) it.copy(status = "CANCELLED") else it
        }
        requestToCancel = null

        db.collection("requests").document(request.id)
            .update("status", "CANCELLED")
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to cancel: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    fun clearHistory(historyList: List<DocumentRequest>) {
        val historyIds = historyList.map { it.id }
        if (historyIds.isEmpty()) return

        // 1. Instantly hide from UI
        requests = requests.map {
            if (it.id in historyIds) it.copy(hiddenFromStudent = true) else it
        }
        showClearHistoryDialog = false

        // 2. Update Firestore in background (Soft Delete)
        val batch = db.batch()
        historyIds.forEach { id ->
            val docRef = db.collection("requests").document(id)
            batch.update(docRef, "hiddenFromStudent", true)
        }
        batch.commit()
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error clearing history: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // Filter out hidden requests for the student
    val visibleRequests = requests.filter { !it.hiddenFromStudent }

    val activeRequests = visibleRequests.filter {
        val s = it.status.uppercase()
        s == "PENDING" || s == "PROCESSING" || s == "READY"
    }

    val historyRequests = visibleRequests.filter {
        val s = it.status.uppercase()
        s == "COMPLETED" || s == "CANCELLED" || s == "REJECTED"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Requests") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (visibleRequests.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("📭 No requests yet", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (activeRequests.isNotEmpty()) {
                        item { Text("Active Requests", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                        items(activeRequests, key = { it.id }) { request ->
                            RequestCard(request = request, onCancelClick = { requestToCancel = request })
                        }
                    }

                    if (historyRequests.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(modifier = Modifier.padding(vertical = 8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                TextButton(onClick = { showClearHistoryDialog = true }) {
                                    Text("Clear History", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        items(historyRequests, key = { it.id }) { request ->
                            RequestCard(request = request, onCancelClick = { })
                        }
                    }
                }
            }
        }
    }

    if (requestToCancel != null) {
        AlertDialog(
            onDismissRequest = { requestToCancel = null },
            title = { Text("Cancel Request?") },
            text = { Text("Are you sure you want to cancel your request for ${requestToCancel?.documentType}?") },
            confirmButton = {
                TextButton(
                    onClick = { requestToCancel?.let { cancelRequest(it) } },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Yes, Cancel") }
            },
            dismissButton = {
                TextButton(onClick = { requestToCancel = null }) { Text("No") }
            }
        )
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear History?") },
            text = { Text("This will remove these requests from your view. The Registrar's Office will still keep a copy of these records.") },
            confirmButton = {
                TextButton(
                    onClick = { clearHistory(historyRequests) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun RequestCard(request: DocumentRequest, onCancelClick: () -> Unit) {
    val formattedDate = remember(request.dateRequested) {
        if (request.dateRequested > 0) {
            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(java.util.Date(request.dateRequested))
        } else "—"
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("📄 ${request.documentType}", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Purpose: ${request.purpose}", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Requested: $formattedDate", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))

            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                Text(
                    text = "🎫 ${request.requestCode}",
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(request.status)

                if (request.status.equals("PENDING", ignoreCase = true)) {
                    TextButton(
                        onClick = onCancelClick,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Cancel") }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (color, label) = when (status.uppercase()) {
        "PENDING" -> Color(0xFFFFA000) to "PENDING"
        "PROCESSING" -> Color(0xFF1976D2) to "PROCESSING"
        "READY" -> Color(0xFF388E3C) to "READY FOR PICKUP"
        "COMPLETED" -> Color(0xFF388E3C) to "COMPLETED"
        "CANCELLED" -> Color(0xFFD32F2F) to "CANCELLED"
        "REJECTED" -> Color(0xFFD32F2F) to "REJECTED"
        else -> Color.Gray to status
    }

    Surface(color = color.copy(alpha = 0.15f), shape = MaterialTheme.shapes.small) {
        Text(text = "● $label", color = color, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}