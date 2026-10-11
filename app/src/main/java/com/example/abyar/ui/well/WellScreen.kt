package com.example.abyar.ui.well

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.example.abyar.data.Well
import com.example.abyar.ui.theme.*
import com.example.abyar.util.toEnglishDigits

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WellScreen(
    onBack: () -> Unit,
    onWellSaved: (Well) -> Unit,
    vm: WellViewModel = viewModel()
) {
    val existingWell by vm.well.collectAsState()
    val error by vm.error.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

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
                title = { Text("ثبت و ویرایش چاه", color = Color.White, fontWeight = FontWeight.Bold) },
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
            when {
                // حالت نمایش (چاه ثبت شده ولی در حال ویرایش نیست)
                existingWell != null && !isEditing -> {
                    WellInfoCard(
                        well = existingWell!!,
                        onEdit = { isEditing = true }
                    )
                }

                // حالت فرم (چاه جدید یا ویرایش)
                else -> {
                    WellForm(
                        existingWell = existingWell,
                        onSaveNew = { name, loc, days ->
                            vm.saveWell(name, loc, days) { well ->
                                onWellSaved(well)
                            }
                        },
                        onUpdate = { name, loc, days ->
                            vm.updateWell(existingWell!!.id, name, loc, days) {
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

/**
 * کارت نمایش اطلاعات چاه (با دکمه ویرایش)
 */
@Composable
private fun WellInfoCard(well: Well, onEdit: () -> Unit) {
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
                    Text("اطلاعات زیر قابل ویرایش است", fontSize = 12.sp, color = TextSecondary)
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, null, tint = AbyarBlue)
                }
            }

            Spacer(Modifier.height(20.dp))
            InfoRow("نام چاه", well.name)
            InfoRow("کد چاه", well.code)
            InfoRow("موقعیت", well.location ?: "ثبت نشده")
            InfoRow("مدت مدار", "${well.cycleDurationDays} روز")
            InfoRow("کل ساعت آب", "${well.totalHours.toInt()} ساعت")

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

/**
 * فرم ثبت یا ویرایش چاه
 */
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

    // محاسبه خودکار کل ساعت
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

            // نمایش خودکار کل ساعت آب
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

            // توضیح فرمول
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBlueLight),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = AbyarBlue, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "کل ساعت آب = مدت مدار × 24 ساعت (شبانه‌روزی)",
                        fontSize = 11.sp,
                        color = AbyarBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

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

            if (!isEditing) {
                Text(
                    "با ثبت چاه، یک کد ۸ رقمی یکتا به صورت خودکار تولید می‌شود.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
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
