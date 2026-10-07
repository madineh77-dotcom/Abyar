package com.example.abyar.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.abyar.data.UserRole
import com.example.abyar.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminScreen(vm: AdminViewModel = viewModel()) {
    val well by vm.well.collectAsState()
    val owners by vm.owners.collectAsState()
    val turns by vm.turns.collectAsState()
    val transactions by vm.transactions.collectAsState()

    var wellName by remember { mutableStateOf("") }
    var totalShares by remember { mutableStateOf("") }
    var cycleDays by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var ownerPhone by remember { mutableStateOf("") }
    var ownerHours by remember { mutableStateOf("") }
    var ownerRole by remember { mutableStateOf(UserRole.SMALL_OWNER) }
    var txAmount by remember { mutableStateOf("") }
    var txCategory by remember { mutableStateOf("") }

    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BackgroundLight),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // هدر گرادیانت
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(AbyarBlueDark, AbyarTeal)
                        )
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Icon(
                        Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "پنل مدیر چاه",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "مدیریت هوشمند آب و هزینه‌ها",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }

        if (well == null) {
            item {
                ModernCard(
                    title = "تعریف چاه جدید",
                    icon = Icons.Default.Add,
                    iconColor = AbyarBlue
                ) {
                    ModernTextField(wellName, { wellName = it }, "نام چاه", Icons.Default.Water)
                    ModernTextField(totalShares, { totalShares = it }, "کل سهم‌ها", Icons.Default.Numbers, KeyboardType.Number)
                    ModernTextField(cycleDays, { cycleDays = it }, "مدت مدار (روز)", Icons.Default.CalendarMonth, KeyboardType.Number)
                    Spacer(Modifier.height(8.dp))
                    GradientButton(
                        text = "ثبت چاه",
                        onClick = {
                            if (wellName.isNotBlank()) {
                                vm.createWell(wellName, totalShares.toIntOrNull() ?: 100, cycleDays.toIntOrNull() ?: 10)
                            }
                        }
                    )
                }
            }
        } else {
            // کارت اطلاعات چاه
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBlueLight),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(AbyarBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Water, null, tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text("چاه ${well!!.name}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AbyarBlueDark)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "مدت مدار: ${well!!.cycleDurationDays} روز  •  ${owners.size} مالک",
                                fontSize = 13.sp, color = TextSecondary
                            )
                        }
                    }
                }
            }

            // فرم افزودن مالک
            item {
                ModernCard("افزودن مالک جدید", Icons.Default.PersonAdd, AbyarGreen) {
                    ModernTextField(ownerName, { ownerName = it }, "نام مالک", Icons.Default.Person)
                    ModernTextField(ownerPhone, { ownerPhone = it }, "شماره تماس", Icons.Default.Phone, KeyboardType.Phone)
                    ModernTextField(ownerHours, { ownerHours = it }, "سهم ساعتی (مثلاً 2.5)", Icons.Default.Schedule, KeyboardType.Decimal)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(UserRole.MAJOR_OWNER, UserRole.SMALL_OWNER).forEach { role ->
                            FilterChip(
                                selected = ownerRole == role,
                                onClick = { ownerRole = role },
                                label = { Text(if (role == UserRole.MAJOR_OWNER) "مالک عمده" else "خرده‌مالک", fontSize = 13.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AbyarGreen,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    GradientButton(
                        text = "افزودن مالک",
                        colors = listOf(AbyarGreen, AbyarTeal),
                        onClick = {
                            if (ownerName.isNotBlank() && ownerHours.isNotBlank()) {
                                vm.addOwner(ownerName, ownerPhone, ownerRole, ownerHours.toDoubleOrNull() ?: 1.0)
                                ownerName = ""; ownerPhone = ""; ownerHours = ""
                            }
                        }
                    )
                }
            }

            // لیست مالکان
            if (owners.isNotEmpty()) {
                item {
                    SectionTitle("لیست مالکان (${owners.size} نفر)", Icons.Default.Group, AbyarPurple)
                }
                items(owners) { owner ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            Modifier.padding(14.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (owner.role == UserRole.MAJOR_OWNER) CardBlueLight else CardGreenLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (owner.role == UserRole.MAJOR_OWNER) Icons.Default.Star else Icons.Default.Person,
                                    null,
                                    tint = if (owner.role == UserRole.MAJOR_OWNER) AbyarBlue else AbyarGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(owner.fullName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                Text(
                                    if (owner.role == UserRole.MAJOR_OWNER) "مالک عمده" else "خرده‌مالک",
                                    fontSize = 12.sp, color = TextSecondary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${owner.shareHours ?: 0.0} ساعت", fontWeight = FontWeight.Bold, color = AbyarBlue, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }

            // دکمه تولید نوبت
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Button(
                        onClick = { vm.generateTurns() },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AbyarGreen),
                        elevation = ButtonDefaults.buttonElevation(6.dp)
                    ) {
                        Icon(Icons.Default.AutoMode, null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("تولید خودکار نوبت‌بندی", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // نوبت‌ها
            if (turns.isNotEmpty()) {
                item {
                    SectionTitle("برنامه آبیاری (${turns.size} نوبت)", Icons.Default.Schedule, AbyarTeal)
                }
                items(turns) { turn ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            Modifier.padding(14.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp, 48.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(AbyarTeal)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(turn.userName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "${dateFormat.format(Date(turn.startTime))} تا ${dateFormat.format(Date(turn.endTime))}",
                                    fontSize = 12.sp, color = TextSecondary
                                )
                            }
                            Surface(
                                color = CardGreenLight,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    turn.status,
                                    color = AbyarGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // بخش مالی
            item {
                ModernCard("ثبت تراکنش مالی", Icons.Default.AccountBalanceWallet, AbyarOrange) {
                    ModernTextField(txCategory, { txCategory = it }, "دسته (برق، گازوئیل، فروش آب)", Icons.Default.Category)
                    ModernTextField(txAmount, { txAmount = it }, "مبلغ (تومان)", Icons.Default.Payments, KeyboardType.Number)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GradientButton(
                            text = "هزینه",
                            modifier = Modifier.weight(1f),
                            colors = listOf(AbyarRed, AbyarOrange),
                            onClick = {
                                if (txAmount.isNotBlank()) {
                                    vm.addTransaction("EXPENSE", txCategory.ifBlank { "هزینه" }, txAmount.toLongOrNull() ?: 0)
                                    txAmount = ""; txCategory = ""
                                }
                            }
                        )
                        GradientButton(
                            text = "درآمد",
                            modifier = Modifier.weight(1f),
                            colors = listOf(AbyarGreen, AbyarTeal),
                            onClick = {
                                if (txAmount.isNotBlank()) {
                                    vm.addTransaction("INCOME", txCategory.ifBlank { "درآمد" }, txAmount.toLongOrNull() ?: 0)
                                    txAmount = ""; txCategory = ""
                                }
                            }
                        )
                    }
                }
            }

            if (transactions.isNotEmpty()) {
                item { SectionTitle("تراکنش‌های اخیر", Icons.Default.History, AbyarOrange) }
                items(transactions) { tx ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            Modifier.padding(14.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (tx.type == "INCOME") CardGreenLight else CardOrangeLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (tx.type == "INCOME") Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                    null,
                                    tint = if (tx.type == "INCOME") AbyarGreen else AbyarRed
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(tx.category, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(if (tx.type == "INCOME") "درآمد" else "هزینه", fontSize = 12.sp, color = TextSecondary)
                            }
                            Text(
                                "${String.format("%,d", tx.amount)} ت",
                                color = if (tx.type == "INCOME") AbyarGreen else AbyarRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============ کامپوننت‌های قابل استفاده مجدد ============

@Composable
fun ModernCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextPrimary)
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 14.sp) },
        leadingIcon = { Icon(icon, null, tint = AbyarBlue) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AbyarBlue,
            focusedLabelColor = AbyarBlue,
            cursorColor = AbyarBlue
        )
    )
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(AbyarBlue, AbyarTeal)
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(colors = colors)),
        contentAlignment = Alignment.Center
    ) {
        TextButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SectionTitle(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}
