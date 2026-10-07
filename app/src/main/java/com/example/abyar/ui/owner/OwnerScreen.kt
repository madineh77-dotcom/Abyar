package com.example.abyar.ui.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.abyar.ui.admin.AdminViewModel
import com.example.abyar.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun OwnerScreen(vm: AdminViewModel = viewModel()) {
    val turns by vm.turns.collectAsState()
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BackgroundLight),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                    .background(Brush.horizontalGradient(colors = listOf(AbyarGreen, AbyarTeal)))
                    .padding(24.dp)
            ) {
                Column {
                    Icon(Icons.Default.Agriculture, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("پنل مالک", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        "مشاهده نوبت‌های آبیاری شما",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }

        if (turns.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.WaterDrop,
                            null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("هنوز نوبتی ثبت نشده است", fontSize = 16.sp, color = TextSecondary)
                        Text("منتظر بمانید تا مدیر چاه نوبت‌بندی را انجام دهد", fontSize = 13.sp, color = Color.Gray)
                    }
                }
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Schedule, null, tint = AbyarTeal)
                    Spacer(Modifier.width(8.dp))
                    Text("نوبت‌های شما (${turns.size})", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            items(turns) { turn ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(3.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CardGreenLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, null, tint = AbyarGreen)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(turn.userName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("مالک آبیاری", fontSize = 12.sp, color = TextSecondary)
                            }
                            Surface(color = CardGreenLight, shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    turn.status,
                                    color = AbyarGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Divider(color = Color(0xFFEEEEEE))
                        Spacer(Modifier.height(14.dp))
                        Row {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PlayArrow, null, tint = AbyarBlue, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("شروع", fontSize = 12.sp, color = TextSecondary)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(dateFormat.format(Date(turn.startTime)), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Stop, null, tint = AbyarRed, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("پایان", fontSize = 12.sp, color = TextSecondary)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(dateFormat.format(Date(turn.endTime)), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
