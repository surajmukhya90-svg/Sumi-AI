package com.sumi.ai.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.sumi.ai.core.ai.GeminiBrain
import com.sumi.ai.core.brain.ActionType
import com.sumi.ai.core.brain.SumiBrain
import com.sumi.ai.core.voice.SumiVoiceEngine
import com.sumi.ai.service.overlay.SumiOverlayService
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

val SumiPink = Color(0xFFFF4081)
val SumiBg = Color(0xFFFFF0F5)
val SumiText = Color(0xFF2E1A22)

data class FeatureItem(
    val name: String,
    val icon: ImageVector,
    val color: Color
)

class MainActivity : ComponentActivity() {

    private var voiceEngine: SumiVoiceEngine? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                val sharedPref = remember { context.getSharedPreferences("sumi_prefs", Context.MODE_PRIVATE) }

                var apiKey by remember { mutableStateOf(sharedPref.getString("gemini_api_key", "") ?: "") }
                var statusText by remember { mutableStateOf("Ready") }
                var userSpokenText by remember { mutableStateOf("") }
                var sumiReplyText by remember { mutableStateOf("Konnichiwa! Main Sumi hoon~ Tap karke baat kijiye sir jii! 🌸") }
                var avatarBitmap by remember { mutableStateOf<Bitmap?>(null) }
                var showSettingsDialog by remember { mutableStateOf(false) }

