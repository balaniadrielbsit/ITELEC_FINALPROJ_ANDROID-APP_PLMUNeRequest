package com.example.plmundocumentrequest

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailsScreen(
    requestId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    var request by remember { mutableStateOf<DocumentRequest?>(null) }
    var requestUserId by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isUpdating by remember { mutableStateOf(false) }

    var showCancelDialog by remember { mutableStateOf(false) }

    LaunchedEffect(requestId) {
        db.collection("requests").document(requestId).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    request = DocumentRequest(
                        id = doc.id,
                        documentType = doc.getString("documentType") ?: "",
                        purpose = doc.getString("purpose") ?: "",
                        status = doc.getString("status") ?: "PENDING",
                        requestCode = doc.getString("requestCode") ?: "—",
                        userName = doc.getString("userName") ?: "Unknown Student",
                        studentId = doc.getString("studentId") ?: "N/A",
                        dateRequested = doc.getTimestamp("dateRequested")?.toDate()?.time ?: 0L
                    )
                    requestUserId = doc.getString("userId") ?: ""
                }
                isLoading = false
            }
            .addOnFailureListener { isLoading = false }
    }

    fun createNotification(newStatus: String) {
        if (requestUserId.isBlank()) return

        val title = when (newStatus) {
            "PROCESSING" -> "Request Processing"
            "READY" -> "Document Ready!"
            "CANCELLED" -> "Request Cancelled"
            "COMPLETED" -> "Document Claimed"
            else -> "Status Update"
        }

        val message = when (newStatus) {
            "PROCESSING" -> "Your ${request?.documentType} is now being processed."
            "READY" -> "Your ${request?.documentType} is ready for pickup. Show code ${request?.requestCode} at the Registrar's Office."
            "CANCELLED" -> "Your ${request?.documentType} request has been cancelled by the Registrar."
            "COMPLETED" -> "Your ${request?.documentType} has been successfully claimed. Thank you!"
            else -> "Your request status is now $newStatus."
        }

        val notification = hashMapOf(
            "userId" to requestUserId,
            "title" to title,
            "message" to message,
            "requestId" to requestId,
            "isRead" to false,
            "createdAt" to FieldValue.serverTimestamp()
        )

        db.collection("notifications").add(notification)
    }

    fun updateStatus(newStatus: String) {
        isUpdating = true
        db.collection("requests").document(requestId)
            .update("status", newStatus)
            .addOnSuccessListener {
                createNotification(newStatus)
                isUpdating = false
                Toast.makeText(context, "Status updated to $newStatus", Toast.LENGTH_SHORT).show()
                request = request?.copy(status = newStatus)
            }
            .addOnFailureListener { e ->
                isUpdating = false
                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            request == null -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("Request not found.")
                }
            }
            else -> {
                val r = request!!
                val formattedDate = remember(r.dateRequested) {
                    if (r.dateRequested > 0) {
                        SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(java.util.Date(r.dateRequested))
                    } else "—"
                }

                val currentStatus = r.status.uppercase()
                val isActive = currentStatus == "PENDING" || currentStatus == "PROCESSING" || currentStatus == "READY"

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Request Code", style = MaterialTheme.typography.titleMedium)
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                        Text(
                            text = "🎫 ${r.requestCode}",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }

                    Text("Requester Information", style = MaterialTheme.typography.titleMedium)
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Name: ${r.userName}", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Student ID: ${r.studentId}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Text("Document Information", style = MaterialTheme.typography.titleMedium)
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Type: ${r.documentType}", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Purpose: ${r.purpose}", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Requested: $formattedDate", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Current Status", style = MaterialTheme.typography.titleMedium)
                    StatusBadge(r.status)

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isActive) {
                        Text("Update Status", style = MaterialTheme.typography.titleMedium)

                        // DYNAMIC BUTTONS BASED ON STATUS
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                            // Only show PROCESSING if currently PENDING
                            if (currentStatus == "PENDING") {
                                Button(
                                    onClick = { updateStatus("PROCESSING") },
                                    enabled = !isUpdating,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000))
                                ) { Text("Mark as PROCESSING") }
                            }

                            // Only show READY if currently PROCESSING
                            if (currentStatus == "PROCESSING") {
                                Button(
                                    onClick = { updateStatus("READY") },
                                    enabled = !isUpdating,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
                                ) { Text("Mark as READY FOR PICKUP") }
                            }

                            // Only show COMPLETED if currently READY
                            if (currentStatus == "READY") {
                                Button(
                                    onClick = { updateStatus("COMPLETED") },
                                    enabled = !isUpdating,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                                ) {
                                    Text("Mark as COMPLETED (Picked up)")
                                }
                            }
                        }

                        // Cancel Request Button (Always available for active requests)
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { showCancelDialog = true },
                            enabled = !isUpdating,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) { Text("Cancel Request") }

                    } else {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "This request is already closed and cannot be modified.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (isUpdating) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { if (!isUpdating) showCancelDialog = false },
            title = { Text("Cancel Request?") },
            text = { Text("Are you sure you want to cancel this request? The student will be notified.") },
            confirmButton = {
                TextButton(
                    onClick = { showCancelDialog = false; updateStatus("CANCELLED") },
                    enabled = !isUpdating,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Yes, Cancel") }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }, enabled = !isUpdating) { Text("No") }
            }
        )
    }
}