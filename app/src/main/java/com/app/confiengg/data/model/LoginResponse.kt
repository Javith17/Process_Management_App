package com.app.confiengg.data.model

data class LoginResponse(
    val user: UserData?,
    val accessToken: String?
) {
    val success: Boolean get() = user != null && accessToken != null
    val message: String? get() = if (user == null) "Invalid credentials" else null
    val role: String? get() = user?.roleName ?: "SALES"
}

data class UserData(
    val userId: String,
    val empName: String,
    val empCode: String,
    val roleCode: String,
    val roleName: String,
    val roleId: String,
    val screens: List<ScreenPermission>
)

data class ScreenPermission(
    val name: String,
    val type: String,
    val screen: String,
    val permission: List<String>
)

data class LoginRequest(
    val emp_code: String,
    val password: String
)
