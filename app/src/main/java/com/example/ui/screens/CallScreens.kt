package com.example.ui.screens

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.data.Contact
import com.example.data.RecentCall
import com.example.ui.viewmodel.CallState
import com.example.ui.viewmodel.CallViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Structure for preset high-quality calling videos
data class PresetVideo(val name: String, val url: String, val category: String)

val PRESET_VIDEOS = listOf(
    PresetVideo("Abstract Tech Loop", "https://assets.mixkit.co/videos/preview/mixkit-abstract-digital-technology-background-loop-41852-large.mp4", "Digital Workspace"),
    PresetVideo("Neon Light Rhythm", "https://assets.mixkit.co/videos/preview/mixkit-neon-light-strip-flashing-and-looping-41223-large.mp4", "Vibrant Cyber"),
    PresetVideo("Circuit Board Glow", "https://assets.mixkit.co/videos/preview/mixkit-glowing-lines-on-a-circuit-board-background-41484-large.mp4", "Glow Waves"),
    PresetVideo("Sunlit Stream", "https://assets.mixkit.co/videos/preview/mixkit-forest-stream-in-the-sunlight-loop-42847-large.mp4", "Nature Zen"),
    PresetVideo("Ocean Sunset Core", "https://assets.mixkit.co/videos/preview/mixkit-waves-gently-crashing-on-beach-loop-41617-large.mp4", "Atmospheric")
)

/**
 * Android Jetpack Media3 ExoPlayer implementation to loop short video wallpapers
 * continuously for incoming calls. In the event of offline loading or errors,
 * are seamlessly bridged with a fallback glowing particle canvas.
 */
@OptIn(UnstableApi::class)
@Composable
fun FullscreenVideoPlayer(videoUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var hasError by remember { mutableStateOf(false) }

    // Initialize looping, muted ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            playWhenReady = true
            volume = 0f // Call-screen visual is muted; we let normal system/simulated ringtone handle audio
        }
    }

    // Prepare media items reactively
    LaunchedEffect(videoUrl) {
        hasError = false
        try {
            val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
        } catch (e: Exception) {
            hasError = true
        }
    }

    // Stop and release player safely when Composable exits scene
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Set listener to handle load errors gracefully without showing blank screens
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                hasError = true
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    Box(modifier = modifier) {
        if (!hasError) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Elegant background wave fallback for offline compatibility
            GlowingFallbackBackground()
        }
    }
}

/**
 * Beautiful canvas background fallback when a network-based video fails to resolve.
 * Renders pulse ripples in dark mode.
 */
@Composable
fun GlowingFallbackBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarWave")
    val pulseRadius1 by infiniteTransition.animateFloat(
        initialValue = 100f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Pulse1"
    )
    val pulseRadius2 by infiniteTransition.animateFloat(
        initialValue = 50f,
        targetValue = 450f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Pulse2"
    )
    val pulseOpacity1 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Opacity1"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF101221),
                        Color(0xFF1A1C2E),
                        Color(0xFF0C0D16)
                    )
                )
            )
            .drawBehind {
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    color = Color(0xFF5C6BC0),
                    radius = pulseRadius1,
                    center = center,
                    style = Stroke(width = 4f),
                    alpha = pulseOpacity1
                )
                drawCircle(
                    color = Color(0xFF00E676),
                    radius = pulseRadius2,
                    center = center,
                    style = Stroke(width = 2f),
                    alpha = (pulseOpacity1 * 0.7f).coerceIn(0f, 1f)
                )
            }
    )
}

/**
 * Display helper for circular initials avatar. Replaces it with Rashad's profile image if suitable.
 */
@Composable
fun ContactAvatar(
    name: String,
    avatarColor: Int,
    modifier: Modifier = Modifier,
    sizeDp: Int = 48
) {
    val isRashad = name.lowercase().contains("rashad")
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(Color(avatarColor)),
        contentAlignment = Alignment.Center
    ) {
        if (isRashad) {
            Image(
                painter = painterResource(id = R.drawable.rashad_profile),
                contentDescription = "Rashad Rashid Workspace Photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val initial = if (name.isNotEmpty()) name.first().uppercaseChar().toString() else "?"
            Text(
                text = initial,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (sizeDp * 0.38f).sp
            )
        }
    }
}

/**
 * Incoming Call loop presentation screen.
 * Shows looping video background, accepting and declining buttons.
 */
