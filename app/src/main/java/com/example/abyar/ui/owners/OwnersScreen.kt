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

    var showFormSheet by remember { mutableStateOf(false) }
    var userToEdit by remember { mutableStateOf<User?>(null) }
    var userToDelete by remember { mutableStateOf<User?>(null) }

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
                title = { Text("مدیریت کاربران", color = Color.White, fontWeight = FontWeight.Bold) },
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
                onClick = { userToEdit = null; showFormSheet = true },
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

            grouped.mainAdmin?.let { admin ->
                item { SectionHeader("مدیر اصلی", Icons.Default.AdminPanelSettings, AbyarRed, 1) }
                item {
                    PersonCard(
                        user = admin, accentColor = AbyarRed, icon = Icons.Default.AdminPanelSettings,
                        onEdit = null, onDelete = null
                    )
                }
            }

            if (grouped.majorOwners.isNotEmpty()) {
                item { SectionHeader("مالکان عمده", Icons.Default.Star, AbyarBlue, grouped.majorOwners.size) }
                items(grouped.majorOwners) { owner ->
                    PersonCard(
                        user = owner, accentColor = AbyarBlue, icon = Icons.Default.Star,
                        onEdit = { userToEdit = owner; showFormSheet = true }, onDelete = { userToDelete = owner }
                    )
                }
            }

            if (grouped.smallOwners.isNotEmpty()) {
                item { SectionHeader("خرده‌مالکان", Icons.Default.Person, AbyarTeal, grouped.smallOwners.size) }
                items(grouped.smallOwners) { owner ->
                    PersonCard(
                        user = owner, accentColor = AbyarTeal, icon = Icons.Default.Person,
                        onEdit = { userToEdit = owner; showFormSheet = true }, onDelete = { userToDelete = owner }
                    )
                }
            }

            if (grouped.operators.isNotEmpty()) {
                item { SectionHeader("موتوربان", Icons.Default.Engineering, AbyarPurple, grouped.operators.size) }
                items(grouped.operators) { op ->
                    PersonCard(
                        user = op, accentColor = AbyarPurple, icon = Icons.Default.Engineering,
                        onEdit = { userToEdit = op; showFormSheet = true }, onDelete = { userToDelete = op }
                    )
                }
            }

            if (grouped.mainAdmin == null && grouped.majorOwners.isEmpty() && grouped.smallOwners.isEmpty() && grouped.operators.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Group, null, tint = Color.LightGray, modifier = Modifier.size(72.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("هنوز کاربری ثبت نشده", fontSize = 16.sp, color = TextSecondary)
                            Text("با دکمه سبز (+) کاربر جدید اضافه کنید", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }

    if (showFormSheet) {
        UserFormSheet(
            existingUser = userToEdit,
            remainingHours = stats.remainingHours,
            availableLeaders = grouped.availableLeaders, // لیست سرگروه‌ها
            onDismiss = { showFormSheet = false; userToEdit = null; vm.clearError() },
            onSubmit = { name, phone, nc, share, role, isLeader, grpId, opWt, opWa, incWage ->
                vm.saveUser(
                    wellId = wellId,
                    existingUser = userToEdit,
                    fullName = name,
                    phone = phone,
                    nationalCode = nc,
                    shareHours = share,
                    role = role,
                    isGroupLeader = isLeader,
                    groupId = grpId,
                    operatorWageType = opWt,
                    operatorWageAmount = opWa,
                    includeWageInSchedule = incWage
                ) {
                    showFormSheet = false
                    userToEdit = null
                    vm.loadOwners(wellId)
                }
            }
        )
    }

    userToDelete?.let { user ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            icon = { Icon(Icons.Default.Warning, null, tint = AbyarRed) },
            title = { Text("حذف کاربر", fontWeight = FontWeight.Bold) },
            text = { Text("آیا از حذف «${user.fullName}» مطمئن هستید؟ سهم ایشان به مجموع ساعت چاه بازمی‌گردد.") },
            confirmButton = {
                Button(
                    onClick = { vm.deleteOwner(user) { userToDelete = null; vm.loadOwners(wellId) } },
                    colors = ButtonDefaults.buttonColors(containerColor = AbyarRed)
                ) { Text("حذف") }
            },
            dismissButton = { TextButton(onClick = { userToDelete = null }) { Text("انصراف") } }
        )
    }
}

