package com.sumi.ai.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.sumi.ai.core.brain.SumiBrain
import com.sumi.ai.core.voice.SumiVoiceEngine

// Sumi Cute Anime Color Palette
val SumiPinkPrimary = Color(0xFFFF4081)
val SumiPinkSecondary = Color(0xFFFF80AB)
val SumiPinkBackground = Color(0xFFFFF0F5)
val SumiCardSurface = Color(0xFFFFFFFF)
val SumiTextDark = Color(0xFF2E1A22)
val SumiGreenReady = Color(0xFF00C853)
val SumiEmergencyRed = Color(0xFFD50000)

class MainActivity : ComponentActivity() {

    private var voiceEngine: SumiVoiceEngine? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val context = LocalContext.current
                var statusText by remember { mutableStateOf("Ready") }
                var userSpokenText by remember { mutableStateOf("") }
                var sumiReplyText by remember { mutableStateOf("Hi ji! Main Sumi hoon. Kahiye, main aapke liye kya kar sakti hoon? 🌸") }

                // Voice Engine Setup
                DisposableEffect(Unit) {
                    voiceEngine = SumiVoiceEngine(
                        context = context,
                        onSpeechRecognized = { spokenQuery ->
                            userSpokenText = spokenQuery
                            val response = SumiBrain.processQuery(context, spokenQuery)
                            sumiReplyText = response.replyText
                            voiceEngine?.speak(response.replyText)
                            SumiBrain.executeAction(context, response)
                        },
                        onStatusChanged = { newStatus ->
                            statusText = newStatus
                        }
                    )
                    onDispose {
                        voiceEngine?.shutdown()
                    }
                }

                // Microphone Permission Launcher
                val micPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        statusText = "Listening..."
                        voiceEngine?.startListening()
                    } else {
                        Toast.makeText(context, "Microphone permission zaroori hai Sumi se baat karne ke liye!", Toast.LENGTH_SHORT).show()
                    }
                }

                fun startListeningWithPermission() {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasPermission) {
                        statusText = "Listening..."
                        voiceEngine?.startListening()
                    } else {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }

                SumiDashboardScreen(
                    statusText = statusText,
                    userSpokenText = userSpokenText,
                    sumiReplyText = sumiReplyText,
                    onAvatarClick = { startListeningWithPermission() },
                    onActionClick = { title ->
                        when (title) {
                            "Talk to Sumi" -> startListeningWithPermission()
                            "Phone Control" -> {
                                val reply = "Ji, aap bol kar YouTube, Instagram, Camera ya Settings khol sakte hain!"
                                sumiReplyText = reply
                                voiceEngine?.speak(reply)
                            }
                            else -> {
                                Toast.makeText(context, "$title feature ready!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onEmergencyStop = {
                        voiceEngine?.stopListening()
                        statusText = "Stopped"
                        sumiReplyText = "Sumi stop ho gayi hai. Jab dubara baat karni ho toh Avatar par tap karein."
                        Toast.makeText(context, "⛔ SUMI STOPPED!", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }
}

@Composable
fun SumiDashboardScreen(
    statusText: String,
    userSpokenText: String,
    sumiReplyText: String,
    onAvatarClick: () -> Unit,
    onActionClick: (String) -> Unit,
    onEmergencyStop: () -> Unit
) {
    // Pulse animation when listening or speaking
    val isPulsing = statusText.contains("Listening") || statusText.contains("Speaking")
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPulsing) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SumiPinkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. Anime Avatar Circular Card with Interactive Tap & Animation
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(125.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(SumiPinkSecondary, SumiPinkPrimary)
                        )
                    )
                    .border(4.dp, Color.White, CircleShape)
                    .clickable { onAvatarClick() }
            ) {
                Text(
                    text = if (isPulsing) "🎙\nListening..." else "🌸\nSUMI",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Status Badge
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 2.dp,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    statusText.contains("Listening") -> Color(0xFFFF9800)
                                    statusText.contains("Speaking") -> SumiPinkPrimary
                                    statusText.contains("Stopped") -> SumiEmergencyRed
                                    else -> SumiGreenReady
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "● Status: $statusText",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = SumiTextDark
                    )
                }
            }

            // 3. Live Speech Bubble (Conversation Box)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    if (userSpokenText.isNotEmpty()) {
                        Text(
                            text = "👤 Aap: $userSpokenText",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(
                        text = "🌸 Sumi: $sumiReplyText",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SumiTextDark
                    )
                }
            }

            // 4. Feature Buttons Grid
            val menuItems = listOf(
                DashboardItem("Talk to Sumi", Icons.Default.Call, SumiPinkPrimary),
                DashboardItem("Settings", Icons.Default.Settings, Color(0xFF673AB7)),
                DashboardItem("Memory", Icons.Default.Favorite, Color(0xFF009688)),
                DashboardItem("Phone Control", Icons.Default.PhoneAndroid, Color(0xFFE91E63)),
                DashboardItem("Air Gestures", Icons.Default.PlayArrow, Color(0xFFFF9800)),
                DashboardItem("Permissions", Icons.Default.Lock, Color(0xFF3F51B5)),
                DashboardItem("Privacy", Icons.Default.Info, Color(0xFF4CAF50)),
                DashboardItem("Routines", Icons.Default.Notifications, Color(0xFF00BCD4))
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(menuItems) { item ->
                    DashboardCard(item = item, onClick = { onActionClick(item.title) })
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5. Emergency Kill Switch
            Button(
                onClick = onEmergencyStop,
                colors = ButtonDefaults.buttonColors(containerColor = SumiEmergencyRed),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "STOP SUMI",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }
        }
    }
}

data class DashboardItem(
    val title: String,
    val icon: ImageVector,
    val tint: Color
)

@Composable
fun DashboardCard(item: DashboardItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = SumiCardSurface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(item.tint.copy(alpha = 0.15f))
            ) {
                Icon(item.icon, contentDescription = null, tint = item.tint, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = item.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = SumiTextDark
            )
        }
    }
}
