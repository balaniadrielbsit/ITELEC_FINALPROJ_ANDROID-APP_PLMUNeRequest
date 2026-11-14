package com.example.plmundocumentrequest

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class DocumentRequirement(
    val documentType: String,
    val requirements: List<String>,
    val fee: String,
    val processingTime: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequirementsScreen(onBack: () -> Unit) {

    // UPDATED: All fees are now ₱100.00
    val requirementList = listOf(
        DocumentRequirement("Transcript of Records (TOR)", listOf("Photocopy of Student ID"), "₱100.00", "5-7 working days"),
        DocumentRequirement("Certificate of Enrollment", listOf("Photocopy of Student ID"), "₱100.00", "2-3 working days"),
        DocumentRequirement("Good Moral Certificate", listOf("Photocopy of Student ID"), "₱100.00", "2-3 working days"),
        DocumentRequirement("Form 138 (Report Card)", listOf("Photocopy of Student ID"), "₱100.00", "2-3 working days"),
        DocumentRequirement("Diploma", listOf("Photocopy of Student ID"), "₱100.00", "7-10 working days"),
        DocumentRequirement("Certificate of Graduation", listOf("Photocopy of Student ID"), "₱100.00", "2-3 working days")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("List of Requirements") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Prepare these requirements before submitting your request to avoid multiple visits.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(requirementList) { req ->
                RequirementCard(req)
            }

            item {
                Text(
                    text = "ℹ️ Note: Requirements are subject to change. Please verify with the Registrar's Office.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun RequirementCard(req: DocumentRequirement) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(req.documentType, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Requirements:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))

            req.requirements.forEach { requirement ->
                Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "•", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = requirement, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("💰 Fee: ${req.fee}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("⏱ ${req.processingTime}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}