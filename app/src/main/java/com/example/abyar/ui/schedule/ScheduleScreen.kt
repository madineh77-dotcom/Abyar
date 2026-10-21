package com.example.abyar.ui.schedule

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.abyar.data.IrrigationTurn
import com.example.abyar.data.UserRole
import com.example.abyar.ui.theme.*
import com.example.abyar.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    wellId: String,
    userRole: UserRole?,
    onBack: () -> Unit,
    vm: ScheduleViewModel = viewModel()
) {
    val turns by vm.turns.collectAsState()
    val summaries by vm.ownerSummaries.collectAsState()
    val stats by vm.stats.collectAsState()
    val error by vm.error.collectAsState()
    val info by vm.info.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var showGenerateDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var filterStatus by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var actionTurn by remember { mutableStateOf<IrrigationTurn?>(null) }
    var showMoveSheet by remember { mutableStateOf(false) }
    var turnToDelete by remember { mutableStateOf<IrrigationTurn?>(null) }
    var showSummary by remember { mutableStateOf(false) }

    LaunchedEffect(wellId) { vm.loadTurns(wellId) }

    LaunchedEffect(error) {
        if (error.isNotEmpty()) { snackbarHostState.showSnackbar(error, duration = SnackbarDuration.Short); vm.clearMessages() }
    }
    LaunchedEffect(info) {
        if (info.isNotEmpty()) { snackbarHostState.showSnackbar(info, duration = SnackbarDuration.Short); vm.clearMessages() }
    }

    val filteredTurns = remember(turns, filterStatus, searchQuery) {
        turns.filter { turn ->
            val matchesStatus = filterStatus == "ALL" || turn.status == filterStatus
            val matchesSearch = searchQuery.isBlank() || turn.userName.contains(searchQuery, ignoreCase = true)
            matchesStatus && matchesSearch
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("برنامه آبیاری", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) } },
                actions = {
                    if (turns.isNotEmpty()) {
                        IconButton(onClick = { showSummary = !showSummary }) {
                            Icon(if (showSummary) Icons.Default.Analytics else Icons.Default.PieChart, null, tint = Color.White)
                        }
                        // مدیر اصلی و موتوربان می‌توانند برنامه را پاک کنند
                        if (userRole == UserRole.MAIN_ADMIN || userRole == UserRole.OPERATOR) {
                            IconButton(onClick = { showClearDialog = true }) {
                                Icon(Icons.Default.DeleteSweep, null, tint = Color.White)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AbyarBlueDark)
            )
        },
        floatingActionButton = {
            // مدیر اصلی و موتوربان می‌توانند برنامه تولید کنند
            if (userRole == UserRole.MAIN_ADMIN || userRole == UserRole.OPERATOR) {
                ExtendedFloatingActionButton(
                    onClick = { showGenerateDialog = true },
                    containerColor = AbyarGreen,
                    icon = { Icon(Icons.Default.AutoMode, null, tint = Color.White) },
                    text = { Text(if (turns.isEmpty()) "تولید برنامه" else "تولید مجدد", color = Color.White, fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().background(BackgroundLight).padding(padding)) {
            if (turns.isNotEmpty()) { CompactStatsCard(stats) }

            AnimatedVisibility(visible = showSummary && summaries.isNotEmpty()) {
                OwnersSummarySection(summaries = summaries)
            }

            if (turns.isNotEmpty()) {
                OutlinedTextField(
                    value = searchQuery, onValueChange = { searchQuery = it },
                    placeholder = { Text("جستجوی مالک...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp), singleLine = true
                )
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusChip("همه", "ALL", filterStatus) { filterStatus = it }
                    StatusChip("در انتظار", "PENDING", filterStatus) { filterStatus = it }
                    StatusChip("انجام‌شده", "COMPLETED", filterStatus) { filterStatus = it }
                }
            }

            if (turns.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp, top = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filteredTurns, key = { it.id }) { turn ->
                        TurnCard(
                            turn = turn,
                            // مدیر اصلی و موتوربان می‌توانند روی نوبت‌ها عملیات انجام دهند
                            onClick = { if (userRole == UserRole.MAIN_ADMIN || userRole == UserRole.OPERATOR) actionTurn = turn },
                            onToggle = { vm.toggleTurnStatus(turn) { } }
                        )
                    }
                }
            }
        }
    }

    actionTurn?.let { turn ->
        TurnActionsSheet(
            turn = turn,
            onDismiss = { actionTurn = null },
            onDelete = { turnToDelete = turn; actionTurn = null },
            onMove = { showMoveSheet = true }
        )
    }

    if (showGenerateDialog) {
        GenerateDialog(hasExisting = turns.isNotEmpty(), onDismiss = { showGenerateDialog = false }, onConfirm = { y, m, d -> vm.generateSchedule(wellId, y, m, d) { showGenerateDialog = false } })
    }

    if (showMoveSheet && actionTurn != null) {
        MoveTurnSheet(
            currentTurn = actionTurn!!,
            allTurns = turns,
            onDismiss = { showMoveSheet = false; actionTurn = null },
            onSelect = { targetTurn ->
                vm.swapTurns(actionTurn!!, targetTurn) {
                    showMoveSheet = false
                    actionTurn = null
                }
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = { Icon(Icons.Default.Warning, null, tint = AbyarRed) },
            title = { Text("پاک کردن کل برنامه", fontWeight = FontWeight.Bold) },
            text = { Text("آیا از پاک کردن ${turns.size} نوبت ثبت‌شده مطمئن هستید؟") },
            confirmButton = { Button(onClick = { vm.clearAllTurns(wellId) { showClearDialog = false } }, colors = ButtonDefaults.buttonColors(containerColor = AbyarRed)) { Text("پاک کن") } },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("انصراف") } }
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
private fun CompactStatsCard(stats: ScheduleStats) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(3.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CompactStatBox(Icons.Default.List, "کل", "${stats.totalTurns}", AbyarBlue, Modifier.weight(1f))
                CompactStatBox(Icons.Default.CheckCircle, "انجام", "${stats.completedCount}", AbyarGreen, Modifier.weight(1f))
                CompactStatBox(Icons.Default.Schedule, "در انتظار", "${stats.pendingCount}", AbyarOrange, Modifier.weight(1f))
            }
            if (stats.startDate != null && stats.endDate != null) {
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SmallInfoChip(Icons.Default.PlayArrow, "شروع: ${formatPersianDate(stats.startDate)}", AbyarBlue, Modifier.weight(1f))
                    SmallInfoChip(Icons.Default.Stop, "پایان: ${formatPersianDate(stats.endDate)}", AbyarRed, Modifier.weight(1f))
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SmallInfoChip(Icons.Default.Refresh, "مدار: ${stats.cycleDays} روز", AbyarPurple, Modifier.weight(1f))
                    SmallInfoChip(Icons.Default.Group, "شرکت‌کنندگان: ${stats.participantsCount} نفر", AbyarTeal, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CompactStatBox(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))) {
        Row(Modifier.padding(8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Column {
                Text(label, fontSize = 9.sp, color = TextSecondary)
                Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
            }
        }
    }
}

@Composable
private fun SmallInfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = color.copy(alpha = 0.08f), shape = RoundedCornerShape(8.dp)) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text(text, fontSize = 10.sp, color = color, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun OwnersSummarySection(summaries: List<OwnerScheduleSummary>) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5)), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PieChart, null, tint = AbyarPurple)
                Spacer(Modifier.width(8.dp))
                Text("خلاصه سهم مالکان در برنامه", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AbyarPurple)
            }
            Spacer(Modifier.height(4.dp))
            Text("مقایسه سهم ثبت‌شده با مجموع نوبت‌های هر مالک", fontSize = 11.sp, color = TextSecondary)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("مالک", Modifier.weight(1.4f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                Text("سهم", Modifier.weight(0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                Text("در برنامه", Modifier.weight(0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                Text("وضعیت", Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            }
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            summaries.forEach { s ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(s.userName, Modifier.weight(1.4f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text("${s.shareHours}", Modifier.weight(0.7f), fontSize = 12.sp, color = TextSecondary)
                    Text(String.format("%.1f", s.scheduledHours), Modifier.weight(0.8f), fontSize = 12.sp, color = AbyarBlue, fontWeight = FontWeight.Bold)
                    Box(Modifier.weight(1f)) {
                        if (s.isBalanced) {
                            Surface(color = CardGreenLight, shape = RoundedCornerShape(6.dp)) {
                                Row(Modifier.padding(horizontal = 6.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Check, null, tint = AbyarGreen, modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text("متوازن", fontSize = 10.sp, color = AbyarGreen, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else if (s.difference > 0) {
                            Surface(color = CardOrangeLight, shape = RoundedCornerShape(6.dp)) {
                                Text("بیشتر: +${String.format("%.1f", s.difference)}", modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), fontSize = 10.sp, color = AbyarOrange, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Surface(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(6.dp)) {
                                Text("کمتر: ${String.format("%.1f", s.difference)}", modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), fontSize = 10.sp, color = AbyarRed, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(label: String, value: String, current: String, onSelect: (String) -> Unit) {
    FilterChip(selected = current == value, onClick = { onSelect(value) }, label = { Text(label, fontSize = 12.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AbyarBlue, selectedLabelColor = Color.White))
}

@Composable
private fun TurnCard(turn: IrrigationTurn, onClick: () -> Unit, onToggle: () -> Unit) {
    val (statusLabel, statusColor, statusIcon) = when (turn.status) {
        "COMPLETED" -> Triple("انجام شد", AbyarGreen, Icons.Default.CheckCircle)
        "CANCELLED" -> Triple("لغو شده", Color.Gray, Icons.Default.Cancel)
        else -> Triple("در انتظار", AbyarOrange, Icons.Default.Schedule)
    }
    val durationMs = turn.endTime - turn.startTime
    val durationHours = durationMs / (1000.0 * 60.0 * 60.0)
    val durationText = if (durationHours < 1) "${(durationMs / (1000 * 60))} دقیقه" else String.format("%.1f ساعت", durationHours)

    Card(Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(4.dp, 44.dp).clip(RoundedCornerShape(2.dp)).background(statusColor))
                Spacer(Modifier.width(12.dp))
                Box(Modifier.size(40.dp).clip(CircleShape).background(AbyarBlue.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, null, tint = AbyarBlue, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(turn.userName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("از ${formatPersianDateTime(turn.startTime)}", fontSize = 11.sp, color = TextSecondary)
                    Text("تا ${formatPersianDateTime(turn.endTime)}", fontSize = 11.sp, color = TextSecondary)
                }
                Surface(color = AbyarBlue.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)) {
                    Text(durationText, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AbyarBlue)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(statusIcon, null, tint = statusColor, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(statusLabel, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onToggle) {
                    Icon(if (turn.status == "COMPLETED") Icons.Default.Undo else Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (turn.status == "COMPLETED") "برگردان" else "تایید", fontSize = 12.sp)
                }
                Icon(Icons.Default.MoreVert, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(120.dp).clip(CircleShape).background(AbyarBlue.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.CalendarMonth, null, tint = AbyarBlue, modifier = Modifier.size(64.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text("هنوز برنامه‌ای تولید نشده", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(6.dp))
            Text("با دکمه سبز پایین، نوبت‌بندی خودکار را انجام دهید", fontSize = 13.sp, color = TextSecondary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TurnActionsSheet(turn: IrrigationTurn, onDismiss: () -> Unit, onDelete: () -> Unit, onMove: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(20.dp).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(AbyarBlue.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, null, tint = AbyarBlue)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(turn.userName, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("${formatPersianDateTime(turn.startTime)} تا ${formatPersianDateTime(turn.endTime)}", fontSize = 11.sp, color = TextSecondary)
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            ActionItem(Icons.Default.SwapHoriz, AbyarOrange, "جابجایی نوبت", "انتقال این نوبت به زمان دیگر و شیفت دادن بقیه", onMove)
            ActionItem(Icons.Default.Delete, AbyarRed, "حذف و شیفت", "حذف این نوبت و شیفت دادن بقیه به عقب", onDelete)
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ActionItem(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, title: String, subtitle: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)), elevation = CardDefaults.cardElevation(0.dp)) {
        Row(Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = color)
                Text(subtitle, fontSize = 11.sp, color = TextSecondary)
            }
            Icon(Icons.Default.ChevronLeft, null, tint = color)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoveTurnSheet(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenerateDialog(hasExisting: Boolean, onDismiss: () -> Unit, onConfirm: (Int, Int, Int) -> Unit) {
    val today = remember { getTodayJalali() }
    var year by remember { mutableStateOf(today.first.toString()) }
    var month by remember { mutableStateOf(today.second.toString()) }
    var day by remember { mutableStateOf(today.third.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.AutoMode, null, tint = AbyarGreen) },
        title = { Text(if (hasExisting) "تولید مجدد برنامه" else "تولید برنامه آبیاری", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (hasExisting) {
                    Card(colors = CardDefaults.cardColors(containerColor = CardOrangeLight), shape = RoundedCornerShape(10.dp)) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = AbyarOrange, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("برنامه قبلی پاک و برنامه جدید ساخته می‌شود.", fontSize = 12.sp, color = AbyarOrange, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                Text("تاریخ شروع مدار را وارد کنید:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = day, onValueChange = { val c = toEnglishDigits(it); if (c.length <= 2 && c.all { ch -> ch.isDigit() }) day = c }, label = { Text("روز", fontSize = 11.sp) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = month, onValueChange = { val c = toEnglishDigits(it); if (c.length <= 2 && c.all { ch -> ch.isDigit() }) month = c }, label = { Text("ماه", fontSize = 11.sp) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = year, onValueChange = { val c = toEnglishDigits(it); if (c.length <= 4 && c.all { ch -> ch.isDigit() }) year = c }, label = { Text("سال", fontSize = 11.sp) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1.3f), singleLine = true, shape = RoundedCornerShape(12.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text("مثال: امروز = ${today.first}/${today.second}/${today.third}", fontSize = 11.sp, color = TextSecondary)
            }
        },
        confirmButton = { Button(onClick = { val y = year.toIntOrNull() ?: today.first; val m = month.toIntOrNull() ?: today.second; val d = day.toIntOrNull() ?: today.third; onConfirm(y, m, d) }, colors = ButtonDefaults.buttonColors(containerColor = AbyarGreen)) { Text("تولید برنامه") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
