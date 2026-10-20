package com.example.abyar.ui.owners

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
import com.example.abyar.data.UserRole
import com.example.abyar.data.WageType
import com.example.abyar.ui.theme.*
import com.example.abyar.util.nationalCodeErrorMessage
import com.example.abyar.util.phoneErrorMessage
import com.example.abyar.util.toEnglishDigits

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnersScreen(
    wellId: String,
    onBack: () -> Unit,
    vm: OwnersViewModel = viewModel()
) {
    val stats by vm.stats.collectAsState()
    val grouped by vm.grouped.collectAsState()
    val error by vm.error.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var showAddSheet by remember { mutableStateOf(false) }
    var ownerToDelete by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(wellId) { vm.loadOwners(wellId) }

    LaunchedEffect(error) {
        if (error.isNotEmpty()) {
            snackbarHostState.showSnackbar(error, duration = SnackbarDuration.Short)
            vm.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("مدیریت مالکان", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AbyarBlueDark)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = AbyarGreen
            ) {
                Icon(Icons.Default.PersonAdd, null, tint = Color.White)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundLight)
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { StatsCard(stats) }

            // مدیر اصلی
            grouped.mainAdmin?.let { admin ->
                item {
                    SectionHeader("مدیر اصلی", Icons.Default.AdminPanelSettings, AbyarRed, 1)
                }
                item {
                    PersonCard(
                        user = admin,
                        accentColor = AbyarRed,
                        icon = Icons.Default.AdminPanelSettings,
                        onDelete = null
                    )
                }
            }

            // سرگروه‌ها
            if (grouped.groupLeaders.isNotEmpty()) {
                item {
                    SectionHeader("سرگروه‌ها", Icons.Default.Star, AbyarBlue, grouped.groupLeaders.size)
                }
                items(grouped.groupLeaders) { leader ->
                    PersonCard(
                        user = leader,
                        accentColor = AbyarBlue,
                        icon = Icons.Default.Star,
                        onDelete = { ownerToDelete = leader }
                    )
                }
            }

            // خرده‌مالکان
            if (grouped.smallOwners.isNotEmpty()) {
                item {
                    SectionHeader("خرده‌مالکان", Icons.Default.Person, AbyarTeal, grouped.smallOwners.size)
                }
                items(grouped.smallOwners) { owner ->
                    PersonCard(
                        user = owner,
                        accentColor = AbyarTeal,
                        icon = Icons.Default.Person,
                        onDelete = { ownerToDelete = owner }
                    )
                }
            }

            // موتوربان و آبیار
            if (grouped.operatorsAndAbyars.isNotEmpty()) {
                item {
                    SectionHeader(
                        "موتوربان و آبیار",
                        Icons.Default.Engineering,
                        AbyarPurple,
                        grouped.operatorsAndAbyars.size
                    )
                }
                items(grouped.operatorsAndAbyars) { op ->
                    PersonCard(
                        user = op,
                        accentColor = AbyarPurple,
                        icon = if (op.role == UserRole.OPERATOR) Icons.Default.Engineering else Icons.Default.Agriculture,
                        onDelete = { ownerToDelete = op }
                    )
                }
            }

            // اگر هیچ‌کس نبود
            if (grouped.mainAdmin == null &&
                grouped.groupLeaders.isEmpty() &&
                grouped.smallOwners.isEmpty() &&
                grouped.operatorsAndAbyars.isEmpty()
            ) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Group,
                                null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text("هنوز کاربری ثبت نشده", fontSize = 16.sp, color = TextSecondary)
                            Text("با دکمه سبز (+) کاربر جدید اضافه کنید", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        AddOwnerSheet(
            remainingHours = stats.remainingHours,
            hasMainAdmin = grouped.mainAdmin != null,
            hasOperator = grouped.operatorsAndAbyars.any { it.role == UserRole.OPERATOR },
            onDismiss = { showAddSheet = false; vm.clearError() },
            onSubmit = { name, phone, nc, share, role, opWt, opWa, abWt, abWa ->
                vm.addOwner(
                    wellId = wellId,
                    fullName = name,
                    phone = phone,
                    nationalCode = nc,
                    shareHours = share,
                    role = role,
                    operatorWageType = opWt,
                    operatorWageAmount = opWa,
                    abyarWageType = abWt,
                    abyarWageAmount = abWa
                ) {
                    showAddSheet = false
                    vm.loadOwners(wellId)
                }
            }
        )
    }

    ownerToDelete?.let { owner ->
        AlertDialog(
            onDismissRequest = { ownerToDelete = null },
            icon = { Icon(Icons.Default.Warning, null, tint = AbyarRed) },
            title = { Text("حذف کاربر", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("آیا از حذف «${owner.fullName}» مطمئن هستید؟")
                    Spacer(Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardOrangeLight),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "سهم ${owner.shareHours} ساعت ایشان به مجموع ساعت چاه بازمی‌گردد.",
                            modifier = Modifier.padding(10.dp),
                            fontSize = 13.sp,
                            color = AbyarOrange,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.deleteOwner(owner) {
                            ownerToDelete = null
                            vm.loadOwners(wellId)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AbyarRed)
                ) { Text("حذف") }
            },
            dismissButton = {
                TextButton(onClick = { ownerToDelete = null }) { Text("انصراف") }
            }
        )
    }
}

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
                StatBox("کل ساعت", "${stats.totalHours.toInt()}", AbyarBlue, Modifier.weight(1f))
                StatBox("تخصیص‌یافته", "${stats.allocatedHours.toInt()}", AbyarGreen, Modifier.weight(1f))
                StatBox("باقیمانده", "${stats.remainingHours.toInt()}", AbyarOrange, Modifier.weight(1f))
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
                "تعداد مالکان با سهم: ${stats.totalOwners}",
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
private fun PersonCard(
    user: User,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onDelete: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = accentColor, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(user.fullName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(user.phone, fontSize = 11.sp, color = TextSecondary)
                    Text(
                        user.roleLabel,
                        fontSize = 11.sp,
                        color = accentColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (user.shareHours > 0) {
                        Text(
                            "${user.shareHours} ساعت",
                            fontSize = 13.sp,
                            color = accentColor,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            "بدون سهم",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, null, tint = AbyarRed, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // نمایش دستمزد اگر موتوربان یا آبیار است
            if (user.role == UserRole.OPERATOR && user.operatorWageType != null) {
                Spacer(Modifier.height(6.dp))
                HorizontalDivider()
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Payments, null, tint = AbyarOrange, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "دستمزد: ${wageDescription(user.operatorWageType, user.operatorWageAmount)}",
                        fontSize = 11.sp,
                        color = AbyarOrange
                    )
                }
            }
            if (user.role == UserRole.ABYAR && user.abyarWageType != null) {
                Spacer(Modifier.height(6.dp))
                HorizontalDivider()
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Payments, null, tint = AbyarOrange, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "دستمزد: ${wageDescription(user.abyarWageType, user.abyarWageAmount)}",
                        fontSize = 11.sp,
                        color = AbyarOrange
                    )
                }
            }
        }
    }
}

