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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.abyar.data.Well
import com.example.abyar.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WellScreen(
    onBack: () -> Unit,
    onWellSaved: (Well) -> Unit,
    vm: WellViewModel = viewModel()
) {
    val existingWell by vm.well.collectAsState()
    val error by vm.error.collectAsState()
    val saved by vm.saved.collectAsState()

    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var totalHours by remember { mutableStateOf("") }
    var cycleDays by remember { mutableStateOf("") }

    LaunchedEffect(saved) {
        if (saved) existingWell?.let { onWellSaved(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ثبت چاه", color = Color.White, fontWeight = FontWeight.Bold) },
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
            if (existingWell != null) {
                // چاه قبلاً ثبت شده
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
                            Column {
                                Text("چاه شما ثبت شده است", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = AbyarBlueDark)
                                Text("اطلاعات زیر قابل مشاهده است", fontSize = 13.sp, color = TextSecondary)
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        InfoRow("نام چاه", existingWell!!.name)
                        InfoRow("کد چاه", existingWell!!.code)
                        InfoRow("موقعیت", existingWell!!.location ?: "ثبت نشده")
                        InfoRow("کل ساعت آب", "${existingWell!!.totalHours} ساعت")
                        InfoRow("مدت مدار", "${existingWell!!.cycleDurationDays} روز")
                    }
                }
            } else {
                // فرم ثبت چاه
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
                                    .background(CardBlueLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Water, null, tint = AbyarBlue)
                            }
                            Spacer(Modifier.width(10.dp))
                            Text("اطلاعات چاه را وارد کنید", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("نام چاه *") },
                            leadingIcon = { Icon(Icons.Default.Water, null, tint = AbyarBlue) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AbyarBlue,
                                focusedLabelColor = AbyarBlue
                            )
                        )

                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            label = { Text("موقعیت مکانی (اختیاری)") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = AbyarTeal) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AbyarTeal,
                                focusedLabelColor = AbyarTeal
                            )
                        )

                        OutlinedTextField(
                            value = totalHours,
                            onValueChange = { totalHours = it },
                            label = { Text("کل ساعت آب چاه *") },
                            leadingIcon = { Icon(Icons.Default.Schedule, null, tint = AbyarOrange) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AbyarOrange,
                                focusedLabelColor = AbyarOrange
                            )
                        )

                        OutlinedTextField(
                            value = cycleDays,
                            onValueChange = { cycleDays = it },
                            label = { Text("مدت مدار (روز) *") },
                            leadingIcon = { Icon(Icons.Default.CalendarMonth, null, tint = AbyarPurple) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AbyarPurple,
                                focusedLabelColor = AbyarPurple
                            )
                        )
                    }
                }

                if (error.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, null, tint = AbyarRed)
                            Spacer(Modifier.width(10.dp))
                            Text(error, color = AbyarRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Button(
                    onClick = {
                        vm.saveWell(
                            name = name,
                            location = location,
                            totalHours = totalHours.toDoubleOrNull() ?: 0.0,
                            cycleDays = cycleDays.toIntOrNull() ?: 0
                        ) { well ->
                            onWellSaved(well)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AbyarGreen)
                ) {
                    Icon(Icons.Default.Save, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("ثبت چاه و تولید کد یکتا", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Text(
                    "با ثبت چاه، یک کد ۸ رقمی یکتا به صورت خودکار تولید می‌شود که مخصوص این چاه است و در سراسر اپلیکیشن با آن شناسایی می‌شود.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
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
