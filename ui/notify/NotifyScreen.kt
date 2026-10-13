package com.example.abyar.ui.notify

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.abyar.data.User
import com.example.abyar.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotifyScreen(
    wellId: String,
    onBack: () -> Unit,
    vm: NotifyViewModel = viewModel()
) {
    val groups by vm.groups.collectAsState()
    val leaders by vm.leaders.collectAsState()
    val allUsers by vm.allUsers.collectAsState()
    val groupMembers by vm.selectedGroupMembers.collectAsState()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var recipientType by remember { mutableStateOf("LEADERS") }
    var selectedGroupId by remember { mutableStateOf<String?>(null) }
    var selectedPersonId by remember { mutableStateOf<String?>(null) }
    var sendToGroupLeaderOnly by remember { mutableStateOf(true) }
    var messageText by remember { mutableStateOf("") }

    LaunchedEffect(wellId) { vm.loadData(wellId) }
    LaunchedEffect(selectedGroupId) {
        val gid = selectedGroupId
        if (gid != null) vm.loadGroupMembers(gid)
    }

    val recipients: List<User> = remember(
        recipientType, selectedGroupId, selectedPersonId,
        sendToGroupLeaderOnly, leaders, groupMembers, allUsers, groups
    ) {
        when (recipientType) {
            "LEADERS" -> leaders
            "GROUP" -> {
                if (sendToGroupLeaderOnly) {
                    val g = groups.find { it.group.id == selectedGroupId }
                    if (g != null) leaders.filter { it.id == g.group.leaderId } else emptyList()
                } else {
                    groupMembers
                }
            }
            "PERSON" -> allUsers.filter { it.id == selectedPersonId }
            else -> emptyList()
        }
    }

    fun sendSms() {
        if (recipients.isEmpty()) {
            scope.launch { snackbarHostState.showSnackbar("گیرنده‌ای انتخاب نشده") }
            return
        }
        if (messageText.isBlank()) {
            scope.launch { snackbarHostState.showSnackbar("متن پیام خالی است") }
            return
        }
        val numbers = recipients.map { it.phone }.distinct()
        val uriStr = if (numbers.size == 1) "smsto:${numbers.first()}"
        else "smsto:${numbers.joinToString(",")}"
        try {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse(uriStr))
            intent.putExtra("sms_body", messageText)
            context.startActivity(intent)
        } catch (e: Exception) {
            scope.launch { snackbarHostState.showSnackbar("امکان باز کردن پیامک وجود ندارد") }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("اطلاع‌رسانی", color = Color.White, fontWeight = FontWeight.Bold) },
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
            Modifier
                .fillMaxSize()
                .background(BackgroundLight)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // انتخاب گیرنده
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("۱. گیرنده را انتخاب کنید", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = recipientType == "LEADERS",
                            onClick = { recipientType = "LEADERS" },
                            label = { Text("سرگروه‌ها (${leaders.size})", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AbyarBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = recipientType == "GROUP",
                            onClick = { recipientType = "GROUP" },
                            label = { Text("یک گروه", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AbyarBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = recipientType == "PERSON",
                            onClick = { recipientType = "PERSON" },
                            label = { Text("یک نفر", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AbyarBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                    }

                    if (recipientType == "GROUP") {
                        Text("گروه:", fontSize = 12.sp, color = TextSecondary)
                        if (groups.isEmpty()) {
                            Text(
                                "هیچ گروهی وجود ندارد. ابتدا گروه بسازید.",
                                color = AbyarOrange,
                                fontSize = 12.sp
                            )
                        } else {
                            LazyColumn(Modifier.heightIn(max = 180.dp)) {
                                items(groups) { g ->
                                    Row(
                                        Modifier.fillMaxWidth()
                                            .clickable { selectedGroupId = g.group.id }
                                            .padding(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selectedGroupId == g.group.id,
                                            onClick = { selectedGroupId = g.group.id }
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Column {
                                            Text(g.group.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Text("${g.membersCount} عضو", fontSize = 11.sp, color = TextSecondary)
                                        }
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = sendToGroupLeaderOnly,
                                    onCheckedChange = { sendToGroupLeaderOnly = it }
                                )
                                Text("فقط سرگروه (نه کل اعضا)", fontSize = 12.sp)
                            }
                        }
                    }

                    if (recipientType == "PERSON") {
                        Text("شخص:", fontSize = 12.sp, color = TextSecondary)
                        LazyColumn(Modifier.heightIn(max = 180.dp)) {
                            items(allUsers.filter { it.shareHours > 0 }) { u ->
                                Row(
                                    Modifier.fillMaxWidth()
                                        .clickable { selectedPersonId = u.id }
                                        .padding(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedPersonId == u.id,
                                        onClick = { selectedPersonId = u.id }
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(u.fullName, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // متن پیام
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("۲. متن پیام", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                    Text("الگوهای آماده:", fontSize = 12.sp, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("نوبت شما نزدیک است", "بدهی معوق", "قطعی برق").forEach { tpl ->
                            AssistChip(
                                onClick = { messageText = tpl },
                                label = { Text(tpl, fontSize = 10.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        placeholder = { Text("پیام خود را بنویسید...") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // پیش‌نمایش گیرندگان
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardBlueLight)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.People, null, tint = AbyarBlue, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "گیرندگان: ${recipients.size} نفر",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = AbyarBlue
                        )
                    }
                    if (recipients.isNotEmpty() && recipients.size <= 5) {
                        Spacer(Modifier.height(4.dp))
                        recipients.forEach { u ->
                            Text(
                                "• ${u.fullName}",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // دکمه ارسال
            Button(
                onClick = { sendSms() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AbyarGreen),
                enabled = recipients.isNotEmpty() && messageText.isNotBlank()
            ) {
                Icon(Icons.Default.Send, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("باز کردن پیامک گوشی", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                "توجه: اپلیکیشن پیامک گوشی باز می‌شود و متن از قبل پر شده است. فقط روی «ارسال» بزنید.",
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )
        }
    }
}