                fun playCustomVoiceIfAvailable(): Boolean {
                    val customAudioFile = File(context.filesDir, "custom_voice.mp3")
                    if (customAudioFile.exists()) {
                        try {
                            val mp = MediaPlayer().apply {
                                setDataSource(customAudioFile.absolutePath)
                                prepare()
                                start()
                                setOnCompletionListener { it.release() }
                            }
                            return true
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    return false
                }

                LaunchedEffect(Unit) {
                    val file = File(context.filesDir, "custom_avatar.png")
                    if (file.exists()) {
                        avatarBitmap = BitmapFactory.decodeFile(file.absolutePath)
                    }
                }

                // 1. Photo Picker
                val imagePickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri: Uri? ->
                    uri?.let { selectedUri ->
                        try {
                            val stream = context.contentResolver.openInputStream(selectedUri)
                            val decoded = BitmapFactory.decodeStream(stream)
                            stream?.close()
                            decoded?.let { bmp ->
                                avatarBitmap = bmp
                                val file = File(context.filesDir, "custom_avatar.png")
                                val out = FileOutputStream(file)
                                bmp.compress(Bitmap.CompressFormat.PNG, 95, out)
                                out.flush()
                                out.close()
                                sumiReplyText = "Milashka~ Kitni pyari photo lagayi hai meri! Thank you sir jii! 🌸"
                                voiceEngine?.speak("Milashka~ Kitni pyari photo lagayi hai meri! Thank you sir jii!")
                            }
                        } catch (e: Exception) {}
                    }
                }

                // 2. Custom Audio/Voice File Picker (Internet se download ki hui mp3 lagane ke liye)
                val audioPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri: Uri? ->
                    uri?.let { selectedUri ->
                        try {
                            val inputStream = context.contentResolver.openInputStream(selectedUri)
                            val destFile = File(context.filesDir, "custom_voice.mp3")
                            val out = FileOutputStream(destFile)
                            inputStream?.copyTo(out)
                            inputStream?.close()
                            out.flush()
                            out.close()

                            sumiReplyText = "Wah sir jii! Nayi real voice file set ho gayi hai! 🎵"
                            playCustomVoiceIfAvailable()
                            Toast.makeText(context, "Real Custom Voice Set Ho Gayi!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Audio file set nahi ho paayi!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                DisposableEffect(Unit) {
                    voiceEngine = SumiVoiceEngine(
                        context = context,
                        onSpeechRecognized = { query ->
                            userSpokenText = query
                            statusText = "Thinking..."

                            val offlineResp = SumiBrain.processQuery(context, query)
                            if (offlineResp.actionType != ActionType.NONE) {
                                sumiReplyText = offlineResp.replyText
                                if (!playCustomVoiceIfAvailable()) {
                                    voiceEngine?.speak(offlineResp.replyText)
                                }
                                SumiBrain.executeAction(context, offlineResp)
                                statusText = "Ready"
                            } else {
                                scope.launch {
                                    val aiResponse = GeminiBrain.getAiReply(apiKey, query)
                                    sumiReplyText = aiResponse
                                    if (!playCustomVoiceIfAvailable()) {
                                        voiceEngine?.speak(aiResponse)
                                    }
                                    statusText = "Ready"
                                }
                            }
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
                    }
                }

                fun triggerListening() {
                    val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                    if (hasMic) {
                        voiceEngine?.startListening()
                    } else {
                        val needed = mutableListOf(Manifest.permission.RECORD_AUDIO)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            needed.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        micLauncher.launch(needed.toTypedArray())
                    }
                }

                Surface(modifier = Modifier.fillMaxSize(), color = SumiBg) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(2.dp))

                        // Avatar Circle
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(135.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(4.dp, SumiPink, CircleShape)
                                .clickable { triggerListening() }
                        ) {
                            if (avatarBitmap != null) {
                                Image(
                                    bitmap = avatarBitmap!!.asImageBitmap(),
                                    contentDescription = "Sumi Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "👧🌸", fontSize = 38.sp)
                                    Text(text = "Tap to Talk", fontSize = 11.sp, color = SumiPink, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Photo Picker Button
                        TextButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = SumiPink, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Anime Photo Set Karein", color = SumiPink, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // Custom Voice File Picker Button (.mp3/.wav)
                        TextButton(onClick = { audioPickerLauncher.launch("audio/*") }) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color(0xFF673AB7), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("🎵 Internet Se Download Ki Hui Voice File Lagayein (.mp3)", color = Color(0xFF673AB7), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Status Badge
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(20.dp),
                            shadowElevation = 2.dp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = "● Status: $statusText (Tap to Speak)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = SumiText,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                            )
                        }

                        // Speech Conversation Bubble
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                if (userSpokenText.isNotEmpty()) {
                                    Text(text = "👤 Aap: $userSpokenText", fontSize = 12.sp, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(2.dp))
                                }
                                Text(
                                    text = "🌸 Sumi: $sumiReplyText",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SumiText
                                )
                            }
                        }

                        // Feature Grid
                        val featureList = listOf(
                            FeatureItem("Talk to Sumi", Icons.Default.Call, SumiPink),
                            FeatureItem("Settings (AI Key)", Icons.Default.Settings, Color(0xFF673AB7)),
                            FeatureItem("Floating Mascot", Icons.Default.Visibility, Color(0xFF009688)),
                            FeatureItem("Screen Reader (GF)", Icons.Default.PhoneAndroid, Color(0xFFE91E63)),
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
                                    onClick = {
                                        when (feat.name) {
                                            "Talk to Sumi" -> triggerListening()
                                            "Settings (AI Key)" -> showSettingsDialog = true
                                            "Floating Mascot" -> {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                                                    Toast.makeText(context, "Sumi ko 'Display over other apps' allow kijiye!", Toast.LENGTH_LONG).show()
                                                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                                    context.startActivity(intent)
                                                } else {
                                                    context.startService(Intent(context, SumiOverlayService::class.java))
                                                    Toast.makeText(context, "Sumi animated mascot screen par active!", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            "Screen Reader (GF)", "Permissions" -> {
                                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                                context.startActivity(intent)
                                            }
                                            else -> {
                                                Toast.makeText(context, "${feat.name} ready!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(14.dp),
                                    elevation = CardDefaults.cardElevation(2.dp),
                                    modifier = Modifier.fillMaxWidth().height(60.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(feat.icon, contentDescription = null, tint = feat.color, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = feat.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SumiText)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Stop Button
                        Button(
                            onClick = {
                                voiceEngine?.stopListening()
                                context.stopService(Intent(context, SumiOverlayService::class.java))
                                statusText = "Stopped"
                                sumiReplyText = "⛔ Sumi shant ho gayi hai sir jii!"
                                Toast.makeText(context, "STOP SUMI Activated!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD50000)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "STOP SUMI", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                        }
                    }
 
