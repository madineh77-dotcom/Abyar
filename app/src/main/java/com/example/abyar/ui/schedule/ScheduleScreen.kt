package com.example.abyar.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import com.example.abyar.ui.theme.*
import com.example.abyar.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    wellId: String,
    onBack: () -> Unit,
    vm: ScheduleViewModel = viewModel()
) {
    val turns by vm.turns.collectAsState()
    val stats by vm.stats.collectAsState()
    val error by vm.error.collectAsState()
    val info by vm.info.collectAsState()

    var showGenerateDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var filterStatus by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(wellId) { vm.loadTurns(wellId) }

    val filteredTurns = remember(turns, filterStatus, searchQuery) {
        turns.filter { turn ->
            val matchesStatus = filterStatus == "ALL" || turn.status == filterStatus
            val matchesSearch = searchQuery.isBlank() ||
                    turn.userName.contains(searchQuery, ignoreCase = true)
            matchesStatus && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("برنامه آبیاری", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    if (turns.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, null, tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AbyarBlueDark)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showGenerateDialog = true },
                containerColor = AbyarGreen,
                icon = { Icon(Icons.Default.AutoMode, null, tint = Color.White) },
                text = {
                    Text(
                        if (turns.isEmpty()) "تولید برنامه" else "تولید مجدد",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(BackgroundLight)
                .padding(padding)
        ) {
            // آمار
            if (turns.isNotEmpty()) {
                StatsCard(stats)
            }

            // پیام خطا / اطلاع
            if (error.isNotEmpty()) {
                MessageCard(error, AbyarRed, Icons.Default.Error)
            }
            if (info.isNotEmpty()) {
                MessageCard(info, AbyarGreen, Icons.Default.CheckCircle)
            }

            // فیلتر و جستجو
            if (turns.isNotEmpty()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("جستجوی مالک...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusChip("همه", "ALL", filterStatus) { filterStatus = it }
                    StatusChip("در انتظار", "PENDING", filterStatus) { filterStatus = it }
                    StatusChip("انجام‌شده", "COMPLETED", filterStatus) { filterStatus = it }
                }
            }

            // لیست نوبت‌ها
            if (turns.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredTurns, key = { it.id }) { turn ->
                        TurnCard(
                            turn = turn,
                            onToggle = { vm.toggleTurnStatus(turn) { } },
                            onDelete = { vm.deleteTurn(turn) { } }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    // دیالوگ تولید برنامه
    if (showGenerateDialog) {
        GenerateDialog(
            hasExisting = turns.isNotEmpty(),
            onDismiss = { showGenerateDialog = false; vm.clearMessages() },
            onConfirm = { y, m, d ->
                vm.generateSchedule(wellId, y, m, d) { showGenerateDialog = false }
            }
        )
    }

    // دیالوگ پاک کردن
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = { Icon(Icons.Default.Warning, null, tint = AbyarRed) },
            title = { Text("پاک کردن کل برنامه", fontWeight = FontWeight.Bold) },
            text = { Text("آیا از پاک کردن ${turns.size} نوبت ثبت‌شده مطمئن هستید؟ این عملیات قابل بازگشت نیست.") },
            confirmButton = {
                Button(
                    onClick = {
                        vm.clearAllTurns(wellId) { showClearDialog = false }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AbyarRed)
                ) { Text("پاک کن") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("انصراف") }
            }
        )
    }
}

// ============ کامپوننت‌ها ============

@Composable
private fun StatsCard(stats: ScheduleStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Insights, null, tint = AbyarBlue)
                Spacer(Modifier.width(8.dp))
                Text("خلاصه برنامه", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBox("کل نوبت", "${stats.totalTurns}", AbyarBlue, Modifier.weight(1f))
                StatBox("انجام‌شده", "${stats.completedCount}", AbyarGreen, Modifier.weight(1f))
                StatBox("در انتظار", "${stats.pendingCount}", AbyarOrange, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(Modifier.height(10.dp))
            if (stats.startDate != null && stats.endDate != null) {
                InfoRow("شروع:", formatPersianDateTime(stats.startDate))
                InfoRow("پایان:", formatPersianDateTime(stats.endDate))
                InfoRow("مدت مدار:", "${stats.cycleDays} روز")
                InfoRow("تعداد شرکت‌کنندگان:", "${stats.participantsCount} نفر")
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, color: Color, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = TextSecondary)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = TextSecondary)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}

@Composable
private fun MessageCard(text: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun StatusChip(label: String, value: String, current: String, onSelect: (String) -> Unit) {
    FilterChip(
        selected = current == value,
        onClick = { onSelect(value) },
        label = { Text(label, fontSize = 12.sp) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = AbyarBlue,
            selectedLabelColor = Color.White
        )
    )
}

@Composable
private fun TurnCard(
    turn: IrrigationTurn,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val (statusLabel, statusColor, statusIcon) = when (turn.status) {
        "COMPLETED" -> Triple("انجام شد", AbyarGreen, Icons.Default.CheckCircle)
        "CANCELLED" -> Triple("لغو شده", Color.Gray, Icons.Default.Cancel)
        else -> Triple("در انتظار", AbyarOrange, Icons.Default.Schedule)
    }

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(4.dp, 40.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(statusColor)
                )
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AbyarBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, null, tint = AbyarBlue, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(turn.userName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        "از ${formatPersianDateTime(turn.startTime)}",
                        fontSize = 11.sp, color = TextSecondary
                    )
                    Text(
                        "تا ${formatPersianDateTime(turn.endTime)}",
                        fontSize = 11.sp, color = TextSecondary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, tint = AbyarRed, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(statusIcon, null, tint = statusColor, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(statusLabel, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onToggle) {
                    Icon(
                        if (turn.status == "COMPLETED") Icons.Default.Undo else Icons.Default.Check,
                        null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (turn.status == "COMPLETED") "برگردان" else "تایید",
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(AbyarBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CalendarMonth, null, tint = AbyarBlue, modifier = Modifier.size(64.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text("هنوز برنامه‌ای تولید نشده", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(6.dp))
            Text(
                "با دکمه سبز پایین، نوبت‌بندی خودکار را انجام دهید",
                fontSize = 13.sp, color = TextSecondary
            )
        }
    }
}

// ============ دیالوگ تولید ============

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenerateDialog(
    hasExisting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, Int) -> Unit
) {
    val today = remember { getTodayJalali() }
    var year by remember { mutableStateOf(today.first.toString()) }
    var month by remember { mutableStateOf(today.second.toString()) }
    var day by remember { mutableStateOf(today.third.toString()) }
    var showMonthPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.AutoMode, null, tint = AbyarGreen) },
        title = {
            Text(
                if (hasExisting) "تولید مجدد برنامه" else "تولید برنامه آبیاری",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                if (hasExisting) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardOrangeLight),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = AbyarOrange, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "برنامه قبلی پاک و برنامه جدید ساخته می‌شود.",
                                fontSize = 12.sp, color = AbyarOrange, fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                Text("تاریخ شروع مدار را وارد کنید:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = day,
                        onValueChange = { if (it.length <= 2) day = it },
                        label = { Text("روز", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = month,
                        onValueChange = { if (it.length <= 2) month = it },
                        label = { Text("ماه", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = year,
                        onValueChange = { if (it.length <= 4) year = it },
                        label = { Text("سال", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1.3f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "مثال: امروز = ${today.first}/${today.second}/${today.third}",
                    fontSize = 11.sp, color = TextSecondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val y = year.toIntOrNull() ?: today.first
                    val m = month.toIntOrNull() ?: today.second
                    val d = day.toIntOrNull() ?: today.third
                    onConfirm(y, m, d)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AbyarGreen)
            ) { Text("تولید برنامه") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}
