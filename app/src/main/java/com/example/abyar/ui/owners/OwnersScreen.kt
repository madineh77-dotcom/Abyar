package com.example.abyar.ui.owners
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.abyar.data.User
import com.example.abyar.data.WageType
import com.example.abyar.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddOwnerSheet(
    remainingHours: Double,
    onDismiss: () -> Unit,
    onSubmit: (
        name: String, phone: String, nc: String, share: Double,
        isOp: Boolean, opWt: WageType?, opWa: Double,
        isAb: Boolean, abWt: WageType?, abWa: Double
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var nc by remember { mutableStateOf("") }
    var share by remember { mutableStateOf("") }
    var isOp by remember { mutableStateOf(false) }
    var isAb by remember { mutableStateOf(false) }
    var opWageType by remember { mutableStateOf(WageType.CASH) }
    var opWageAmount by remember { mutableStateOf("") }
    var abWageType by remember { mutableStateOf(WageType.CASH) }
    var abWageAmount by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("افزودن مالک جدید", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AbyarBlueDark)
            Text(
                "باقیمانده ساعت قابل تخصیص: $remainingHours ساعت",
                fontSize = 13.sp,
                color = AbyarOrange,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("نام و نام خانوادگی *") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { newValue ->
                    val converted = com.example.abyar.util.toEnglishDigits(newValue)
                    if (converted.length <= 11 && converted.all { it.isDigit() || it == ' ' }) {
                        phone = converted
                    }
                },
                label = { Text("شماره تماس (11 رقمی، شروع با 0) *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = nc,
                onValueChange = { newValue ->
                    val converted = com.example.abyar.util.toEnglishDigits(newValue)
                    if (converted.length <= 10 && converted.all { it.isDigit() }) {
                        nc = converted
                    }
                },
                label = { Text("کد ملی (10 رقمی) *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = share,
                onValueChange = { share = com.example.abyar.util.toEnglishDigits(it) },
                label = { Text("سهم ساعتی (ساعت) *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isOp, onCheckedChange = { isOp = it })
                Text("این شخص موتوربان است", fontSize = 14.sp)
            }
            if (isOp) {
                WageSelector(
                    selected = opWageType,
                    onSelect = { opWageType = it },
                    amount = opWageAmount,
                    onAmountChange = { opWageAmount = com.example.abyar.util.toEnglishDigits(it) }
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isAb, onCheckedChange = { isAb = it })
                Text("این شخص آبیار است", fontSize = 14.sp)
            }
            if (isAb) {
                WageSelector(
                    selected = abWageType,
                    onSelect = { abWageType = it },
                    amount = abWageAmount,
                    onAmountChange = { abWageAmount = com.example.abyar.util.toEnglishDigits(it) }
                )
            }

            // نمایش خطای محلی
            if (localError.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, null, tint = AbyarRed, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(localError, color = AbyarRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val phoneError = com.example.abyar.util.phoneErrorMessage(phone)
                    val ncError = com.example.abyar.util.nationalCodeErrorMessage(nc)
                    val shareValue = share.toDoubleOrNull() ?: 0.0

                    when {
                        name.isBlank() -> localError = "نام و نام خانوادگی الزامی است"
                        phoneError != null -> localError = phoneError
                        ncError != null -> localError = ncError
                        shareValue <= 0.0 -> localError = "سهم ساعتی باید بیشتر از صفر باشد"
                        else -> {
                            localError = ""
                            onSubmit(
                                name.trim(), phone.trim(), nc.trim(),
                                shareValue,
                                isOp,
                                if (isOp) opWageType else null,
                                opWageAmount.toDoubleOrNull() ?: 0.0,
                                isAb,
                                if (isAb) abWageType else null,
                                abWageAmount.toDoubleOrNull() ?: 0.0
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AbyarGreen)
            ) {
                Icon(Icons.Default.Save, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("افزودن مالک", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// =============== کامپوننت‌ها ===============

@Composable
private fun StatsCard(stats: OwnersStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BarChart, null, tint = AbyarBlue)
                Spacer(Modifier.width(8.dp))
                Text("آمار چاه", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBox("کل ساعت", "${stats.totalHours}", AbyarBlue, Modifier.weight(1f))
                StatBox("تخصیص‌یافته", "${stats.allocatedHours}", AbyarGreen, Modifier.weight(1f))
                StatBox("باقیمانده", "${stats.remainingHours}", AbyarOrange, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = {
                    if (stats.totalHours > 0) (stats.allocatedHours / stats.totalHours).toFloat()
                    else 0f
                },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = AbyarGreen,
                trackColor = Color(0xFFE0E0E0)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "تعداد مالکان: ${stats.totalOwners}",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = TextSecondary)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    count: Int
) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color)
        Spacer(Modifier.width(8.dp))
        Text("$title ($count)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}

@Composable
private fun MajorOwnerCard(
    owner: User,
    memberCount: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onToggle() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBlueLight),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(
            Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AbyarBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Star, null, tint = Color.White, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(owner.fullName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AbyarBlueDark)
                Text("سهم: ${owner.shareHours} ساعت", fontSize = 12.sp, color = AbyarBlue)
                if (memberCount > 0) {
                    Text("$memberCount زیرمجموعه", fontSize = 11.sp, color = TextSecondary)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = AbyarRed)
            }
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                null,
                tint = AbyarBlue
            )
        }
    }
}

@Composable
private fun MemberItem(member: User, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(start = 32.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AbyarTeal.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = AbyarTeal, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(member.fullName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("${member.shareHours} ساعت", fontSize = 11.sp, color = AbyarTeal)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = AbyarRed, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun SmallOwnerCard(owner: User, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardGreenLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = AbyarGreen)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(owner.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(owner.phone, fontSize = 11.sp, color = TextSecondary)
                Text(
                    "${owner.shareHours} ساعت",
                    fontSize = 12.sp,
                    color = AbyarGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = AbyarRed)
            }
        }
    }
}

@Composable
private fun OperatorCard(owner: User, onDelete: () -> Unit) {
    val roleText = when {
        owner.isOperator && owner.isAbyar -> "موتوربان و آبیار"
        owner.isOperator -> "موتوربان"
        else -> "آبیار"
    }
    val wageText = when {
        owner.isOperator -> wageDescription(owner.operatorWageType, owner.operatorWageAmount)
        owner.isAbyar -> wageDescription(owner.abyarWageType, owner.abyarWageAmount)
        else -> ""
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AbyarPurple),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Engineering, null, tint = Color.White)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(owner.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(roleText, fontSize = 12.sp, color = AbyarPurple, fontWeight = FontWeight.SemiBold)
                if (wageText.isNotEmpty()) {
                    Text(wageText, fontSize = 11.sp, color = TextSecondary)
                }
                if (owner.shareHours > 0) {
                    Text("سهم آب: ${owner.shareHours} ساعت", fontSize = 11.sp, color = AbyarBlue)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = AbyarRed)
            }
        }
    }
}

private fun wageDescription(type: WageType?, amount: Double): String = when (type) {
    WageType.CASH -> "دستمزد نقدی: $amount تومان"
    WageType.FROM_TOTAL -> "کسر از کل چاه: $amount ساعت"
    WageType.FROM_OWN_SHARE -> "کسر از سهم خودش: $amount ساعت"
    null -> ""
}

// =============== شیت افزودن مالک ===============

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddOwnerSheet(
    remainingHours: Double,
    onDismiss: () -> Unit,
    onSubmit: (
        name: String, phone: String, nc: String, share: Double,
        isOp: Boolean, opWt: WageType?, opWa: Double,
        isAb: Boolean, abWt: WageType?, abWa: Double
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var nc by remember { mutableStateOf("") }
    var share by remember { mutableStateOf("") }
    var isOp by remember { mutableStateOf(false) }
    var isAb by remember { mutableStateOf(false) }
    var opWageType by remember { mutableStateOf(WageType.CASH) }
    var opWageAmount by remember { mutableStateOf("") }
    var abWageType by remember { mutableStateOf(WageType.CASH) }
    var abWageAmount by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("افزودن مالک جدید", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AbyarBlueDark)
            Text(
                "باقیمانده ساعت قابل تخصیص: $remainingHours ساعت",
                fontSize = 13.sp,
                color = AbyarOrange,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("نام و نام خانوادگی *") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("شماره تماس *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = nc,
                onValueChange = { newValue -> if (newValue.length <= 10) nc = newValue },
                label = { Text("کد ملی (۱۰ رقمی) *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = share,
                onValueChange = { share = it },
                label = { Text("سهم ساعتی (ساعت) *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isOp, onCheckedChange = { isOp = it })
                Text("این شخص موتوربان است", fontSize = 14.sp)
            }
            if (isOp) {
                WageSelector(
                    selected = opWageType,
                    onSelect = { opWageType = it },
                    amount = opWageAmount,
                    onAmountChange = { opWageAmount = it }
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isAb, onCheckedChange = { isAb = it })
                Text("این شخص آبیار است", fontSize = 14.sp)
            }
            if (isAb) {
                WageSelector(
                    selected = abWageType,
                    onSelect = { abWageType = it },
                    amount = abWageAmount,
                    onAmountChange = { abWageAmount = it }
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    onSubmit(
                        name, phone, nc,
                        share.toDoubleOrNull() ?: 0.0,
                        isOp,
                        if (isOp) opWageType else null,
                        opWageAmount.toDoubleOrNull() ?: 0.0,
                        isAb,
                        if (isAb) abWageType else null,
                        abWageAmount.toDoubleOrNull() ?: 0.0
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AbyarGreen)
            ) {
                Icon(Icons.Default.Save, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("افزودن مالک", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun WageSelector(
    selected: WageType,
    onSelect: (WageType) -> Unit,
    amount: String,
    onAmountChange: (String) -> Unit
) {
    Column(Modifier.padding(start = 32.dp, top = 4.dp, bottom = 4.dp)) {
        Text("نوع دستمزد:", fontSize = 12.sp, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            WageType.values().forEach { wt ->
                FilterChip(
                    selected = selected == wt,
                    onClick = { onSelect(wt) },
                    label = {
                        Text(
                            when (wt) {
                                WageType.CASH -> "نقدی"
                                WageType.FROM_TOTAL -> "از کل چاه"
                                WageType.FROM_OWN_SHARE -> "از سهم خودش"
                            },
                            fontSize = 11.sp
                        )
                    }
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = amount,
            onValueChange = onAmountChange,
            label = {
                Text(
                    if (selected == WageType.CASH) "مبلغ (تومان)" else "مقدار (ساعت)",
                    fontSize = 12.sp
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
    }
}
