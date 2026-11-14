package com.example.plmundocumentrequest

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf("loading") }
    var userRole by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }
    var userStudentId by remember { mutableStateOf("") }
    var selectedRequestId by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            fetchUserData(currentUser.uid) { role, name, studentId ->
                userRole = role
                userName = name
                userStudentId = studentId
                currentScreen = "dashboard"
            }
        } else {
            currentScreen = "login"
        }
    }

    when (currentScreen) {
        "loading" -> LoadingScreen()

        "login" -> LoginScreen(
            onLoginSuccess = { role, name, studentId ->
                Log.d("RoleDebug", "LOGIN SUCCESS — role='$role'")
                userRole = role
                userName = name
                userStudentId = studentId
                currentScreen = "dashboard"
            }
        )

        "dashboard" -> {
            val normalizedRole = userRole.trim().lowercase()
            Log.d("RoleDebug", "ROUTING — raw role='$userRole', normalized='$normalizedRole'")

            if (normalizedRole == "admin") {
                Log.d("RoleDebug", "-> GOING TO ADMIN DASHBOARD")
                AdminDashboard(
                    name = userName,
                    onLogout = {
                        FirebaseAuth.getInstance().signOut()
                        currentScreen = "login"
                    },
                    onRequestClick = { requestId ->
                        selectedRequestId = requestId
                        currentScreen = "request_details"
                    }
                )
            } else {
                Log.d("RoleDebug", "-> GOING TO STUDENT DASHBOARD")
                StudentDashboard(
                    name = userName,
                    onLogout = {
                        FirebaseAuth.getInstance().signOut()
                        currentScreen = "login"
                    },
                    onRequestDocument = { currentScreen = "request_document" },
                    onMyRequests = { currentScreen = "my_requests" },
                    onNotifications = { currentScreen = "notifications" },
                    onRequirements = { currentScreen = "requirements" }
                )
            }
        }

        "request_document" -> {
            RequestDocumentScreen(
                userName = userName,
                userId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                studentId = userStudentId,
                onBack = { currentScreen = "dashboard" },
                onSubmitted = { currentScreen = "dashboard" }
            )
        }

        "my_requests" -> {
            MyRequestsScreen(onBack = { currentScreen = "dashboard" })
        }

        "notifications" -> {
            NotificationsScreen(onBack = { currentScreen = "dashboard" })
        }

        "requirements" -> {
            RequirementsScreen(onBack = { currentScreen = "dashboard" })
        }

        "request_details" -> {
            RequestDetailsScreen(
                requestId = selectedRequestId,
                onBack = { currentScreen = "dashboard" }
            )
        }

        else -> {
            PlaceholderScreen(title = "Unknown", onBack = { currentScreen = "dashboard" })
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

fun fetchUserData(userId: String, onResult: (String, String, String) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    // CHANGED TO "users"
    db.collection("users").document(userId).get()
        .addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val role = document.getString("role") ?: "student"
                val name = document.getString("fullName") ?: "User"
                val studentId = document.getString("studentId") ?: "N/A"

                Log.d("RoleDebug", "Firestore fetched -> role: '$role', name: '$name', id: '$studentId'")
                onResult(role, name, studentId)
            } else {
                Log.d("RoleDebug", "Firestore document NOT FOUND for UID: $userId")
                onResult("student", "User", "N/A")
            }
        }
        .addOnFailureListener { e ->
            Log.e("RoleDebug", "Firestore error: ${e.message}")
            onResult("student", "User", "N/A")
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderScreen(title: String, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text(text = "$title — Coming Soon", style = MaterialTheme.typography.titleMedium)
        }
    }
}