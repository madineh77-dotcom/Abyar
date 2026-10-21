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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.abyar.data.IrrigationTurn
import com.example.abyar.data.User
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
    onManageOwners: () -> Unit,
    onFullSchedule: () -> Unit,
    onNotify: () -> Unit,
    onEditProfile: () -> Unit,
    vm: ScheduleViewModel = viewModel()
) {
    val turns by vm.turns.collectAsState()
    val owners by vm.owners.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var turnForActions by remember { mutableStateOf<IrrigationTurn?>(null) }
    var showMoveSheet by remember { mutableStateOf(false) }
    var turnToDelete by remember { mutableStateOf<IrrigationTurn?>(null) }

    LaunchedEffect(wellId) { vm.loadTurns(wellId) }

    LaunchedEffect(Unit) {
        vm.error.collect { err ->
            if (err.isNotEmpty()) {
                snackbarHostState.showSnackbar(err, duration = SnackbarDuration.Short)
                vm.clearMessages()
            }
        }
    }
    LaunchedEffect(Unit) {
        vm.info.collect { inf ->
            if (inf.isNotEmpty()) {
                snackbarHostState.showSnackbar(inf, duration = SnackbarDuration.Short)
                vm.clearMessages()
            }
        }
    }

    val todayTurns = remember(turns) {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran"))
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val dayStart = cal.timeInMillis
        val dayEnd = dayStart + (24 * 60 * 60 * 1000L)
        turns.filter { it.startTime in dayStart until dayEnd }.sortedBy { it.startTime }
    }

    val nextTurn = remember(turns) {
        val now = System.currentTimeMillis()
        turns.filter { it.startTime > now && it.status == "PENDING" }.minByOrNull { it.startTime }
    }

    fun getPhoneFor(userId: String): String? = owners.find { it.id == userId }?.phone

    fun callNumber(phone: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
        runCatching { context.startActivity(intent) }.onFailure {
            scope.launch { snackbarHostState.showSnackbar("امکان تماس وجود ندارد") }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().background(BackgroundLight).padding(padding)
        ) {
            // هدر
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                    .background(Brush.horizontalGradient(listOf(AbyarPurple, AbyarBlueDark)))
                    .padding(horizontal = 12.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
                    Spacer(Modifier.width(4.dp))
                    Column(Modifier.weight(1f)) {
                        Text("پنل موتوربان", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(wellName, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                    }
                    IconButton(onClick = onManageOwners) { Icon(Icons.Default.People, null, tint = Color.White) }
                    IconButton(onClick = onFullSchedule) { Icon(Icons.Default.CalendarMonth, null, tint = Color.White) }
                    IconButton(onClick = onEditProfile) { Icon(Icons.Default.Person, null, tint = Color.White) }
                }
            }

            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    val nt = nextTurn
                    if (nt != null) {
                        NextTurnCard(
                            turn = nt,
                            phone = getPhoneFor(nt.userId),
                            onToggle = { vm.toggleTurnStatus(nt) { } },
                            onCall = { phone -> callNumber(phone) },
                            onMoreActions = { turnForActions = nt }
                        )
                    } else {
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardGreenLight),
                            elevation = CardDefaults.cardElevation(3.dp)
                        ) {
                            Row(Modifier.padding(20.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = AbyarGreen, modifier = Modifier.size(48.dp))
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text("همه نوبت‌ها انجام شده", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = AbyarGreen)
                                    Text("نوبت بعدی وجود ندارد", fontSize = 12.sp, color = TextSecondary)
                                }
                            }
                        }
                    }
                }

                item {
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Today, null, tint = AbyarBlue, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("نوبت‌های امروز (${todayTurns.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                }

                if (todayTurns.isEmpty()) {
                    item {
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.EventAvailable, null, tint = Color.LightGray, modifier = Modifier.size(56.dp))
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
                            onCall = { phone -> callNumber(phone) },
                            onMoreActions = { turnForActions = t }
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BigActionCard(icon = Icons.Default.People, title = "مالکان", color = AbyarTeal, onClick = onManageOwners, modifier = Modifier.weight(1f))
                        BigActionCard(icon = Icons.Default.CalendarMonth, title = "برنامه", color = AbyarBlue, onClick = onFullSchedule, modifier = Modifier.weight(1f))
                        BigActionCard(icon = Icons.Default.Campaign, title = "اطلاع‌رسانی", color = AbyarGreen, onClick = onNotify, modifier = Modifier.weight(1f))
                        BigActionCard(icon = Icons.Default.Groups, title = "گروه‌ها", color = AbyarPurple, onClick = onManageGroups, modifier = Modifier.weight(1f))
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    turnForActions?.let { turn ->
        TurnActionsSheet(
            turn = turn,
            phone = getPhoneFor(turn.userId),
            onDismiss = { turnForActions = null },
            onCall = { phone -> turnForActions = null; callNumber(phone) },
            onToggle = { vm.toggleTurnStatus(turn) { }; turnForActions = null },
            onMove = { showMoveSheet = true },
            onDelete = { turnToDelete = turn; turnForActions = null }
        )
    }

    if (showMoveSheet && turnForActions != null) {
        OperatorMoveTurnSheet(
            currentTurn = turnForActions!!,
            allTurns = turns,
            onDismiss = { showMoveSheet = false; turnForActions = null },
            onSelect = { targetTurn ->
                vm.swapTurns(turnForActions!!, targetTurn) {
                    showMoveSheet = false
                    turnForActions = null
                }
            }
        )
    }

    turnToDelete?.let { turn ->
        AlertDialog(
            onDismissRequest = { turnToDelete = null },
            icon = { Icon(Icons.Default.Warning, null, tint = AbyarRed) },
            title = { Text("حذف نوبت", fontWeight = FontWeight.Bold) },
            text = { Text("آیا از حذف نوبت «${turn.userName}» مطمئن هستید؟ نوبت‌های بعدی به عقب شیفت داده می‌شوند.") },
            confirmButton = {
                Button(
                    onClick = { vm.deleteTurnAndShift(turn) { turnToDelete = null } },
                    colors = ButtonDefaults.buttonColors(containerColor = AbyarRed)
                ) { Text("حذف و شیفت") }
            },
            dismissButton = { TextButton(onClick = { turnToDelete = null }) { Text("انصراف") } }
        )
    }
}

@Composable
private fun NextTurnCard(
    turn: IrrigationTurn,
    phone: String?,
    onToggle: () -> Unit,
    onCall: (String) -> Unit,
    onMoreActions: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(5.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(AbyarOrange.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.NotificationsActive, null, tint = AbyarOrange, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("نوبت بعدی", fontSize = 13.sp, color = AbyarOrange, fontWeight = FontWeight.Bold)
                    Text(turn.userName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                IconButton(onClick = onMoreActions) { Icon(Icons.Default.MoreVert, null, tint = TextSecondary) }
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, null, tint = AbyarBlue, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(formatPersianDate(turn.startTime), fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(16.dp))
                Icon(Icons.Default.Schedule, null, tint = AbyarBlue, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("${formatPersianTime(turn.startTime)} تا ${formatPersianTime(turn.endTime)}", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onToggle, modifier = Modifier.weight(1.3f).height(56.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = AbyarGreen)) {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("انجام شد", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Button(onClick = { phone?.let { onCall(it) } }, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = AbyarBlue), enabled = phone != null) {
                    Icon(Icons.Default.Phone, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("تماس", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TodayTurnCard(
    turn: IrrigationTurn,
    phone: String?,
    onToggle: () -> Unit,
    onCall: (String) -> Unit,
    onMoreActions: () -> Unit
) {
    val isCompleted = turn.status == "COMPLETED"
    val statusColor = if (isCompleted) AbyarGreen else AbyarOrange

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (isCompleted) CardGreenLight else Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(5.dp, 46.dp).clip(RoundedCornerShape(3.dp)).background(statusColor))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(turn.userName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, tint = AbyarBlue, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${formatPersianTime(turn.startTime)} - ${formatPersianTime(turn.endTime)}", fontSize = 12.sp, color = TextSecondary)
                }
            }
            if (phone != null) {
                IconButton(onClick = { onCall(phone) }, modifier = Modifier.size(40.dp)) {
                    Box(Modifier.size(36.dp).clip(CircleShape).background(AbyarBlue.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Phone, null, tint = AbyarBlue, modifier = Modifier.size(18.dp))
                    }
                }
            }
            IconButton(onClick = onToggle, modifier = Modifier.size(40.dp)) {
                Box(Modifier.size(36.dp).clip(CircleShape).background(statusColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(if (isCompleted) Icons.Default.Undo else Icons.Default.Check, null, tint = statusColor, modifier = Modifier.size(20.dp))
                }
            }
            IconButton(onClick = onMoreActions, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.MoreVert, null, tint = TextSecondary, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TurnActionsSheet(
    turn: IrrigationTurn,
    phone: String?,
    onDismiss: () -> Unit,
    onCall: (String) -> Unit,
    onToggle: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(20.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(AbyarBlue.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, null, tint = AbyarBlue)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(turn.userName, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("${formatPersianTime(turn.startTime)} تا ${formatPersianTime(turn.endTime)}", fontSize = 12.sp, color = TextSecondary)
                }
            }
            Spacer(Modifier.height(16.dp)); HorizontalDivider(); Spacer(Modifier.height(8.dp))

            if (phone != null) {
                ActionRow(icon = Icons.Default.Phone, color = AbyarBlue, title = "تماس با مالک", onClick = { onCall(phone) })
            }
            ActionRow(
                icon = if (turn.status == "COMPLETED") Icons.Default.Undo else Icons.Default.CheckCircle,
                color = if (turn.status == "COMPLETED") AbyarOrange else AbyarGreen,
                title = if (turn.status == "COMPLETED") "برگرداندن به در انتظار" else "علامت‌گذاری انجام‌شده",
                onClick = onToggle
            )
            ActionRow(
                icon = Icons.Default.SwapHoriz,
                color = AbyarOrange,
                title = "جابجایی نوبت",
                subtitle = "تعویض این نوبت با نوبت دیگر و شیفت دادن بقیه",
                onClick = onMove
            )
            ActionRow(icon = Icons.Default.Delete, color = AbyarRed, title = "حذف و شیفت", subtitle = "حذف این نوبت و شیفت دادن بقیه به عقب", onClick = onDelete)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ActionRow(icon: ImageVector, color: Color, title: String, subtitle: String? = null, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
                if (subtitle != null) { Text(subtitle, fontSize = 11.sp, color = TextSecondary) }
            }
            Icon(Icons.Default.ChevronLeft, null, tint = color)
        }
    }
}

@Composable
private fun BigActionCard(icon: ImageVector, title: String, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(86.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(color), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(4.dp))
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OperatorMoveTurnSheet(
    currentTurn: IrrigationTurn,
    allTurns: List<IrrigationTurn>,
    onDismiss: () -> Unit,
    onSelect: (IrrigationTurn) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(20.dp).fillMaxWidth().heightIn(max = 500.dp)) {
            Text("جابجایی نوبت", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AbyarBlueDark)
            Text("انتخاب زمان مقصد برای نوبت ${currentTurn.userName}", fontSize = 13.sp, color = TextSecondary)
            Spacer(Modifier.height(16.dp))

            Text("این نوبت را با کدام نوبت جابجا کنیم؟", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))

            val candidates = allTurns.filter { it.id != currentTurn.id }.sortedBy { it.startTime }
            if (candidates.isEmpty()) {
                Text("نوبت دیگری برای جابجایی وجود ندارد", color = AbyarRed, fontSize = 13.sp)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(candidates) { targetTurn ->
                        Card(
                            Modifier.fillMaxWidth().clickable { onSelect(targetTurn) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(38.dp).clip(CircleShape).background(AbyarOrange.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Schedule, null, tint = AbyarOrange, modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(targetTurn.userName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(formatPersianDateTime(targetTurn.startTime), fontSize = 11.sp, color = TextSecondary)
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

private fun formatPersianDateTime(timestamp: Long): String {
    return "${formatPersianDate(timestamp)} - ${formatPersianTime(timestamp)}"
}
