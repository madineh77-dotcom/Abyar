package com.example.abyar.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.abyar.data.UserRole
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
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("پنل مدیر چاه", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1))
        }

        if (well == null) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("تعریف چاه جدید", fontWeight = FontWeight.Bold)
                        OutlinedTextField(wellName, { wellName = it }, label = { Text("نام چاه") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(totalShares, { totalShares = it }, label = { Text("کل سهم‌ها") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(cycleDays, { cycleDays = it }, label = { Text("مدت مدار (روز)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                        Button(onClick = {
                            if (wellName.isNotBlank()) {
                                vm.createWell(wellName, totalShares.toIntOrNull() ?: 100, cycleDays.toIntOrNull() ?: 10)
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("ثبت چاه") }
                    }
                }
            }
        } else {
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
                    Column(Modifier.padding(16.dp)) {
                        Text("چاه: ${well!!.name}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("مدت مدار: ${well!!.cycleDurationDays} روز | تعداد مالکان: ${owners.size}")
                    }
                }
            }

            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("افزودن مالک", fontWeight = FontWeight.Bold)
                        OutlinedTextField(ownerName, { ownerName = it }, label = { Text("نام مالک") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(ownerPhone, { ownerPhone = it }, label = { Text("شماره تماس") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(ownerHours, { ownerHours = it }, label = { Text("سهم ساعتی") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(UserRole.MAJOR_OWNER, UserRole.SMALL_OWNER).forEach { role ->
                                FilterChip(selected = ownerRole == role, onClick = { ownerRole = role }, label = { Text(if (role == UserRole.MAJOR_OWNER) "مالک عمده" else "خرده‌مالک") })
                            }
                        }
                        Button(onClick = {
                            if (ownerName.isNotBlank() && ownerHours.isNotBlank()) {
                                vm.addOwner(ownerName, ownerPhone, ownerRole, ownerHours.toDoubleOrNull() ?: 1.0)
                                ownerName = ""; ownerPhone = ""; ownerHours = ""
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("افزودن مالک") }
                    }
                }
            }

            item {
                Button(
                    onClick = { vm.generateTurns() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
                ) { Text("تولید خودکار نوبت‌بندی", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            }

            if (turns.isNotEmpty()) {
                item { Text("برنامه آبیاری:", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                items(turns) { turn ->
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9))) {
                        Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(turn.userName, fontWeight = FontWeight.Bold)
                                Text("${dateFormat.format(Date(turn.startTime))} تا ${dateFormat.format(Date(turn.endTime))}", fontSize = 12.sp)
                            }
                            Text(turn.status, color = Color(0xFF1976D2), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("ثبت تراکنش مالی", fontWeight = FontWeight.Bold)
                        OutlinedTextField(txCategory, { txCategory = it }, label = { Text("دسته (برق، گازوئیل، فروش آب)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(txAmount, { txAmount = it }, label = { Text("مبلغ (تومان)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                if (txAmount.isNotBlank()) {
                                    vm.addTransaction("EXPENSE", txCategory.ifBlank { "هزینه" }, txAmount.toLongOrNull() ?: 0)
                                    txAmount = ""; txCategory = ""
                                }
                            }) { Text("هزینه") }
                            Button(onClick = {
                                if (txAmount.isNotBlank()) {
                                    vm.addTransaction("INCOME", txCategory.ifBlank { "درآمد" }, txAmount.toLongOrNull() ?: 0)
                                    txAmount = ""; txCategory = ""
                                }
                            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))) { Text("درآمد") }
                        }
                    }
                }
            }

            if (transactions.isNotEmpty()) {
                item { Text("تراکنش‌های اخیر:", fontWeight = FontWeight.Bold) }
                items(transactions) { tx ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(tx.category, fontWeight = FontWeight.Bold)
                                Text(if (tx.type == "INCOME") "درآمد" else "هزینه", fontSize = 12.sp)
                            }
                            Text("${tx.amount} تومان", color = if (tx.type == "INCOME") Color(0xFF388E3C) else Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
