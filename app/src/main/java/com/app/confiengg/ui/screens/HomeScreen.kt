package com.app.confiengg.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.app.confiengg.navigation.BottomBarScreen
import com.app.confiengg.navigation.Screen
import com.app.confiengg.data.api.RetrofitClient
import com.app.confiengg.data.model.UpdateTokenRequest
import com.app.confiengg.data.pref.SessionManager
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userRole: String,
    onNavigateToAttendanceHistory: () -> Unit,
    onNavigateToLeaveRequest: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val navController = rememberNavController()
    
    LaunchedEffect(Unit) {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            val userId = sessionManager.getUserId()
            if (userId != null) {
                val apiService = RetrofitClient.getClient(context)
                apiService.updateNotificationToken(UpdateTokenRequest(userId, token))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    val screens = listOf(
        BottomBarScreen.Home,
        BottomBarScreen.Attendance,
        BottomBarScreen.Profile
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = navBackStackEntry?.destination?.route

    // Determine title and whether to show back button
    val (title, showBack) = when (currentRoute) {
        BottomBarScreen.Home.route -> "Home" to false
        BottomBarScreen.Attendance.route -> "Attendance" to false
        BottomBarScreen.Profile.route -> "Profile" to false
        Screen.Leads.route -> "Leads" to true
        Screen.FollowUps.route -> "Follow Ups" to true
        Screen.Quotations.route -> "Quotations" to true
        else -> "ConfiEngg" to (navController.previousBackStackEntry != null)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = title) },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar {
                screens.forEach { screen ->
                    NavigationBarItem(
                        label = { Text(text = screen.title) },
                        icon = { Icon(imageVector = screen.icon, contentDescription = screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomBarScreen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomBarScreen.Home.route) { 
                DashboardScreen(
                    onNavigateToLeads = { navController.navigate(Screen.Leads.route) },
                    onNavigateToFollowUps = { navController.navigate(Screen.FollowUps.route) },
                    onNavigateToQuotations = { navController.navigate(Screen.Quotations.route) }
                )
            }
            composable(BottomBarScreen.Attendance.route) { 
                AttendanceScreen(onNavigateToHistory = onNavigateToAttendanceHistory) 
            }
            composable(BottomBarScreen.Profile.route) { 
                ProfileScreen(
                    onLogout = onLogout,
                    onLeaveRequest = onNavigateToLeaveRequest
                ) 
            }
            
            composable(Screen.Leads.route) { LeadsScreen() }
            composable(Screen.FollowUps.route) { FollowUpsScreen() }
            composable(Screen.Quotations.route) { QuotationScreen() }
        }
    }
}

@Composable
fun DashboardScreen(
    onNavigateToLeads: () -> Unit,
    onNavigateToFollowUps: () -> Unit,
    onNavigateToQuotations: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    var enquiryCount by remember { mutableStateOf("0") }
    var followUpCount by remember { mutableStateOf("0") }
    var quotationCount by remember { mutableStateOf("0") }

    LaunchedEffect(Unit) {
        val userId = sessionManager.getUserId()
        if (userId != null) {
            try {
                val apiService = RetrofitClient.getClient(context)
                val response = apiService.getMarketingDashboard(userId)
                if (response.isSuccessful) {
                    response.body()?.let {
                        enquiryCount = it.enquiryCount ?: "0"
                        followUpCount = it.followUpCount ?: "0"
                        quotationCount = it.quotationCount ?: "0"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val dashboardItems = listOf(
        DashboardItem("Leads", Icons.AutoMirrored.Filled.List, enquiryCount, onNavigateToLeads),
        DashboardItem("Follow ups", Icons.Default.ThumbUp, followUpCount, onNavigateToFollowUps),
        DashboardItem("Quotations", Icons.Default.Description, quotationCount, onNavigateToQuotations)
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(dashboardItems) { item ->
                DashboardCard(item)
            }
        }
    }
}

data class DashboardItem(val title: String, val icon: ImageVector, val badgeCount: String, val onClick: () -> Unit)

@Composable
fun DashboardCard(item: DashboardItem) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(8.dp) // Add padding to allow the badge to overflow
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .clickable { item.onClick() },
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color.Red)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (item.badgeCount != "0") {
            Surface(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 8.dp, y = (-8).dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = item.badgeCount,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun PlaceholderScreen(name: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "$name Screen", style = MaterialTheme.typography.headlineMedium)
    }
}
