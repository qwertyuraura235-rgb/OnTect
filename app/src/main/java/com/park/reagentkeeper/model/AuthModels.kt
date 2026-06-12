package com.park.reagentkeeper.model

enum class UserRole(val label: String) {
    ADMIN("관리자"),
    TEACHER("교사"),
    ASSISTANT("실험 보조"),
}

data class AppUser(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val labName: String,
    val active: Boolean,
    val lastLoginAt: Long,
)

data class LoginResult(
    val user: AppUser?,
    val errorMessage: String?,
)

data class RegistrationResult(
    val user: AppUser?,
    val errorMessage: String?,
)

data class UserStats(
    val totalUsers: Int,
    val activeUsers: Int,
    val adminUsers: Int,
    val teacherUsers: Int,
    val assistantUsers: Int,
)

data class AdminStats(
    val userStats: UserStats,
    val totalItems: Int,
    val lowStockItems: Int,
    val expiringItems: Int,
    val highHazardItems: Int,
    val totalEvents: Int,
    val stockInEvents: Int,
    val stockOutEvents: Int,
    val auditEvents: Int,
    val labSummaries: List<LabSummary>,
)

data class LabSummary(
    val labName: String,
    val ownerCount: Int,
    val itemCount: Int,
    val alertCount: Int,
)
