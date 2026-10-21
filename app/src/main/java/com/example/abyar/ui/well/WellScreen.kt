package com.example.abyar.ui.well

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.abyar.data.UserRole
import com.example.abyar.data.Well
import com.example.abyar.ui.theme.*
import com.example.abyar.util.toEnglishDigits

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WellScreen(
    userRole: UserRole?,
    onBack: () -> Unit,
    onWellSaved: (Well) -> Unit,
    vm: WellViewModel = viewModel()
) {
    val existingWell by vm.well.collectAsState()
    val error by vm.error.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val canEdit = userRole == UserRole.MAIN_ADMIN

    var isEditing by remember { mutableStateOf(false) }

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
                title = { Text("مشخصات چاه", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AbyarBlueDark)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundLight)
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // بنر حالت مشاهده
            if (!canEdit) {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardOrangeLight)
                ) {
                    Row(
                        Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Visibility, null, tint = AbyarOrange, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "شما در حالت مشاهده هستید. ویرایش مشخصات چاه فقط با مدیر اصلی است.",
                            fontSize = 12.sp,
                            color = AbyarOrange,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            when {
                existingWell != null && !isEditing -> {
                    WellInfoCard(
                        well = existingWell!!,
                        canEdit = canEdit,
                        onEdit = { isEditing = true }
                    )
                }

                existingWell == null && !canEdit -> {
                    // نباید به اینجا برسیم — محافظت
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                    ) {
                        Column(
                            Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Lock, null, tint = AbyarRed, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "دسترسی محدود",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = AbyarRed
                            )
                            Text(
                                "فقط مدیر اصلی می‌تواند چاه ثبت کند",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                else -> {
                    WellForm(
                        existingWell = existingWell,
                        onSaveNew = { name, loc, days ->
                            vm.saveWell(name, loc, days, userRole) { well ->
                                onWellSaved(well)
                            }
                        },
                        onUpdate = { name, loc, days ->
                            vm.updateWell(existingWell!!.id, name, loc, days, userRole) {
                                isEditing = false
                            }
                        },
                        onCancelEdit = {
                            isEditing = false
                            vm.clearError()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WellInfoCard(well: Well, canEdit: Boolean, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardGreenLight),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AbyarGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("چاه ثبت شده است", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = AbyarBlueDark)
                    Text(
                        if (canEdit) "اطلاعات زیر قابل ویرایش است" else "اطلاعات چاه",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                if (canEdit) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, null, tint = AbyarBlue)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            InfoRow("نام چاه", well.name)
            InfoRow("کد چاه", well.code)
            InfoRow("موقعیت", well.location ?: "ثبت نشده")
            InfoRow("مدت مدار", "${well.cycleDurationDays} روز")
            InfoRow("کل ساعت آب", "${well.totalHours.toInt()} ساعت")

            if (canEdit) {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AbyarBlue)
                ) {
                    Icon(Icons.Default.Edit, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("ویرایش اطلاعات چاه", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WellForm(
    existingWell: Well?,
    onSaveNew: (name: String, loc: String, days: Int) -> Unit,
    onUpdate: (name: String, loc: String, days: Int) -> Unit,
    onCancelEdit: () -> Unit
) {
    val isEditing = existingWell != null

    var name by remember(existingWell?.id) { mutableStateOf(existingWell?.name ?: "") }
    var location by remember(existingWell?.id) { mutableStateOf(existingWell?.location ?: "") }
    var cycleDays by remember(existingWell?.id) {
        mutableStateOf(existingWell?.cycleDurationDays?.toString() ?: "")
    }

    val cycleDaysInt = cycleDays.toIntOrNull() ?: 0
    val totalHours = cycleDaysInt * 24

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isEditing) CardOrangeLight else CardBlueLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isEditing) Icons.Default.Edit else Icons.Default.Water,
                        null,
                        tint = if (isEditing) AbyarOrange else AbyarBlue
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    if (isEditing) "ویرایش اطلاعات چاه" else "اطلاعات چاه را وارد کنید",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("نام چاه *") },
                leadingIcon = { Icon(Icons.Default.Water, null, tint = AbyarBlue) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("موقعیت مکانی (اختیاری)") },
                leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = AbyarTeal) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = cycleDays,
                onValueChange = { newValue ->
                    val converted = toEnglishDigits(newValue)
                    if (converted.length <= 3 && converted.all { it.isDigit() }) {
                        cycleDays = converted
                    }
                },
                label = { Text("مدت مدار (روز) *") },
                leadingIcon = { Icon(Icons.Default.CalendarMonth, null, tint = AbyarPurple) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (totalHours > 0) CardGreenLight else Color(0xFFF5F5F5)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(14.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        null,
                        tint = if (totalHours > 0) AbyarGreen else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "کل ساعت آب چاه (خودکار محاسبه می‌شود)",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Text(
                            if (totalHours > 0) "$totalHours ساعت" else "منتظر وارد کردن مدت مدار...",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (totalHours > 0) AbyarGreen else Color.Gray
                        )
                    }
                    Icon(Icons.Default.Calculate, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isEditing) {
                    OutlinedButton(
                        onClick = onCancelEdit,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("انصراف", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            val days = cycleDays.toIntOrNull() ?: 0
                            onUpdate(name, location, days)
                        },
                        modifier = Modifier.weight(1.5f).height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AbyarOrange),
                        enabled = name.isNotBlank() && cycleDaysInt > 0
                    ) {
                        Icon(Icons.Default.Save, null, tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("ذخیره تغییرات", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            val days = cycleDays.toIntOrNull() ?: 0
                            onSaveNew(name, location, days)
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AbyarGreen),
                        enabled = name.isNotBlank() && cycleDaysInt > 0
                    ) {
                        Icon(Icons.Default.Save, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("ثبت چاه و تولید کد یکتا", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = TextSecondary)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}