@Composable
fun IncomingCallScreen(
    state: CallState.Incoming,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val context = LocalContext.current
    
    // Play subtle system call-ring sound when the incoming screen mounts
    DisposableEffect(Unit) {
        val defaultRingtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val ringtone = RingtoneManager.getRingtone(context, defaultRingtoneUri)
        try {
            ringtone.play()
        } catch (e: Exception) {
            // Safe fallback
        }
        onDispose {
            try {
                if (ringtone.isPlaying) {
                    ringtone.stop()
                }
            } catch (e: Exception) {
                // Safe ignore
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Video loops in the background
        FullscreenVideoPlayer(
            videoUrl = state.videoUrl,
            modifier = Modifier.fillMaxSize()
        )

        // Semi-transparent overlay mask for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.2f),
                            Color.Black.copy(alpha = 0.75f)
                        )
                    )
                )
        )

        // Caller Info Block (Top Center)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 90.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ContactAvatar(
                name = state.contactName,
                avatarColor = 0xFF3F51B5.toInt(),
                sizeDp = 100,
                modifier = Modifier
                    .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                    .padding(4.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "INCOMING CALL",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = state.contactName,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = state.contactNumber,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
        }

        // Action controls (Bottom Center)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 80.dp, start = 32.dp, end = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Decline Button (Red Glow Circle)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onDecline,
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color(0xFFE53935), CircleShape)
                        .testTag("decline_call_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.CallEnd,
                        contentDescription = "Decline and reject call",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Decline", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            // Accept Button (Green Glow Circle)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onAccept,
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color(0xFF43A047), CircleShape)
                        .testTag("accept_call_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Call,
                        contentDescription = "Accept and answer call",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Accept", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * Ongoing Call dialog presentation. Keeps track of calling durations reactively.
 */
@Composable
fun OngoingCallScreen(
    state: CallState.Ongoing,
    durationSeconds: Int,
    onHangup: () -> Unit
) {
    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60
    val durationFormatted = String.format(Locale.US, "%02d:%02d", minutes, seconds)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF151820),
                        Color(0xFF1F2432),
                        Color(0xFF0F1116)
                    )
                )
            )
    ) {
        // Aesthetic dynamic waves centered
        GlowingFallbackBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 90.dp, bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Contact Header Info
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ContactAvatar(
                    name = state.contactName,
                    avatarColor = 0xFF42A5F5.toInt(),
                    sizeDp = 110,
                    modifier = Modifier.border(3.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), CircleShape)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = if (state.callType == "OUTGOING") "OUTGOING CALL" else "ONGOING CALL",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = state.contactName,
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = state.contactNumber,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Pulsing Call Duration Badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E676))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = durationFormatted,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Large Hangup Circle Control
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onHangup,
                    modifier = Modifier
                        .size(76.dp)
                        .background(Color(0xFFD32F2F), CircleShape)
                        .testTag("hangup_call_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.CallEnd,
                        contentDescription = "Hang up call",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "End Call",
                    color = Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

/**
 * Main Home Tab View. Contains history, dynamic quick trigger systems.
 */
@Composable
fun HomeScreen(
    viewModel: CallViewModel,
    modifier: Modifier = Modifier,
    onDialRequest: (String) -> Unit
) {
    val recents by viewModel.recentCalls.collectAsState()
    val contactsList by viewModel.contacts.collectAsState()
    var searchNumber by remember { mutableStateOf("") }
    var context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Welcome Header with custom Creator Card featuring Rashad Rashid's picture
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular picture frame referencing Rashad's profile image
                Image(
                    painter = painterResource(id = R.drawable.rashad_profile),
                    contentDescription = "Rashad Rashid Rashtech Solutions President",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "RashtechSolutions",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Rashad Rashid",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Lightweight, secure Calling Hub",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Simulated Dialer Pad Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Quick Dialer & Simulator",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = searchNumber,
                    onValueChange = { searchNumber = it },
                    placeholder = { Text("Enter calling number...") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialer_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Outgoing Call Option (Left)
                    Button(
                        onClick = {
                            if (searchNumber.trim().isNotEmpty()) {
                                // Locate matched contact name if exists
                                val matched = contactsList.find { it.phoneNumber.contains(searchNumber.trim()) }
                                val name = matched?.name ?: "Unknown Call"
                                viewModel.initiateOutgoingCall(name, searchNumber.trim(), matched?.id)
                                searchNumber = ""
                            } else {
                                Toast.makeText(context, "Please type a phone number!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp)
                            .testTag("quick_outgoing_button")
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Call", fontSize = 13.sp)
                    }

                    // Simulated Incoming loop (Right, showing off video screen!)
                    Button(
                        onClick = {
                            if (searchNumber.trim().isNotEmpty()) {
                                val matched = contactsList.find { it.phoneNumber.contains(searchNumber.trim()) }
                                val name = matched?.name ?: "Inbound Client"
                                viewModel.simulateIncomingCall(name, searchNumber.trim(), matched?.id, matched?.customVideoUrl)
                                searchNumber = ""
                            } else {
                                // Simulation with default Rashad Profile setup
                                viewModel.simulateIncomingCall(
                                    "Rashad Rashid",
                                    "+1 (555) 727-4832",
                                    contactsList.find { it.name.contains("Rashad") }?.id
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047)),
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 6.dp)
                            .testTag("sim_inbound_button")
                    ) {
                        Icon(Icons.Default.RingVolume, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Incoming", fontSize = 13.sp, maxLines = 1)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Recent Calls Header & Empty State Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Calls Log",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (recents.isNotEmpty()) {
                TextButton(
                    onClick = { viewModel.clearHistory() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear All")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (recents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PhoneMissed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your call history is empty",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            // Display list items cleanly inside Column for inner scrolls
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    recents.forEachIndexed { index, call ->
                        RecentCallItem(
                            call = call,
                            onDelete = { viewModel.deleteCallLog(call.id) },
                            onCallAgain = {
                                viewModel.initiateOutgoingCall(call.name, call.phoneNumber, call.contactId)
                            }
                        )
                        if (index < recents.size - 1) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Single Recent Call row display. Format timestamp nicely.
 */
@Composable
fun RecentCallItem(
    call: RecentCall,
    onDelete: () -> Unit,
    onCallAgain: () -> Unit
) {
    val dateString = remember(call.timestamp) {
        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        sdf.format(Date(call.timestamp))
    }

    val typeColor = when (call.callType) {
        "MISSED" -> Color(0xFFE53935)
        "OUTGOING" -> Color(0xFF00B0FF)
        else -> Color(0xFF43A047)
    }

    val typeIcon = when (call.callType) {
        "MISSED" -> Icons.Default.CallMissed
        "OUTGOING" -> Icons.Default.CallMade
        else -> Icons.Default.CallReceived
    }

    val minutes = call.durationSeconds / 60
    val seconds = call.durationSeconds % 60
    val durationText = if (call.durationSeconds > 0) {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    } else {
        if (call.callType == "MISSED") "Missed" else "Declined"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCallAgain() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(typeColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = typeIcon,
                contentDescription = call.callType,
                tint = typeColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = call.name,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = call.phoneNumber,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = durationText,
                    fontSize = 12.sp,
                    color = typeColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = dateString,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }

        Row {
            IconButton(onClick = onCallAgain) {
                Icon(
                    imageVector = Icons.Default.Call,
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = "Redial contact"
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Close,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    contentDescription = "Remove logs"
                )
            }
        }
    }
}

/**
 * Full contacts address list. Fits search bars, deletion dialogs, and customized video links.
 */
@Composable
fun ContactsScreen(
    viewModel: CallViewModel,
    modifier: Modifier = Modifier
) {
    val contactsList by viewModel.contacts.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedContactForEdit by remember { mutableStateOf<Contact?>(null) }

    val filteredContacts = remember(contactsList, searchQuery) {
        contactsList.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.phoneNumber.contains(searchQuery)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Search Input Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search contact name or index...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_contacts_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredContacts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ContactSupport,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) "No contacts saved yet" else "No matching contacts found",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 15.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredContacts) { contact ->
                        ContactRowItem(
                            contact = contact,
                            onInteract = { selectedContactForEdit = contact },
                            onCall = { viewModel.initiateOutgoingCall(contact.name, contact.phoneNumber, contact.id) },
                            onSimInbound = { viewModel.simulateIncomingCall(contact.name, contact.phoneNumber, contact.id, contact.customVideoUrl) }
                        )
                    }
                }
            }
        }

        // Add Contact FAB
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
                .testTag("add_contact_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add new contact record")
        }

        // Add Contact Interactive Overlay Screen
        if (showAddDialog) {
            ContactFormDialog(
                title = "Create New Contact",
                nameInitial = "",
                phoneInitial = "",
                emailInitial = "",
                videoUrlInitial = "",
                onDismiss = { showAddDialog = false },
                onSave = { name, phone, email, video ->
                    viewModel.addContact(name, phone, email, video)
                    showAddDialog = false
                }
            )
        }

        // Edit/Delete Contact Overlay Screen
        if (selectedContactForEdit != null) {
            val contactToEdit = selectedContactForEdit!!
            ContactFormDialog(
                title = "Edit Contact",
                nameInitial = contactToEdit.name,
                phoneInitial = contactToEdit.phoneNumber,
                emailInitial = contactToEdit.email,
                videoUrlInitial = contactToEdit.customVideoUrl ?: "",
                isEdit = true,
                onDismiss = { selectedContactForEdit = null },
                onSave = { name, phone, email, video ->
                    val updated = contactToEdit.copy(
                        name = name,
                        phoneNumber = phone,
                        email = email,
                        customVideoUrl = if (video.isEmpty()) null else video
                    )
                    viewModel.updateContact(updated)
                    selectedContactForEdit = null
                },
                onDelete = {
                    viewModel.deleteContact(contactToEdit)
                    selectedContactForEdit = null
                }
            )
        }
    }
}

/**
 * Elegant item card representation of a contact. Supports calling or simulation instantly.
 */
@Composable
fun ContactRowItem(
    contact: Contact,
    onInteract: () -> Unit,
    onCall: () -> Unit,
    onSimInbound: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onInteract() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContactAvatar(
                name = contact.name,
                avatarColor = contact.avatarColor,
                sizeDp = 48
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = contact.phoneNumber,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontFamily = FontFamily.Monospace
                )
                if (!contact.email.isNullOrEmpty()) {
                    Text(
                        text = contact.email,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                
                // Overlay label if there is a customized wallpaper
                if (!contact.customVideoUrl.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = Color(0xFF00E676).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                Icons.Default.VideoCall,
                                contentDescription = null,
                                tint = Color(0xFF00C853),
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Custom Video Active",
                                color = Color(0xFF00C853),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Simple swift dial actions
            Row(horizontalArrangement = Arrangement.End) {
                // Outgoing Call clicker
                IconButton(onClick = onCall) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = "Trigger Outgoing Simulator"
                    )
                }
                // Simulated Ring ring trigger
                IconButton(onClick = onSimInbound) {
                    Icon(
                        imageVector = Icons.Default.RingVolume,
                        tint = Color(0xFF43A047),
                        contentDescription = "Simulate Loop Incoming Ring"
                    )
                }
            }
        }
    }
}

/**
 * Universal Form Dialog for inserting or updating contacts.
 */
@Composable
fun ContactFormDialog(
    title: String,
    nameInitial: String,
    phoneInitial: String,
    emailInitial: String,
    videoUrlInitial: String,
    isEdit: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, email: String, video: String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(nameInitial) }
    var phone by remember { mutableStateOf(phoneInitial) }
    var email by remember { mutableStateOf(emailInitial) }
    var videoUrl by remember { mutableStateOf(videoUrlInitial) }
    var showUrlPresetsDropdown by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("contact_form_name")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("contact_form_phone")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (Optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Custom video selector or URL paste text-field
                OutlinedTextField(
                    value = videoUrl,
                    onValueChange = { videoUrl = it },
                    label = { Text("Contact Video Background URL") },
                    placeholder = { Text("Paste MP4 link or select preset") },
                    trailingIcon = {
                        IconButton(onClick = { showUrlPresetsDropdown = !showUrlPresetsDropdown }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Open video choices")
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("contact_form_video")
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    "You can enter any web MP4 file link to loop when this person calls, or select a pre-tested backdrop below.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    lineHeight = 13.sp
                )

                if (showUrlPresetsDropdown) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Column {
                            PRESET_VIDEOS.forEach { preset ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            videoUrl = preset.url
                                            showUrlPresetsDropdown = false
                                        }
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(preset.name, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(preset.category, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (isEdit && onDelete != null) {
                        TextButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete")
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (name.trim().isEmpty() || phone.trim().isEmpty()) {
                                return@Button
                            }
                            onSave(name.trim(), phone.trim(), email.trim(), videoUrl.trim())
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("contact_form_save_button")
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

/**
 * Settings Screen: Controls light/dark mode and global wallpaper.
 */
@Composable
fun SettingsScreen(
    viewModel: CallViewModel,
    modifier: Modifier = Modifier
) {
    val defaultVideo by viewModel.defaultVideoUrl.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    var inputCustomUrl by remember { mutableStateOf(defaultVideo) }
    var context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "App Settings",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Fine-tune calling aesthetics & theme preferences",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Card 1: Theme Chooser
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Dark Mode Theme", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            text = if (isDarkTheme) "Sleek dark screen visual layout active" else "High contrast light mode layout active",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = { viewModel.setThemeMode(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card 2: Custom Loop Video Picker
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.VideoSettings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Global Calling Video Wallpaper", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            "This looping backdrop shows when people call if no specific contact video is defined",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Custom URL Input
                OutlinedTextField(
                    value = inputCustomUrl,
                    onValueChange = { inputCustomUrl = it },
                    label = { Text("Custom Loop MP4 Web Url") },
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (inputCustomUrl.trim().isNotEmpty()) {
                            viewModel.setDefaultVideoUrl(inputCustomUrl.trim())
                            Toast.makeText(context, "Saved default calling video!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Apply Video")
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "Quick Choose Preset Backdrop Wallpaper",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.secondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                PRESET_VIDEOS.forEach { preset ->
                    val isSelected = defaultVideo == preset.url
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.16f) else Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setDefaultVideoUrl(preset.url)
                                inputCustomUrl = preset.url
                                Toast.makeText(context, "Selected preset: ${preset.name}", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(preset.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                Text(preset.category, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Currently selected calling visual",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
