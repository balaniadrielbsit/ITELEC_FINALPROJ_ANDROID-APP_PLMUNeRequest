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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

data class DocumentInfo(
    val documentType: String,
    val requirements: List<String>,
    val fee: String,
    val processingTime: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDocumentScreen(
    userName: String,
    userId: String,
    studentId: String,
    onBack: () -> Unit,
    onSubmitted: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    // UPDATED: All fees are now ₱100.00, requirements are just the photocopy
    val documentCatalog = listOf(
        DocumentInfo("Transcript of Records (TOR)", listOf("Photocopy of Student ID"), "₱100.00", "5-7 working days"),
        DocumentInfo("Certificate of Enrollment", listOf("Photocopy of Student ID"), "₱100.00", "2-3 working days"),
        DocumentInfo("Good Moral Certificate", listOf("Photocopy of Student ID"), "₱100.00", "2-3 working days"),
        DocumentInfo("Form 138 (Report Card)", listOf("Photocopy of Student ID"), "₱100.00", "2-3 working days"),
        DocumentInfo("Diploma", listOf("Photocopy of Student ID"), "₱100.00", "7-10 working days"),
        DocumentInfo("Certificate of Graduation", listOf("Photocopy of Student ID"), "₱100.00", "2-3 working days")
    )

    var selectedDocument by remember { mutableStateOf<DocumentInfo?>(null) }
    var purpose by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    fun generateRequestCode(): String {
        val timestamp = System.currentTimeMillis()
        val random = (1000..9999).random()
        return "REQ-${timestamp.toString().takeLast(6)}-$random"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request a Document") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            Text("Student Information", style = MaterialTheme.typography.titleMedium)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Name: $userName", style = MaterialTheme.typography.bodyMedium)
                    Text("Student ID: $studentId", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("Select Document", style = MaterialTheme.typography.titleMedium)
            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = !dropdownExpanded }
            ) {
                OutlinedTextField(
                    value = selectedDocument?.documentType ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Document Type") },
                    placeholder = { Text("Select a document type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    documentCatalog.forEach { doc ->
                        DropdownMenuItem(
                            text = { Text(doc.documentType) },
                            onClick = {
                                selectedDocument = doc
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }

            selectedDocument?.let { doc ->
                RequirementsCard(doc)
            }

            Text("Purpose", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = purpose,
                onValueChange = { purpose = it },
                label = { Text("Why do you need this document?") },
                placeholder = { Text("e.g., Job application, Scholarship") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val doc = selectedDocument ?: return@Button
                    if (purpose.isBlank()) {
                        Toast.makeText(context, "Please enter a purpose", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isLoading = true
                    val requestCode = generateRequestCode()

                    val request = hashMapOf(
                        "userId" to userId,
                        "userName" to userName,
                        "studentId" to studentId,
                        "documentType" to doc.documentType,
                        "purpose" to purpose.trim(),
                        "status" to "PENDING",
                        "requestCode" to requestCode,
                        "dateRequested" to FieldValue.serverTimestamp()
                    )

                    db.collection("requests")
                        .add(request)
                        .addOnSuccessListener {
                            isLoading = false
                            Toast.makeText(context, "Request submitted! Code: $requestCode", Toast.LENGTH_LONG).show()
                            onSubmitted()
                        }
                        .addOnFailureListener { e ->
                            isLoading = false
                            Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                },
                enabled = !isLoading && selectedDocument != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Submit Request")
                }
            }

            Text(
                text = "You will receive a unique request code for pickup. Show this code when claiming your document.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RequirementsCard(doc: DocumentInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("📋 Requirements for ${doc.documentType}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            doc.requirements.forEach { requirement ->
                Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "•", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = requirement, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("💰 ${doc.fee}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text("⏱ ${doc.processingTime}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }
}