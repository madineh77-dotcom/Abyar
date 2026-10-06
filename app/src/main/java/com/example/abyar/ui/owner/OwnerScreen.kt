package com.example.abyar.ui.owner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.abyar.ui.admin.AdminViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun OwnerScreen(vm: AdminViewModel = viewModel()) {
    val turns by vm.turns.collectAsState()
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "نوبت‌های آبیاری من",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0D47A1),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (turns.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("هنوز نوبتی ثبت نشده است.", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(turns) { turn ->
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                        Column(Modifier.padding(16.dp)) {
                            Text("مالک: ${turn.userName}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(Modifier.height(4.dp))
                            Text("شروع: ${dateFormat.format(Date(turn.startTime))}")
                            Text("پایان: ${dateFormat.format(Date(turn.endTime))}")
                            Spacer(Modifier.height(4.dp))
                            Text("وضعیت: ${turn.status}", color = Color(0xFF1976D2), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
