package com.example.abyar.ui.wells

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.abyar.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyWellsScreen(
    nationalCode: String,
    fullName: String,
    phone: String,
    currentWellId: String?,
    onBack: () -> Unit,
    onSelectWell: (String) -> Unit,
    vm: MyWellsViewModel = viewModel()
) {
    val wells by vm.myWells.collectAsState()
    val error by vm.error.collectAsState()

    var showAddSheet by remember { mutableStateOf(false) }
    var showJoinSheet by remember { mutableStateOf(false) }

    LaunchedEffect(nationalCode) {
        vm.loadMyWells(nationalCode)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("چاه‌های من", color = Color.White, fontWeight = FontWeight.Bold) },
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
                Icon(Icons.Default.Add, null, tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundLight)
                .padding(padding)
                .padding(16.dp)
        ) {
            // دکمه پیوستن با کد
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showJoinSheet = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBlueLight),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AbyarBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Login, null, tint = Color.White)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("پیوستن به چاه موجود", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AbyarBlueDark)
                        Text("با کد ۸ رقمی چاه وارد شوید", fontSize = 12.sp, color = TextSecondary)
                    }
                    Icon(Icons.Default.ChevronLeft, null, tint = AbyarBlue)
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "چاه‌هایی که در آن‌ها عضو هستید (${wells.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(Modifier.height(8.dp))

            if (wells.isEmpty()) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Water, null, tint = Color.LightGray, modifier = Modifier.size(72.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("هنوز در هیچ چاهی عضو نیستید", fontSize = 16.sp, color = TextSecondary)
                        Text("با دکمه + یک چاه جدید بسازید یا با کد به چاه موجود بپیوندید", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(wells) { item ->
                        WellCard(
                            item = item,
                            isActive = item.well.id == currentWellId,
                            onClick = { onSelectWell(item.well.id) }
                        )
                    }
                }
            }
        }
    }

    // شیت افزودن چاه جدید
    if (showAddSheet) {
        AddNewWellSheet(
            nationalCode = nationalCode,
            fullName = fullName,
            phone = phone,
            error = error,
            onDismiss = { showAddSheet = false; vm.clearError() },
            onSubmit = { name, loc, hours, days, share, onDone ->
                vm.createNewWell(nationalCode, fullName, phone, name, loc, hours, days, share) { wellId ->
                    onDone()
                    showAddSheet = false
                    onSelectWell(wellId)
                }
            }
        )
    }

    // شیت پیوستن با کد
    if (showJoinSheet) {
        JoinWellSheet(
            error = error,
            onDismiss = { showJoinSheet = false; vm.clearError() },
            onSubmit = { code, share, onDone ->
                vm.joinWellByCode(nationalCode, fullName, phone, code, share) { wellId ->
                    onDone()
                    showJoinSheet = false
                    onSelectWell(wellId)
                }
            }
        )
    }
}

@Composable
private fun WellCard(item: WellWithShare, isActive: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) CardGreenLight else Color.White
        ),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isActive) AbyarGreen else AbyarBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Water, null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.well.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                Spacer(Modifier.height(2.dp))
                Text("کد: ${item.well.code}", fontSize = 12.sp, color = TextSecondary)
                Spacer(Modifier.height(2.dp))
                Text(
                    "سهم شما: ${item.shareHours} ساعت",
                    fontSize = 12.sp,
                    color = AbyarBlue,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (isActive) {
                Surface(color = AbyarGreen, shape = RoundedCornerShape(8.dp)) {
                    Text(
                        "فعال",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else {
                Icon(Icons.Default.ChevronLeft, null, tint = Color.Gray)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddNewWellSheet(
    nationalCode: String,
    fullName: String,
    phone: String,
    error: String,
    onDismiss: () -> Unit,
    onSubmit: (name: String, loc: String, hours: Double, days: Int, share: Double, onDone: () -> Unit) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("") }
    var days by remember { mutableStateOf("") }
    var myShare by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(20.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("افزودن چاه جدید", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AbyarBlueDark)
            Text("شما مدیر این چاه خواهید بود", fontSize = 13.sp, color = TextSecondary)

            OutlinedTextField(name, { name = it }, label = { Text("نام چاه *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(location, { location = it }, label = { Text("موقعیت مکانی") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                hours, { hours = it }, label = { Text("کل ساعت آب چاه *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                days, { days = it }, label = { Text("مدت مدار (روز) *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                myShare, { myShare = it }, label = { Text("سهم شما از این چاه (ساعت) *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            if (error.isNotEmpty()) {
                Text(error, color = AbyarRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    onSubmit(
                        name,
                        location,
                        hours.toDoubleOrNull() ?: 0.0,
                        days.toIntOrNull() ?: 0,
                        myShare.toDoubleOrNull() ?: 0.0
                    ) { }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AbyarGreen)
            ) {
                Icon(Icons.Default.Save, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("ایجاد چاه", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JoinWellSheet(
    error: String,
    onDismiss: () -> Unit,
    onSubmit: (code: String, share: Double, onDone: () -> Unit) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var share by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(20.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("پیوستن به چاه موجود", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AbyarBlueDark)
            Text("کد ۸ رقمی چاه را وارد کنید", fontSize = 13.sp, color = TextSecondary)

            OutlinedTextField(
                code, { if (it.length <= 8) code = it },
                label = { Text("کد چاه *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                share, { share = it },
                label = { Text("سهم شما (ساعت) *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            if (error.isNotEmpty()) {
                Text(error, color = AbyarRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    onSubmit(code, share.toDoubleOrNull() ?: 0.0) { }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AbyarBlue)
            ) {
                Icon(Icons.Default.Login, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("پیوستن", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}
