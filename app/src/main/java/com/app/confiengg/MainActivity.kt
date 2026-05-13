package com.app.confiengg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.app.confiengg.data.pref.SessionManager
import com.app.confiengg.navigation.Screen
import com.app.confiengg.ui.screens.AttendanceHistoryScreen
import com.app.confiengg.ui.screens.HomeScreen
import com.app.confiengg.ui.screens.LeaveRequestScreen
import com.app.confiengg.ui.screens.LoginScreen
import com.app.confiengg.ui.screens.SplashScreen
import com.app.confiengg.ui.theme.ConfiEnggTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ConfiEnggTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val navController = rememberNavController()
    var userRole by remember { mutableStateOf(sessionManager.getUserRole() ?: "") }

    val startDestination = if (sessionManager.isLoggedIn()) Screen.Home.route else Screen.Splash.route

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Splash.route) {
            SplashScreen(onTimeout = {
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Login.route) {
            LoginScreen(onLoginSuccess = { role ->
                userRole = role
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Home.route) {
            HomeScreen(
                userRole = userRole,
                onNavigateToAttendanceHistory = {
                    navController.navigate(Screen.AttendanceList.route)
                },
                onNavigateToLeaveRequest = {
                    navController.navigate(Screen.LeaveRequest.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.AttendanceList.route) {
            AttendanceHistoryScreen(onBack = {
                navController.popBackStack()
            })
        }
        composable(Screen.LeaveRequest.route) {
            LeaveRequestScreen(onBack = {
                navController.popBackStack()
            })
        }
    }
}
