package com.sumi.ai.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.sumi.ai.core.brain.SumiBrain
import com.sumi.ai.core.voice.SumiVoiceEngine
import com.sumi.ai.service.voice.SumiForegroundService

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
                var isBackgroundServiceActive by remember { mutableStateOf(false) }
                var activeDialogTitle by remember { mutableStateOf<String?>(null) }

                // Photo loader (Check karega agar sumi_avatar photo upload hui hai)
                val avatarResId = remember {
                    val id = context.resources.getIdentifier("sumi_avatar", "drawable", context.packageName)
                    if (id != 0) id else null
                }

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

                // Mic Permission
                val permissionsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { perms ->
                    val micGranted = perms[Manifest.permission.RECORD_AUDIO] ?: false
                    if (micGranted) {
                        statusText = "Listening..."
                        voiceEngine?.startListening()
                    } else {
                        Toast.makeText(context, "Mic permission allow kijiye!", Toast.LENGTH_SHORT).show()
                    }
                }

                fun checkAndStartListening() {
                    val mic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                    if (mic) {
                        statusText = "Listening..."
                        voiceEngine?.startListening()
                    } else {
                        val needed = mutableListOf(Manifest.permission.RECORD_AUDIO)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            needed.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        permissionsLauncher.launch(needed.toTypedArray())
                    }
                }

                fun toggleBackgroundService() {
                    val intent = Intent(context, SumiForegroundService::class.java)
                    if (!isBackgroundServiceActive) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                        isBackgroundServiceActive = true
                        sumiReplyText = "Background Listen ON ho gaya! Ab 'Suno Sumi' bolein ✨"
                        voiceEngine?.speak("Background service on ho gayi hai!")
                    } else {
                        context.stopService(intent)
                        isBackgroundServiceActive = false
                        sumiReplyText = "Background service band ho gayi."
                    }
                }

                SumiDashboardScreen(
                    statusText = statusText,
                    userSpokenText = userSpokenText,
                    sumiReplyText = sumiReplyText,
                    avatarResId = avatarResId,
                    isBackgroundActive = isBackgroundServiceActive,
                    onToggleBackground = { toggleBackgroundService() },
                    onAvatarClick = { checkAndStartListening() },
                    onActionClick = { title ->
                        when (title) {
                            "Talk to Sumi" -> checkAndStartListening()
                            "Permissions" -> {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            }
                            else -> activeDialogTitle = title
                        }
                    },
                    onEmergencyStop = {
                        voiceEngine?.stopListening()
                        val serviceIntent = Intent(context, SumiForegroundService::class.java)
                        context.stopService(serviceIntent)
                        isBackgroundServiceActive = false
                        statusText = "Stopped"
                        sumiReplyText = "⛔ Sumi sab band karke ruk gayi hai!"
                        Toast.makeText(context, "Emergency Kill Switch Activated!", Toast.LENGTH_LONG).show()
                    }
                )

                // Dialogs
                activeDialogTitle?.let { title ->
                    AlertDialog(
                        onDismissRequest = { activeDialogTitle = null },
                        title = { Text(text = "🌸 $title", fontWeight = FontWeight.Bold) },
                        text = {
                            Text(
                                text = when (title) {
                                    "Phone Control" -> "Aap bol sakte hain:\n• 'इंस्टाग्राम खोलो'\n• 'यूट्यूब खोलो'\n• 'व्हाट्सएप खोलो'\n• 'कैमरा खोलो'\n• 'फोन सेटिंग खोलो'\nSumi turant app open karegi!"
                                    "Settings" -> "🌸 Sumi Voice Settings:\n• Pitch: 1.25x (Cute anime)\n• Speed: 0.95x\n• Preferred Language: Hindi / Hinglish"
                                    "Memory" -> "Local Memory System: ON\nSumi aapki baatein safe phone storage me yaad rakhti hai."
                                    "Privacy" -> "100% Private\nSumi koi bhi voice audio kisi server par upload nahi karti."
                                    else -> "Feature active hai!"
                                }
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { activeDialogTitle = null }) {
                                Text("Theek hai")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SumiDashboardScreen(
    statusText: String,
    userSpokenText: String,
    sumiReplyText: String,
    avatarResId: Int?,
    isBackgroundActive: Boolean,
    onToggleBackground: () -> Unit,
    onAvatarClick: () -> Unit,
    onActionClick: (String) -> Unit,
    onEmergencyStop: () -> Unit
) {
    val isPulsing = statusText.contains("Listening") || statusText.contains("Speaking")
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPulsing) 1.12f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Surface(modifier = Modifier.fillMaxSize(), color = SumiPinkBackground) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // 1. Anime Girl Avatar Circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(135.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(listOf(SumiPinkSecondary, SumiPinkPrimary))
                    )
                    .border(4.dp, Color.White, CircleShape)
                    .clickable { onAvatarClick() }
            ) {
                if (avatarResId != null) {
                    Image(
                        painter = painterResource(id = avatarResId),
                        contentDescription = "Sumi Anime Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "👧🌸", fontSize = 42.sp)
                        Text(
                            text = if (isPulsing) "Listening..." else "SUMI",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 2. All-Time Background Wake-Word Switch Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isBackgroundActive) Color(0xFFE8F5E9) else Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (isBackgroundActive) "🟢 All-Time Background Listen: ON" else "⚪ Background Listen: OFF",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isBackgroundActive) Color(0xFF2E7D32) else SumiTextDark
                        )
                        Text(
                            text = "Bolein: 'Suno Sumi' ya 'Sumi'",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = isBackgroundActive,
                        onCheckedChange = { onToggleBackground() }
                    )
                }
            }

            // 3. Conversation Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (userSpokenText.isNotEmpty()) {
                        Text(
                            text = "👤 Aap: $userSpokenText",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    Text(
                        text = "🌸 Sumi: $sumiReplyText",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SumiTextDark
                    )
                }
            }

            // 4. Feature Cards Grid
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
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(menuItems) { item ->
                    DashboardCard(item = item, onClick = { onActionClick(item.title) })
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 5. Emergency STOP
            Button(
                onClick = onEmergencyStop,
                colors = ButtonDefaults.buttonColors(containerColor = SumiEmergencyRed),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "STOP SUMI", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
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
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth().height(72.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(item.tint.copy(alpha = 0.15f))
            ) {
                Icon(item.icon, contentDescription = null, tint = item.tint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = item.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SumiTextDark
            )
        }
    }
}
