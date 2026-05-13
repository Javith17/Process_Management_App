package com.app.confiengg.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.app.confiengg.data.pref.SessionManager
import com.app.confiengg.ui.theme.ConfiEnggTheme

class AttachmentsActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val links = intent.getStringArrayListExtra("links") ?: emptyList<String>()
        val sessionManager = SessionManager(this)
        val token = sessionManager.fetchAuthToken()

        setContent {
            ConfiEnggTheme {
                var selectedImageUrl by remember { mutableStateOf<String?>(null) }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Attachments") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                ) { padding ->
                    AttachmentsScreen(
                        modifier = Modifier.padding(padding),
                        links = links,
                        authToken = token,
                        onImageClick = { selectedImageUrl = it },
                        onYoutubeClick = { url ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            startActivity(intent)
                        }
                    )

                    selectedImageUrl?.let { url ->
                        FullScreenImageDialog(url, token) { selectedImageUrl = null }
                    }
                }
            }
        }
    }
}

@Composable
fun AttachmentsScreen(
    modifier: Modifier = Modifier,
    links: List<String>,
    authToken: String?,
    onImageClick: (String) -> Unit,
    onYoutubeClick: (String) -> Unit
) {
    val youtubeLinks = links.filter { it.contains("youtube.com") || it.contains("youtu.be") }
    val instagramLinks = links.filter { it.contains("instagram.com") }
    val otherVideoLinks = links.filter { url ->
        (url.endsWith(".mp4") || url.endsWith(".mkv") || url.endsWith(".mov")) && 
        !youtubeLinks.contains(url) && !instagramLinks.contains(url)
    }
    
    val videoLinks = youtubeLinks + instagramLinks + otherVideoLinks
    
    val imageLinks = links.filter { url ->
        !youtubeLinks.contains(url) && !instagramLinks.contains(url) && !otherVideoLinks.contains(url) &&
        (url.contains("http") || url.contains("https")) &&
        (url.endsWith(".jpg") || url.endsWith(".jpeg") || url.endsWith(".png") || url.endsWith(".webp") || !url.contains("."))
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        if (imageLinks.isNotEmpty()) {
            item {
                Text("Images", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(imageLinks) { url ->
                        Card(
                            modifier = Modifier.size(120.dp).clickable { onImageClick(url) },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(url)
                                    .crossfade(true)
                                    .apply {
                                        authToken?.let { addHeader("Authorization", "Bearer $it") }
                                    }
                                    .addHeader("User-Agent", "Mozilla/5.0")
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }

        if (videoLinks.isNotEmpty()) {
            item {
                Text("Videos & Reels", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(videoLinks) { url ->
                val isYouTube = url.contains("youtube.com") || url.contains("youtu.be")
                val isInstagram = url.contains("instagram.com")
                
                val videoId = if (isYouTube) extractVideoId(url) else null
                val thumbnailUrl = if (isYouTube && videoId?.isNotEmpty() == true) "https://img.youtube.com/vi/$videoId/0.jpg" else null
                
                Card(
                    modifier = Modifier.fillMaxWidth().height(200.dp).clickable { onYoutubeClick(url) },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (thumbnailUrl != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(thumbnailUrl)
                                    .crossfade(true)
                                    .addHeader("User-Agent", "Mozilla/5.0")
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize().background(Color.DarkGray),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = if (isInstagram) Icons.Default.VideoLibrary else Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = Color.White.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        text = if (isInstagram) "Instagram Reel/Video" else "Play Video",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                        
                        if (thumbnailUrl != null) {
                            Surface(
                                modifier = Modifier.align(Alignment.Center),
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = androidx.compose.foundation.shape.CircleShape
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Play",
                                    modifier = Modifier.size(48.dp).padding(8.dp),
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenImageDialog(url: String, authToken: String?, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(url)
                        .crossfade(true)
                        .apply {
                            authToken?.let { addHeader("Authorization", "Bearer $it") }
                        }
                        .addHeader("User-Agent", "Mozilla/5.0")
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }
}

fun extractVideoId(url: String): String {
    return when {
        url.contains("v=") -> url.substringAfter("v=").substringBefore("&")
        url.contains("shorts/") -> url.substringAfter("shorts/").substringBefore("?")
        url.contains("youtu.be/") -> url.substringAfter("youtu.be/").substringBefore("?")
        else -> ""
    }
}