private fun wageDescription(type: WageType?, amount: Double): String = when (type) {
    WageType.CASH -> "$amount تومان نقدی"
    WageType.FROM_TOTAL -> "$amount ساعت از کل چاه"
    WageType.FROM_OWN_SHARE -> "$amount ساعت از سهم خودش"
    null -> ""
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddOwnerSheet(
    remainingHours: Double,
    hasMainAdmin: Boolean,
    hasOperator: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (
        name: String, phone: String, nc: String, share: Double,
        role: UserRole,
        opWt: WageType?, opWa: Double,
        abWt: WageType?, abWa: Double
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var nc by remember { mutableStateOf("") }
    var share by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(UserRole.SMALL_OWNER) }
    var opWageType by remember { mutableStateOf(WageType.CASH) }
    var opWageAmount by remember { mutableStateOf("") }
    var abWageType by remember { mutableStateOf(WageType.CASH) }
    var abWageAmount by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf("") }

    val availableRoles = remember(hasMainAdmin, hasOperator) {
        val list = mutableListOf<UserRole>()
        if (!hasMainAdmin) list.add(UserRole.MAIN_ADMIN)
        list.add(UserRole.GROUP_LEADER)
        list.add(UserRole.SMALL_OWNER)
        if (!hasOperator) list.add(UserRole.OPERATOR)
        list.add(UserRole.ABYAR)
        list
    }

    // اگر نقش انتخاب شده در لیست نیست، به SMALL_OWNER تغییر کند
    LaunchedEffect(availableRoles) {
        if (role !in availableRoles && availableRoles.isNotEmpty()) {
            role = availableRoles.first()
        }
    }

    val shareRequired = role == UserRole.GROUP_LEADER || role == UserRole.SMALL_OWNER

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("افزودن کاربر جدید", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AbyarBlueDark)
            Text(
                "باقیمانده ساعت قابل تخصیص: ${remainingHours.toInt()} ساعت",
                fontSize = 13.sp,
                color = AbyarOrange,
                fontWeight = FontWeight.SemiBold
            )

            // فیلدهای اطلاعات
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("نام و نام خانوادگی *") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { newValue ->
                    val converted = toEnglishDigits(newValue)
                    if (converted.length <= 11 && converted.all { it.isDigit() }) {
                        phone = converted
                    }
                },
                label = { Text("شماره تماس (11 رقمی) *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            OutlinedTextField(
                value = nc,
                onValueChange = { newValue ->
                    val converted = toEnglishDigits(newValue)
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

            // انتخاب نقش
            Text("نقش کاربر:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = BackgroundLight)
            ) {
                Column(Modifier.padding(8.dp)) {
                    availableRoles.forEach { r ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable { role = r }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = role == r,
                                onClick = { role = r },
                                colors = RadioButtonDefaults.colors(selectedColor = AbyarBlue)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                when (r) {
                                    UserRole.MAIN_ADMIN -> "مدیر اصلی"
                                    UserRole.GROUP_LEADER -> "سرگروه (سهم بیشتر از ۵ ساعت)"
                                    UserRole.SMALL_OWNER -> "خرده‌مالک (سهم ۵ ساعت یا کمتر)"
                                    UserRole.OPERATOR -> "موتوربان"
                                    UserRole.ABYAR -> "آبیار"
                                },
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // فیلد سهم (فقط برای مالکان)
            if (shareRequired) {
                OutlinedTextField(
                    value = share,
                    onValueChange = { share = toEnglishDigits(it) },
                    label = { Text("سهم ساعتی (ساعت) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            } else {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = CardOrangeLight)
                ) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = AbyarOrange, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "این نقش نیازی به سهم مالکیت ندارد",
                            fontSize = 11.sp,
                            color = AbyarOrange,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // دستمزد موتوربان
            if (role == UserRole.OPERATOR) {
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Text("دستمزد موتوربان:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                WageSelector(
                    selected = opWageType,
                    onSelect = { opWageType = it },
                    amount = opWageAmount,
                    onAmountChange = { opWageAmount = toEnglishDigits(it) }
                )
            }

            // دستمزد آبیار
            if (role == UserRole.ABYAR) {
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Text("دستمزد آبیار:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                WageSelector(
                    selected = abWageType,
                    onSelect = { abWageType = it },
                    amount = abWageAmount,
                    onAmountChange = { abWageAmount = toEnglishDigits(it) }
                )
            }

            // خطا
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
                    val phoneError = phoneErrorMessage(phone)
                    val ncError = nationalCodeErrorMessage(nc)
                    val shareValue = share.toDoubleOrNull() ?: 0.0

                    when {
                        name.isBlank() -> localError = "نام و نام خانوادگی الزامی است"
                        phoneError != null -> localError = phoneError
                        ncError != null -> localError = ncError
                        shareRequired && shareValue <= 0.0 -> localError = "سهم ساعتی باید بیشتر از صفر باشد"
                        role == UserRole.GROUP_LEADER && shareValue <= 5.0 -> localError = "سرگروه باید سهم بیشتر از ۵ ساعت داشته باشد"
                        role == UserRole.SMALL_OWNER && shareValue > 5.0 -> localError = "خرده‌مالک باید سهم ۵ ساعت یا کمتر داشته باشد"
                        else -> {
                            localError = ""
                            onSubmit(
                                name.trim(), phone.trim(), nc.trim(),
                                shareValue,
                                role,
                                if (role == UserRole.OPERATOR) opWageType else null,
                                if (role == UserRole.OPERATOR) (opWageAmount.toDoubleOrNull() ?: 0.0) else 0.0,
                                if (role == UserRole.ABYAR) abWageType else null,
                                if (role == UserRole.ABYAR) (abWageAmount.toDoubleOrNull() ?: 0.0) else 0.0
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
                Text("افزودن کاربر", fontWeight = FontWeight.Bold)
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
    Column(Modifier.padding(top = 4.dp, bottom = 4.dp)) {
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
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
    }
}
