package com.simats.goiretrieval.api

import com.google.gson.annotations.SerializedName

data class SignupRequest(
    val name: String,
    val email: String,
    val password: String,
    @SerializedName("employee_id") val employeeId: String? = null,
    @SerializedName("role") val role: String? = "Analyst"
)

data class SigninRequest(
    val email: String,
    val password: String,
    @SerializedName("device_name") val deviceName: String? = null,
    @SerializedName("os_version") val osVersion: String? = null
)

data class Update2FARequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("is_enabled") val isEnabled: Boolean
)

data class Verify2FARequest(
    @SerializedName("email") val email: String,
    @SerializedName("otp") val otp: String
)

data class ForgotRequest(
    val email: String
)

data class ResetRequest(
    val email: String,
    val new_password: String
)

data class VerifyOtpRequest(
    val email: String,
    val otp: String
)

data class User(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("employee_id") val employeeId: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("is_2fa_enabled") val is2faEnabled: Boolean? = false
)

data class SignupResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("user") val user: User? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("is_2fa_enabled") val is2faEnabled: Boolean? = false
)

// Document Models
data class ApiDocument(
    @SerializedName("id") val id: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("date") val date: String? = null,
    @SerializedName("format") val format: String? = null,
    @SerializedName("source") val source: String? = null,
    @SerializedName("filename") val filename: String? = null,
    @SerializedName("folder") val folder: String? = null,
    @SerializedName("department") val department: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("eligibility") val eligibility: String? = null,
    @SerializedName("benefits") val benefits: String? = null,
    @SerializedName("excerpt") val excerpt: String? = null,
    @SerializedName("preview") val preview: String? = null,
    @SerializedName("content") val content: String? = null
) : java.io.Serializable

data class RecentActivityResponse(
    @SerializedName("status") val status: String,
    @SerializedName("activities") val activities: List<ApiActivity>? = null
)

data class NotificationsResponse(
    @SerializedName("status") val status: String,
    @SerializedName("notifications") val notifications: List<ApiNotification>? = null
)

data class DocumentsResponse(
    @SerializedName("status") val status: String,
    @SerializedName("documents") val documents: List<ApiDocument>? = null
)

data class SavedDocRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("doc_id") val docId: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("date") val date: String? = null,
    @SerializedName("format") val format: String? = null,
    @SerializedName("source") val source: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("content") val content: String? = null
)

data class ContentResponse(
    @SerializedName("status") val status: String,
    @SerializedName("content") val content: String? = null,
    @SerializedName("message") val message: String? = null
)

data class ActivityLogRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("activity_type") val type: String,
    @SerializedName("detail") val detail: String,
    @SerializedName("doc_id") val docId: String? = null
)

data class MarkReadRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("notification_id") val notificationId: Int? = null
)

data class UpdateProfileRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("name") val name: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("employee_id") val employeeId: String? = null,
    @SerializedName("role") val role: String? = null
)

data class ApiActivity(
    @SerializedName("id") val id: Int,
    @SerializedName("type") val type: String,
    @SerializedName("detail") val detail: String,
    @SerializedName("time") val time: String,
    @SerializedName("date_label") val date_label: String,
    @SerializedName("doc_id") val doc_id: String?
)

data class ApiNotification(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("message") val message: String,
    @SerializedName("type") val type: String,
    @SerializedName("is_read") var is_read: Boolean,
    @SerializedName("time_ago") val time_ago: String
)

data class AuditDocument(
    @SerializedName("id") val id: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("compliance_score") val compliance_score: Int? = null,
    @SerializedName("summary") val summary: String? = null,
    @SerializedName("original_content") val original_content: String? = null,
    @SerializedName("ai_analysis") val ai_analysis: String? = null,
    @SerializedName("date") val date: String? = null,
    @SerializedName("format") val format: String? = null,
    @SerializedName("download_url") val download_url: String? = null
) : java.io.Serializable

data class AuditResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String? = null,
    @SerializedName("system_audit_summary") val system_audit_summary: String? = null,
    @SerializedName("documents") val documents: List<AuditDocument>? = null
)

data class AuditRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("source_url") val sourceUrl: String,
    @SerializedName("questions") val questions: String? = null
)

data class BriefingExportRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("audit_data") val auditData: List<AuditDocument>,
    @SerializedName("questions") val questions: String? = null,
    @SerializedName("report_id") val reportId: String? = null
)

data class ChatBotRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("message") val message: String
)

data class ChatBotResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String
)

data class GlobalStatsResponse(
    @SerializedName("status") val status: String,
    @SerializedName("total_users") val totalUsers: Int,
    @SerializedName("total_ai_queries") val totalAiQueries: Int,
    @SerializedName("total_documents") val totalDocuments: Int
)

data class DeviceItem(
    @SerializedName("id") val id: Int,
    @SerializedName("device_name") val deviceName: String,
    @SerializedName("os_version") val osVersion: String?,
    @SerializedName("last_login") val lastLogin: String?,
    @SerializedName("is_active") val isActive: Boolean
)

data class DevicesResponse(
    @SerializedName("status") val status: String,
    @SerializedName("devices") val devices: List<DeviceItem>? = null
)

data class NotificationCreateRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("title") val title: String,
    @SerializedName("message") val message: String,
    @SerializedName("type") val type: String = "info"
)
