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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.sumi.ai.core.brain.SumiBrain
import com.sumi.ai.core.brain.SumiMood
import com.sumi.ai.core.voice.SumiVoiceEngine
import com.sumi.ai.service.voice.SumiForegroundService

val SumiPinkPrimary = Color(0xFFFF4081)
val SumiPinkBackground = Color(0xFFFFF0F5)
val SumiCardSurface = Color(0xFFFFFFFF)
val SumiTextDark = Color(0xFF2E1A22)
val SumiGreenReady = Color(0xFF00C853)
val SumiEmergencyRed = Color(0xFFD50000)

data class FeatureItem(
    val itemName: String,
    val itemIcon: ImageVector,
    val itemColor: Color
)

class MainActivity : ComponentActivity() {

    private var voiceEngine: SumiVoiceEngine? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val context = LocalContext.current
                var statusText by remember { mutableStateOf("Ready") }
                var userSpokenText by remember { mutableStateOf("") }
                var sumiReplyText by remember { mutableStateOf("Konnichiwa! Main Sumi hoon, aapki cute anime assistant~ Boliye babu ji! 🌸") }
                var currentMood by remember { mutableStateOf(SumiMood.HAPPY) }
                var isBackgroundActive by remember { mutableStateOf(false) }
                var activeDialogTitle by remember { mutableStateOf<String?>(null) }

                DisposableEffect(Unit) {
                    voiceEngine = SumiVoiceEngine(
                        context = context,
                        onSpeechRecognized = { spokenQuery ->
                            userSpokenText = spokenQuery
                            val response = SumiBrain.processQuery(context, spokenQuery)
                            sumiReplyText = response.replyText
                            currentMood = response.mood
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

                val micLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { perms ->
                    val granted = perms[Manifest.permission.RECORD_AUDIO] ?: false
                    if (granted) {
                        voiceEngine?.startListening()
                    } else {
                        Toast.makeText(context, "Microphone permission allow kijiye!", Toast.LENGTH_SHORT).show()
                    }
                }

                fun triggerListening() {
                    val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                    if (hasMic) {
                        voiceEngine?.startListening()
                    } else {
                        val list = mutableListOf(Manifest.permission.RECORD_AUDIO)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            list.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        micLauncher.launch(list.toTypedArray())
                    }
                }

                fun toggleBackground() {
                    val intent = Intent(context, SumiForegroundService::class.java)
                    if (!isBackgroundActive) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                        isBackgroundActive = true
                        sumiReplyText = "Hehe~ Background mode ON ho gaya! Main hamesha sun rahi hoon 🌸"
                        currentMood = SumiMood.HAPPY
                        voiceEngine?.speak("Haan ji! Main background mein ready hoon!")
                    } else {
                        context.stopService(intent)
                        isBackgroundActive = false
                        sumiReplyText = "Background service band ho gayi hai."
                    }
                }

                SumiDashboardScreen(
                    statusText = statusText,
                    userSpokenText = userSpokenText,
                    sumiReplyText = sumiReplyText,
                    currentMood = currentMood,
                    isBackgroundActive = isBackgroundActive,
                    onAvatarClick = { triggerListening() },
                    onToggleBackground = { toggleBackground() },
                    onActionClick = { title ->
                        when (title) {
                            "Talk to Sumi" -> triggerListening()
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
                        context.stopService(Intent(context, SumiForegroundService::class.java))
                        isBackgroundActive = false
                        statusText = "Stopped"
                        currentMood = SumiMood.TSUNDERE_ANNOYED
                        sumiReplyText = "⛔ Sumi sab band karke ruk gayi hai!"
                        Toast.makeText(context, "STOP SUMI Activated!", Toast.LENGTH_SHORT).show()
                    }
                )

                activeDialogTitle?.let { clickedTitle ->
                    AlertDialog(
                        onDismissRequest = { activeDialogTitle = null },
                        title = { Text(text = "🌸 $clickedTitle", fontWeight = FontWeight.Bold) },
                        text = {
                            Text(
                                text = when (clickedTitle) {
                                    "Phone Control" -> "Aap bol sakte hain:\n• 'इंस्टाग्राम खोलो'\n• 'यूट्यूब खोलो'\n• 'व्हाट्सएप खोलो'\n• 'कैमरा खोलो'\n• 'फोन सेटिंग खोलो'"
                                    "Settings" -> "🌸 Voice: Alya-style Cute Anime Girl\n• Pitch: 1.72x\n• Speed: 1.10x\n• Personality: Tsundere & Cute"
                                    "Memory" -> "Local memory storage active hai."
                                    "Privacy" -> "100% On-device privacy protection."
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
    currentMood: SumiMood,
    isBackgroundActive: Boolean,
    onAvatarClick: () -> Unit,
    onToggleBackground: () -> Unit,
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

            // Anime Avatar with Moods
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(150.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(4.dp, SumiPinkPrimary, CircleShape)
                    .clickable { onAvatarClick() }
            ) {
                AnimeCharacterGraphic(
                    mood = currentMood,
                    isSpeaking = statusText.contains("Speaking")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Status Badge
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 2.dp,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
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
                        fontSize = 12.sp,
                        color = SumiTextDark
                    )
                }
            }

            // Background Switch
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isBackgroundActive) Color(0xFFE8F5E9) else Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (isBackgroundActive) "🟢 Sumi Background: ON" else "⚪ Sumi Background: OFF",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isBackgroundActive) Color(0xFF2E7D32) else SumiTextDark
                        )
                        Text(
                            text = "Background mein active rahegi",
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

            // Speech Bubble
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
                        text = "🌸 Sumi (Alya voice): $sumiReplyText",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SumiTextDark
                    )
                }
            }

            // Feature Grid Items
            val featureList = listOf(
                FeatureItem("Talk to Sumi", Icons.Default.Call, SumiPinkPrimary),
                FeatureItem("Settings", Icons.Default.Settings, Color(0xFF673AB7)),
                FeatureItem("Memory", Icons.Default.Favorite, Color(0xFF009688)),
                FeatureItem("Phone Control", Icons.Default.PhoneAndroid, Color(0xFFE91E63)),
                FeatureItem("Air Gestures", Icons.Default.PlayArrow, Color(0xFFFF9800)),
                FeatureItem("Permissions", Icons.Default.Lock, Color(0xFF3F51B5)),
                FeatureItem("Privacy", Icons.Default.Info, Color(0xFF4CAF50)),
                FeatureItem("Routines", Icons.Default.Notifications, Color(0xFF00BCD4))
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(featureList) { feat ->
                    Card(
                        onClick = { onActionClick(feat.itemName) },
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
                                    .background(feat.itemColor.copy(alpha = 0.15f))
                            ) {
                                Icon(feat.itemIcon, contentDescription = null, tint = feat.itemColor, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = feat.itemName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SumiTextDark
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Stop Button
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
