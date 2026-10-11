package com.example.abyar.ui.operator

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.abyar.data.IrrigationTurn
import com.example.abyar.ui.schedule.ScheduleViewModel
import com.example.abyar.ui.theme.*
import com.example.abyar.util.formatPersianTime
import com.example.abyar.util.formatPersianDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperatorDashboardScreen(
    wellId: String,
    wellName: String,
    onBack: () -> Unit,
    onManageGroups: () -> Unit,
    onFullSchedule: () -> Unit,
    vm: ScheduleViewModel = viewModel()
) {
    val turns by vm.turns.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(wellId) { vm.loadTurns(wellId) }

    // نوبت‌های امروز
    val todayTurns = remember(turns) {
        val now = System.currentTimeMillis()
        val dayStart = now - (now % (24 * 60 * 60 * 1000L))
        val dayEnd = dayStart + (24 * 60 * 60 * 1000L)
        turns.filter { it.startTime in dayStart until dayEnd }
    }

    // نوبت بعدی
    val nextTurn = remember(turns) {
        val now = System.currentTimeMillis()
        turns.filter { it.startTime > now && it.status == "PENDING" }
            .minByOrNull { it.startTime }
    }

    Column(Modifier.fillMaxSize().background(BackgroundLight)) {
        // هدر گرادیانت
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(Brush.horizontalGradient(listOf(AbyarPurple, AbyarBlueDark)))
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("پنل موتوربان", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(wellName, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                    }
                    IconButton(onClick = onManageGroups) {
                        Icon(Icons.Default.Groups, null, tint = Color.White)
                    }
                    IconButton(onClick = onFullSchedule) {
                        Icon(Icons.Default.CalendarMonth, null, tint = Color.White)
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                Spacer(Modifier.height(12.dp))

                // آمار امروز
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    val pendingCount = turns.count { it.status == "PENDING" }
                    val completedCount = turns.count { it.status == "COMPLETED" }
                    OpStat("نوبت‌های امروز", "${todayTurns.size}", Color.White)
                    OpStat("در انتظار", "$pendingCount", Color.White)
                    OpStat("انجام‌شده", "$completedCount", Color.White)
                }
            }
        }

        // نوبت بعدی
        nextTurn?.let { nt ->
            Card(
                Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AbyarOrange.copy(alpha = 0.15f)),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsActive, null, tint = AbyarOrange)
                        Spacer(Modifier.width(8.dp))
                        Text("نوبت بعدی", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AbyarOrange)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(nt.userName, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(
                        "${formatPersianDate(nt.startTime)} | ساعت ${formatPersianTime(nt.startTime)}",
                        fontSize = 13.sp, color = TextSecondary
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                // تایید و تکمیل نوبت
                                vm.toggleTurnStatus(nt) { }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = AbyarGreen),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("تایید", fontSize = 13.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                val phone = nt.userName // اینجا باید شماره تماس باشد
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                runCatching { context.startActivity(intent) }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Phone, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("تماس", fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // لیست نوبت‌های امروز
        Text(
            "نوبت‌های امروز",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        if (todayTurns.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.EventAvailable, null, tint = Color.LightGray, modifier = Modifier.size(72.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("امروز نوبتی وجود ندارد", fontSize = 16.sp, color = TextSecondary)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(todayTurns, key = { it.id }) { t ->
                    OperatorTurnCard(
                        turn = t,
                        onToggle = { vm.toggleTurnStatus(t) { } }
                    )
                }
            }
        }
    }
}

@Composable
private fun OpStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold
