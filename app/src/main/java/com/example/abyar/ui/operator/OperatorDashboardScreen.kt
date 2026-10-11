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
import com.example.abyar.util.formatPersianDate
import com.example.abyar.util.formatPersianTime
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

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
    val owners by vm.owners.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var turnToMove by remember { mutableStateOf<IrrigationTurn?>(null) }

    LaunchedEffect(wellId) { vm.loadTurns(wellId) }

    // نوبت‌های امروز (بر اساس تقویم تهران)
    val todayTurns = remember(turns) {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran"))
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val dayStart = cal.timeInMillis
        val dayEnd = dayStart + (24 * 60 * 60 * 1000L)
        turns.filter { it.startTime in dayStart until dayEnd }
            .sortedBy { it.startTime }
    }

    // نوبت بعدی: اولین نوبت PENDING در آینده
    val nextTurn = remember(turns) {
        val now = System.currentTimeMillis()
        turns.filter { it.startTime > now && it.status == "PENDING" }
            .minByOrNull { it.startTime }
    }

    // تابع کمکی برای پیدا کردن شماره تماس
    fun getPhoneFor(userId: String): String? {
        return owners.find { it.id == userId }?.phone
    }

    fun callNumber(phone: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
        runCatching { context.startActivity(intent) }
            .onFailure {
                scope.launch {
                    snackbarHostState.showSnackbar("امکان تماس وجود ندارد")
                }
            }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(BackgroundLight)
                .padding(padding)
        ) {
            // هدر ساده
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                    .background(Brush.horizontalGradient(listOf(AbyarPurple, AbyarBlueDark)))
                    .padding(horizontal = 12.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                    Spacer(Modifier.width(4.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "پنل موتوربان",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            wellName,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = onFullSchedule) {
                        Icon(Icons.Default.CalendarMonth, null, tint = Color.White)
                    }
                }
            }

            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // کارت نوبت بعدی
                item {
                    val nt = nextTurn
                    if (nt != null) {
                        NextTurnCard(
                            turn = nt,
                            phone = getPhoneFor(nt.userId),
                            onToggle = { vm.toggleTurnStatus(nt) { } },
                            onCall = { phone -> callNumber(phone) }
                        )
                    } else {
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardGreenLight),
                            elevation = CardDefaults.cardElevation(3.dp)
                        ) {
                            Row(
                                Modifier.padding(20.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    null,
                                    tint = AbyarGreen,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(
                                        "همه نوبت‌ها انجام شده",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = AbyarGreen
                                    )
                                    Text(
                                        "نوبت بعدی وجود ندارد",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // عنوان لیست نوبت‌های امروز
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Today, null, tint = AbyarBlue, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "نوبت‌های امروز (${todayTurns.size})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                // لیست نوبت‌های امروز
                if (todayTurns.isEmpty()) {
                    item {
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.EventAvailable,
                                    null,
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text("امروز نوبتی وجود ندارد", fontSize = 15.sp, color = TextSecondary)
                            }
                        }
                    }
                } else {
                    items(todayTurns, key = { it.id }) { t ->
                        TodayTurnCard(
                            turn = t,
                            phone = getPhoneFor(t.userId),
                            onToggle = { vm.toggleTurnStatus(t) { } },
                            onCall = { phone -> callNumber(phone) }
                        )
                    }
                }

                // دکمه‌های پایین
                item {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BigActionCard(
                            icon = Icons.Default.SwapHoriz,
                            title = "جابجایی",
                            color = AbyarOrange,
                            onClick = onFullSchedule,
                            modifier = Modifier.weight(1f)
                        )
                        BigActionCard(
                            icon = Icons.Default.Groups,
                            title = "گروه‌ها",
                            color = AbyarPurple,
                            onClick = onManageGroups,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    // شیت جابجایی سریع
    turnToMove?.let { turn ->
        QuickMoveSheet(
            turn = turn,
            owners = owners,
            onDismiss = { turnToMove = null },
            onSelect = { userId, userName ->
                vm.moveTurnToUser(turn, userId, userName) {
                    turnToMove = null
                    scope.launch {
                        snackbarHostState.showSnackbar("نوبت به $userName منتقل شد")
                    }
                }
            }
        )
    }
}

/**
 * کارت نوبت بعدی (بزرگ)
 */
@Composable
private fun NextTurnCard(
    turn: IrrigationTurn,
    phone: String?,
    onToggle: () -> Unit,
    onCall: (String) -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(5.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AbyarOrange.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.NotificationsActive, null, tint = AbyarOrange, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "نوبت بعدی",
                        fontSize = 13.sp,
                        color = AbyarOrange,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        turn.userName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, null, tint = AbyarBlue, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    formatPersianDate(turn.startTime),
                    fontSize = 14.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(16.dp))
                Icon(Icons.Default.Schedule, null, tint = AbyarBlue, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "${formatPersianTime(turn.startTime)} تا ${formatPersianTime(turn.endTime)}",
                    fontSize = 14.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(16.dp))

            // دکمه‌های بزرگ
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onToggle,
                    modifier = Modifier.weight(1.3f).height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AbyarGreen)
                ) {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("انجام شد", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { phone?.let { onCall(it) } },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AbyarBlue),
                    enabled = phone != null
                ) {
                    Icon(Icons.Default.Phone, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("تماس", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * کارت نوبت در لیست امروز
 */
@Composable
private fun TodayTurnCard(
    turn: IrrigationTurn,
    phone: String?,
    onToggle: () -> Unit,
    onCall: (String) -> Unit
) {
    val isCompleted = turn.status == "COMPLETED"
    val statusColor = if (isCompleted) AbyarGreen else AbyarOrange

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) CardGreenLight else Color.White
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // نشانگر رنگ
            Box(
                Modifier
                    .size(5.dp, 46.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(statusColor)
            )
            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    turn.userName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        null,
                        tint = AbyarBlue,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${formatPersianTime(turn.startTime)} - ${formatPersianTime(turn.endTime)}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            // دکمه تماس کوچک
            if (phone != null) {
                IconButton(
                    onClick = { onCall(phone) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AbyarBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            null,
                            tint = AbyarBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // دکمه تیک
            IconButton(
                onClick = onToggle,
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isCompleted) Icons.Default.Undo else Icons.Default.Check,
                        null,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * کارت دکمه بزرگ پایین صفحه
 */
@Composable
private fun BigActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(76.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

/**
 * شیت جابجایی سریع
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickMoveSheet(
    turn: IrrigationTurn,
    owners: List<com.example.abyar.data.User>,
    onDismiss: () -> Unit,
    onSelect: (String, String) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .heightIn(max = 500.dp)
        ) {
            Text(
                "جابجایی نوبت",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AbyarBlueDark
            )
            Text(
                "نوبت ${turn.userName} • ${formatPersianTime(turn.startTime)} تا ${formatPersianTime(turn.endTime)}",
                fontSize = 12.sp,
                color = TextSecondary
            )
            Spacer(Modifier.height(16.dp))
            Text("انتقال به:", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            val candidates = owners.filter { it.id != turn.userId && it.shareHours > 0 }
            if (candidates.isEmpty()) {
                Text("مالکی برای انتقال وجود ندارد", color = AbyarRed, fontSize = 13.sp)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(candidates) { owner ->
                        Card(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(owner.id, owner.fullName) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Row(
                                Modifier.padding(12.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(AbyarBlue.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        null,
                                        tint = AbyarBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(owner.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        "سهم: ${owner.shareHours} ساعت",
                                        fontSize = 11.sp,
                                        color = AbyarGreen
                                    )
                                }
                                Icon(Icons.Default.ChevronLeft, null, tint = AbyarBlue)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
