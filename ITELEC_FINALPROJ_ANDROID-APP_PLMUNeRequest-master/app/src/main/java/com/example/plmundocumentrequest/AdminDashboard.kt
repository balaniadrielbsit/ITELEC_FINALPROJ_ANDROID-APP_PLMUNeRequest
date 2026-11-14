package com.example.plmundocumentrequest

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale

@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboard(
    name: String,
    onLogout: () -> Unit,
    onRequestClick: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    var allRequests by remember { mutableStateOf<List<DocumentRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var archiveView by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        db.collection("requests")
            .orderBy("dateRequested", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    isLoading = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    allRequests = snapshot.documents.map { doc ->
                        DocumentRequest(
                            id = doc.id,
                            documentType = doc.getString("documentType") ?: "",
                            purpose = doc.getString("purpose") ?: "",
                            status = doc.getString("status") ?: "PENDING",
                            requestCode = doc.getString("requestCode") ?: "—",
                            userName = doc.getString("userName") ?: "Unknown Student",
                            studentId = doc.getString("studentId") ?: "N/A",
                            dateRequested = doc.getTimestamp("dateRequested")?.toDate()?.time ?: 0L
                        )
                    }
                    isLoading = false
                }
            }
    }

    val baseRequests = allRequests.filter {
        val s = it.status.uppercase()
        if (archiveView == null) {
            // CHANGED HERE: Looks for COMPLETED instead of CLEARED to hide from active list
            s != "CANCELLED" && s != "COMPLETED"
        } else {
            s == archiveView
        }
    }

    val filteredRequests = baseRequests.filter { req ->
        val matchesFilter = when (selectedFilter) {
            "ALL" -> true
            else -> req.status.equals(selectedFilter, ignoreCase = true)
        }

        val matchesSearch = if (searchQuery.isBlank()) {
            true
        } else {
            req.requestCode.contains(searchQuery, ignoreCase = true) ||
                    req.documentType.contains(searchQuery, ignoreCase = true) ||
                    req.userName.contains(searchQuery, ignoreCase = true) ||
                    req.studentId.contains(searchQuery, ignoreCase = true)
        }

        matchesFilter && matchesSearch
    }

    val activeOnly = allRequests.filter {
        val s = it.status.uppercase()
        s != "CANCELLED" && s != "COMPLETED"
    }
    val pendingCount = activeOnly.count { it.status.equals("PENDING", ignoreCase = true) }
    val processingCount = activeOnly.count { it.status.equals("PROCESSING", ignoreCase = true) }
    val readyCount = activeOnly.count { it.status.equals("READY", ignoreCase = true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (archiveView == null) "Admin Dashboard" else "Archive: $archiveView") },
                navigationIcon = {
                    if (archiveView != null) {
                        IconButton(onClick = { archiveView = null; selectedFilter = "ALL" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close Archive")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            if (archiveView == null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryCard("Pending", pendingCount, Color(0xFFFFA000), selectedFilter == "PENDING", Modifier.weight(1f)) { selectedFilter = if (selectedFilter == "PENDING") "ALL" else "PENDING" }
                    SummaryCard("Processing", processingCount, Color(0xFF1976D2), selectedFilter == "PROCESSING", Modifier.weight(1f)) { selectedFilter = if (selectedFilter == "PROCESSING") "ALL" else "PROCESSING" }
                    SummaryCard("Ready", readyCount, Color(0xFF388E3C), selectedFilter == "READY", Modifier.weight(1f)) { selectedFilter = if (selectedFilter == "READY") "ALL" else "READY" }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // CHANGED HERE: Archive view for "COMPLETED"
                    OutlinedButton(onClick = { archiveView = "COMPLETED" }, modifier = Modifier.weight(1f)) { Text("Completed") }
                    OutlinedButton(
                        onClick = { archiveView = "CANCELLED" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Cancelled") }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search name, ID, or code...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Filled.Clear, contentDescription = "Clear") }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(selectedFilter == "ALL", { selectedFilter = "ALL" }, label = { Text("All (${baseRequests.size})") })
                    FilterChip(selectedFilter == "PENDING", { selectedFilter = "PENDING" }, label = { Text("Pending") })
                    FilterChip(selectedFilter == "PROCESSING", { selectedFilter = "PROCESSING" }, label = { Text("Processing") })
                    FilterChip(selectedFilter == "READY", { selectedFilter = "READY" }, label = { Text("Ready") })
                }
            } else {
                Text(
                    text = "Showing ${archiveView?.lowercase()} requests",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            when {
                isLoading -> { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                filteredRequests.isEmpty() -> { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No requests found.") } }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredRequests, key = { it.id }) { request ->
                            AdminRequestCard(request, onClick = { onRequestClick(request.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryCard(label: String, count: Int, color: Color, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = if (isSelected) color else color.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(count.toString(), style = MaterialTheme.typography.headlineSmall, color = if (isSelected) Color.White else color, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = if (isSelected) Color.White else color)
        }
    }
}

@Composable
fun AdminRequestCard(request: DocumentRequest, onClick: () -> Unit) {
    val formattedDate = remember(request.dateRequested) {
        if (request.dateRequested > 0) {
            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(java.util.Date(request.dateRequested))
        } else "—"
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(request.documentType, style = MaterialTheme.typography.titleMedium)
                StatusBadge(request.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "👤 ${request.userName}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "🆔 Student ID: ${request.studentId}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Code: ${request.requestCode}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Purpose: ${request.purpose}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Date: $formattedDate",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}