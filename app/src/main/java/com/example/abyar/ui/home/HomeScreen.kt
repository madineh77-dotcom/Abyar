package com.example.abyar.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import com.example.abyar.ui.theme.*

data class HomePanel(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val route: String
)

@Composable
fun HomeScreen(
    userName: String,
    phone: String,
    wellName: String,
    wellCode: String,
    nextTurnInfo: String,
    onPanelClick: (String) -> Unit
) {
    val panels = listOf(
        HomePanel("ثبت چاه", Icons.Default.Water, AbyarBlue, "well"),
        HomePanel("مدیریت مالکان", Icons.Default.Group, AbyarGreen, "owners"),
        HomePanel("برنامه آبیاری", Icons.Default.CalendarMonth, AbyarTeal, "schedule"),
        HomePanel("مدیریت مالی", Icons.Default.AccountBalanceWallet, AbyarOrange, "finance"),
        HomePanel("پنل موتوربان", Icons.Default.Engineering, AbyarPurple, "operator"),
        HomePanel("پنل آبیار", Icons.Default.Agriculture, Color(0xFF00897B), "abyar"),
        HomePanel("اطلاع‌رسانی", Icons.Default.Campaign, Color(0xFFD81B60), "notify"),
        HomePanel("دفترچه تلفن", Icons.Default.ContactPhone, Color(0xFF5E35B1), "contacts"),
        HomePanel("قوانین و ضوابط", Icons.Default.Gavel, Color(0xFF6D4C41), "rules"),
        HomePanel("گزارش‌ها", Icons.Default.BarChart, Color(0xFF00838F), "reports"),
        HomePanel("تنظیمات", Icons.Default.Settings, Color(0xFF546E7A), "settings"),
        HomePanel("پشتیبانی", Icons.Default.SupportAgent, Color(0xFFC62828), "support")
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize().background(BackgroundLight),
        contentPadding = PaddingValues(bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // هدر گرادیانت
        item(span = { GridItemSpan(3) }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                    .background(Brush.horizontalGradient(colors = listOf(AbyarBlueDark, AbyarTeal)))
                    .padding(20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("خوش آمدید", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                            Text(userName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(phone, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                        }
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.Edit, null, tint = Color.White)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Divider(color = Color.White.copy(alpha = 0.2f))
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Water, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("چاه: $wellName", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(12.dp))
                        Text("کد: $wellCode", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("نوبت شما: $nextTurnInfo", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                    }
                }
            }
        }

        // عنوان دسترسی سریع
        item(span = { GridItemSpan(3) }) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Bolt, null, tint = AbyarOrange)
                Spacer(Modifier.width(8.dp))
                Text("دسترسی سریع", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        }

        // کارت‌های پنل‌ها
        items(panels) { panel ->
            PanelCard(panel = panel, onClick = { onPanelClick(panel.route) })
        }
    }
}

@Composable
fun PanelCard(panel: HomePanel, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(panel.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(panel.icon, null, tint = panel.color, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(
                panel.title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = 13.sp
            )
        }
    }
}