// (بقیه کامپوننت‌های OwnersScreen مثل StatsCard, SectionHeader, PersonCard بدون تغییر)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserFormSheet(
    existingUser: User?,
    remainingHours: Double,
    availableLeaders: List<User>,
    onDismiss: () -> Unit,
    onSubmit: (
        name: String, phone: String, nc: String, share: Double,
        role: UserRole, isLeader: Boolean, groupId: String?,
        opWt: WageType?, opWa: Double, incWage: Boolean
    ) -> Unit
) {
    var name by remember { mutableStateOf(existingUser?.fullName ?: "") }
    var phone by remember { mutableStateOf(existingUser?.phone ?: "") }
    var nc by remember { mutableStateOf(existingUser?.nationalCode ?: "") }
    var share by remember { mutableStateOf(existingUser?.shareHours?.toString() ?: "") }
    var role by remember { mutableStateOf(existingUser?.role ?: UserRole.SMALL_OWNER) }
    var isLeader by remember { mutableStateOf(existingUser?.isGroupLeader ?: false) }
    var selectedGroupId by remember { mutableStateOf(existingUser?.groupId) } // گروه انتخاب شده
    var opWageType by remember { mutableStateOf(existingUser?.operatorWageType ?: WageType.CASH) }
    var opWageAmount by remember { mutableStateOf(existingUser?.operatorWageAmount?.toString() ?: "") }
    var includeWage by remember { mutableStateOf(existingUser?.includeWageInSchedule ?: true) }
    var localError by remember { mutableStateOf("") }

    val isEditing = existingUser != null

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(20.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(if (isEditing) "ویرایش کاربر" else "افزودن کاربر جدید", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AbyarBlueDark)
            Text("باقیمانده ساعت قابل تخصیص: ${remainingHours.toInt()} ساعت", fontSize = 13.sp, color = AbyarOrange, fontWeight = FontWeight.SemiBold)

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("نام و نام خانوادگی *") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
            OutlinedTextField(value = phone, onValueChange = { val c = toEnglishDigits(it); if (c.length <= 11 && c.all { ch -> ch.isDigit() }) phone = c }, label = { Text("شماره تماس (11 رقمی) *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
            OutlinedTextField(value = nc, onValueChange = { val c = toEnglishDigits(it); if (c.length <= 10 && c.all { ch -> ch.isDigit() }) nc = c }, label = { Text("کد ملی (10 رقمی) *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)

            Text("نقش مالکیت:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = BackgroundLight)) {
                Column(Modifier.padding(8.dp)) {
                    if (!isEditing) {
                        Row(Modifier.fillMaxWidth().clickable { role = UserRole.MAIN_ADMIN }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = role == UserRole.MAIN_ADMIN, onClick = { role = UserRole.MAIN_ADMIN })
                            Spacer(Modifier.width(4.dp))
                            Text("مدیر اصلی", fontSize = 13.sp)
                        }
                    }
                    Row(Modifier.fillMaxWidth().clickable { role = UserRole.MAJOR_OWNER }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = role == UserRole.MAJOR_OWNER, onClick = { role = UserRole.MAJOR_OWNER })
                        Spacer(Modifier.width(4.dp))
                        Text("مالک عمده", fontSize = 13.sp)
                    }
                    Row(Modifier.fillMaxWidth().clickable { role = UserRole.SMALL_OWNER }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = role == UserRole.SMALL_OWNER, onClick = { role = UserRole.SMALL_OWNER })
                        Spacer(Modifier.width(4.dp))
                        Text("خرده‌مالک", fontSize = 13.sp)
                    }
                    Row(Modifier.fillMaxWidth().clickable { role = UserRole.OPERATOR }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = role == UserRole.OPERATOR, onClick = { role = UserRole.OPERATOR })
                        Spacer(Modifier.width(4.dp))
                        Text("موتوربان", fontSize = 13.sp)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isLeader, onCheckedChange = { isLeader = it })
                Text("این شخص سرگروه است", fontSize = 13.sp)
            }

            // اگر سرگروه نیست، انتخاب کند زیر نظر کدام سرگروه باشد
            if (!isLeader && availableLeaders.isNotEmpty()) {
                Text("اتصال به گروه (سرگروه):", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                var expanded by remember { mutableStateOf(false) }
                val selectedLeaderName = availableLeaders.find { it.groupId == selectedGroupId }?.fullName ?: "بدون گروه"

                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(selectedLeaderName, fontSize = 13.sp)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text("بدون گروه") },
                            onClick = { selectedGroupId = null; expanded = false }
                        )
                        availableLeaders.forEach { leader ->
                            DropdownMenuItem(
                                text = { Text(leader.fullName) },
                                onClick = { selectedGroupId = leader.id; expanded = false }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(value = share, onValueChange = { share = toEnglishDigits(it) }, label = { Text("سهم ساعتی (ساعت) *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)

            if (role == UserRole.OPERATOR) {
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Text("دستمزد موتوربان:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                WageSelector(
                    selected = opWageType, onSelect = { opWageType = it },
                    amount = opWageAmount, onAmountChange = { opWageAmount = toEnglishDigits(it) }
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = includeWage, onCheckedChange = { includeWage = it })
                    Text("دستمزد در برنامه آبیاری لحاظ شود", fontSize = 13.sp)
                }
            }

            if (localError.isNotEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
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
                        name.isBlank() -> localError = "نام الزامی است"
                        phoneError != null -> localError = phoneError
                        ncError != null -> localError = ncError
                        shareValue < 0 -> localError = "سهم نمی‌تواند منفی باشد"
                        else -> {
                            localError = ""
                            // آیدی گروهی که انتخاب کرده، در واقع آیدی سرگروه است.
                            // ما باید آیدی گروه واقعی را پیدا کنیم.
                            val actualGroupId = availableLeaders.find { it.id == selectedGroupId }?.groupId
                            onSubmit(name.trim(), phone.trim(), nc.trim(), shareValue, role, isLeader, actualGroupId, if (role == UserRole.OPERATOR) opWageType else null, if (role == UserRole.OPERATOR) (opWageAmount.toDoubleOrNull() ?: 0.0) else 0.0, includeWage)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AbyarGreen)
            ) {
                Icon(Icons.Default.Save, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text(if (isEditing) "ذخیره تغییرات" else "افزودن کاربر", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun WageSelector(selected: WageType, onSelect: (WageType) -> Unit, amount: String, onAmountChange: (String) -> Unit) {
    Column(Modifier.padding(top = 4.dp, bottom = 4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            WageType.values().forEach { wt ->
                FilterChip(
                    selected = selected == wt, onClick = { onSelect(wt) },
                    label = {
                        Text(
                            when (wt) {
                                WageType.CASH -> "نقدی"
                                WageType.FROM_TOTAL -> "از کل چاه"
                                WageType.FROM_OWN_SHARE -> "از سهم خودش"
                            }, fontSize = 11.sp
                        )
                    }
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = amount, onValueChange = onAmountChange,
            label = { Text(if (selected == WageType.CASH) "مبلغ (تومان)" else "مقدار (ساعت)", fontSize = 12.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true
        )
    }
}
