package com.example.plmundocumentrequest

data class User(
    val studentId: String = "",
    val fullName: String = "",
    val email: String = "",
    val role: String = "student" // "student" or "admin"
)