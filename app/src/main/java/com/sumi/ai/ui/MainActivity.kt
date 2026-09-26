package com.sumi.ai.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Sumi Cute Anime Color Palette
val SumiPinkPrimary = Color(0xFFFF4081)
val SumiPinkSecondary = Color(0xFFFF80AB)
val SumiPinkBackground = Color(0xFFFFF0F5)
val SumiCardSurface = Color(0xFFFFFFFF)
val SumiTextDark = Color(0xFF2E1A22)
val SumiGreenReady = Color(0xFF00C853)
val SumiEmergencyRed = Color(0xFFD50000)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SumiDashboardScreen(
                    onActionClick = { title ->
                        Toast.makeText(this, "$title: Phase 1 ready!", Toast.LENGTH_SHORT).show()
                    },
                    onEmergencyStop = {
                        Toast.makeText(this, "⛔ SUMI STOPPED! Emergency killswitch activated.", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }
}

@Composable
fun SumiDashboardScreen(
    onActionClick: (String) -> Unit,
    onEmergencyStop: () -> Unit
) {
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

            // 1. Anime Avatar Circular Card
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(SumiPinkSecondary, SumiPinkPrimary)
                        )
                    )
                    .border(4.dp, Color.White, CircleShape)
            ) {
                Text(
                    text = "🌸\nSUMI",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Greeting & Tagline
            Text(
                text = "Hi ji! Main Sumi hoon 👋",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = SumiTextDark
            )
            Text(
                text = "Your personal Hindi AI companion & phone controller",
                fontSize = 13.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            // 3. Status Badge
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 2.dp,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(SumiGreenReady)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "● Sumi is ready",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = SumiTextDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Feature Buttons Grid (Using 100% Standard Universal Android Icons)
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

            // 5. Emergency Kill Switch (Stop Sumi)
            Button(
                onClick = onEmergencyStop,
                colors = ButtonDefaults.buttonColors(containerColor = SumiEmergencyRed),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "STOP SUMI",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
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
        modifier = Modifier.fillMaxWidth().height(85.dp)
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
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(item.tint.copy(alpha = 0.15f))
            ) {
                Icon(item.icon, contentDescription = null, tint = item.tint, modifier = Modifier.size(24.dp))
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
