package com.app.confiengg.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Home : Screen("home_root")
    object AttendanceList : Screen("attendance_list")
    object Leads : Screen("leads")
    object FollowUps : Screen("follow_ups")
    object Quotations : Screen("quotations")
    object LeaveRequest : Screen("leave_request")
}

sealed class BottomBarScreen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : BottomBarScreen(
        route = "home",
        title = "Home",
        icon = Icons.Default.Home
    )

    object Attendance : BottomBarScreen(
        route = "attendance",
        title = "Attendance",
        icon = Icons.Default.CalendarMonth
    )

    object Profile : BottomBarScreen(
        route = "profile",
        title = "Profile",
        icon = Icons.Default.Person
    )
}
