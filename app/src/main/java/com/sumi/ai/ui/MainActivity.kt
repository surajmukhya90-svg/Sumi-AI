package com.sumi.ai.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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

class MainActivity : ComponentActivity() {
    private var voiceEngine: SumiVoiceEngine? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val sp = remember { context.getSharedPreferences("sumi_prefs", Context.MODE_PRIVATE) }
            var apiKey by remember { mutableStateOf(sp.getString("gemini_api_key", "") ?: "") }
            var sumiText by remember { mutableStateOf("Konnichiwa! Main Sumi hoon~ Tap karke baat kijiye! 🌸") }
            var userText by remember { mutableStateOf("") }
            var showKeyDialog by remember { mutableStateOf(false) }

            val avatarFile = File(context.filesDir, "custom_avatar.png")
            var avatarBmp by remember { mutableStateOf(if (avatarFile.exists()) BitmapFactory.decodeFile(avatarFile.path) else null) }

            fun playVoice(): Boolean {
                val f = File(context.filesDir, "custom_voice.mp3")
                if (!f.exists()) return false
                try {
                    MediaPlayer().apply { setDataSource(f.path); prepare(); start(); setOnCompletionListener { it.release() } }
                    return true
                } catch (e: Exception) { return false }
            }

            val imgPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                uri?.let {
                    val bmp = BitmapFactory.decodeStream(context.contentResolver.openInputStream(it))
                    bmp?.let { b ->
                        avatarBmp = b
                        FileOutputStream(avatarFile).use { out -> b.compress(android.graphics.Bitmap.CompressFormat.PNG, 95, out) }
                        sumiText = "Milashka~ Photo set ho gayi! 🌸"
                    }
                }
            }

            val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                uri?.let {
                    FileOutputStream(File(context.filesDir, "custom_voice.mp3")).use { out ->
                        context.contentResolver.openInputStream(it)?.copyTo(out)
                    }
                    sumiText = "Real custom voice set ho gayi sir jii! 🎵"
                    playVoice()
                }
            }

            DisposableEffect(Unit) {
                voiceEngine = SumiVoiceEngine(context, { query ->
                    userText = query
                    val resp = SumiBrain.processQuery(context, query)
                    if (resp.actionType != ActionType.NONE) {
                        sumiText = resp.replyText
                        if (!playVoice()) voiceEngine?.speak(resp.replyText)
                        SumiBrain.executeAction(context, resp)
                    } else {
                        scope.launch {
                            val ai = GeminiBrain.getAiReply(apiKey, query)
                            sumiText = ai
                            if (!playVoice()) voiceEngine?.speak(ai)
                        }
                    }
                }, {})
                onDispose { voiceEngine?.shutdown() }
            }

            val micPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
                if (ok) voiceEngine?.startListening()
            }

            fun listen() {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    voiceEngine?.startListening()
                } else micPerm.launch(Manifest.permission.RECORD_AUDIO)
            }

            Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFFFF0F5)) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.size(130.dp).clip(CircleShape).background(Color.White).border(3.dp, Color(0xFFFF4081), CircleShape).clickable { listen() }, Alignment.Center) {
                        if (avatarBmp != null) Image(avatarBmp!!.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        else Text("👧🌸\nTap", fontSize = 16.sp, color = Color(0xFFFF4081), fontWeight = FontWeight.Bold)
                    }
                    Row {
                        TextButton(onClick = { imgPicker.launch("image/*") }) { Text("📸 Photo Set", fontSize = 12.sp, color = Color(0xFFFF4081)) }
                        TextButton(onClick = { audioPicker.launch("audio/*") }) { Text("🎵 Voice Set", fontSize = 12.sp, color = Color(0xFF673AB7)) }
                    }
                    Card(Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(12.dp)) {
                            if (userText.isNotEmpty()) Text("👤 $userText", fontSize = 12.sp, color = Color.Gray)
                            Text("🌸 Sumi: $sumiText", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2E1A22))
                        }
                    }
                    Button(onClick = { listen() }, Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4081))) {
                        Text("🎙️ Talk To Sumi", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { showKeyDialog = true }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7))) {
                            Text("🧠 AI Key", fontSize = 12.sp)
                        }
                        Button(onClick = {
                            if (Settings.canDrawOverlays(context)) {
                                context.startService(Intent(context, SumiOverlayService::class.java))
                                Toast.makeText(context, "Mascot Active!", Toast.LENGTH_SHORT).show()
                            } else context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                        }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009688))) {
                            Text("🧚 Mascot", fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        voiceEngine?.stopListening()
                        context.stopService(Intent(context, SumiOverlayService::class.java))
                        sumiText = "⛔ Sumi Stopped!"
                    }, Modifier.fillMaxWidth().height(44.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD50000))) {
                        Text("⛔ STOP SUMI", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (showKeyDialog) {
                var k by remember { mutableStateOf(apiKey) }
                AlertDialog(
                    onDismissRequest = { showKeyDialog = false },
                    title = { Text("Gemini AI Key") },
                    text = { OutlinedTextField(k, { k = it }, label = { Text("Paste AQ... Key") }) },
                    confirmButton = {
                        TextButton(onClick = {
                            apiKey = k.trim()
                            sp.edit().putString("gemini_api_key", apiKey).apply()
                            showKeyDialog = false
                            sumiText = "AI Connected! 🌸"
                        }) { Text("Save") }
                    }
                )
            }
        }
    }
}
