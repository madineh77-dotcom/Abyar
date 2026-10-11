package com.example.abyar.ui.groups

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
import com.example.abyar.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupManagementScreen(
    wellId: String,
    onBack: () -> Unit,
    vm: GroupViewModel = viewModel()
) {
    val groups by vm.groups.collectAsState()
    val ungrouped by vm.ungroupedUsers.collectAsState()
    val error by vm.error.collectAsState()
    val info by vm.info.collectAsState()
    val loading by vm.loading.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var showCreateDialog by remember { mutableStateOf(false) }
    var groupToDelete by remember { mutableStateOf<GroupWithDetails?>(null) }
    var expandedGroupId by remember { mutableStateOf<String?>(null) }
    var showAddMemberSheet by remember { mutableStateOf(false) }
    var targetGroupForAdd by remember { mutableStateOf<GroupWithDetails?>(null) }

    LaunchedEffect(wellId) { vm.loadGroups(wellId) }

    LaunchedEffect(error) {
        if (error.isNotEmpty()) {
            snackbarHostState.showSnackbar(error, duration = SnackbarDuration.Short)
            vm.clearMessages()
        }
    }
    LaunchedEffect(info) {
        if (info.isNotEmpty()) {
            snackbarHostState.showSnackbar(info, duration = SnackbarDuration.Short)
            vm.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("مدیریت گروه‌ها", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AbyarBlueDark)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = AbyarGreen,
                icon = { Icon(Icons.Default.GroupAdd, null, tint = Color.White) },
                text = { Text("گروه جدید", color = Color.White, fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AbyarBlue)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().background(BackgroundLight).padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // آمار
                item {
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(3.dp)
                    ) {
                        Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatBox(Icons.Default.Groups, "گروه‌ها", "${groups.size}", AbyarBlue, Modifier.weight(1f))
                            StatBox(Icons.Default.Person, "بدون گروه", "${ungrouped.size}", AbyarOrange, Modifier.weight(1f))
                        }
                    }
                }

                // لیست گروه‌ها
                if (groups.isEmpty()) {
                    item {
                        Card(
                            Modifier.fillMaxWidth().padding(vertical = 40.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBlueLight)
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Groups, null, tint = AbyarBlue, modifier = Modifier.size(72.dp))
                                Spacer(Modifier.height(12.dp))
                                Text("هنوز گروهی ساخته نشده", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AbyarBlueDark)
                                Text("با دکمه سبز پایین، اولین گروه را بسازید", fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                    }
                } else {
                    item {
                        Text("لیست گروه‌ها (${groups.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(vertical = 4.dp))
                    }
                    items(groups, key = { it.group.id }) { g ->
                        GroupCard(
                            details = g,
                            expanded = expandedGroupId == g.group.id,
                            onToggle = {
                                expandedGroupId = if (expandedGroupId == g.group.id) null else g.group.id
                                if (expandedGroupId == g.group.id) vm.loadGroupMembers(g.group.id)
                            },
                            onDelete = { groupToDelete = g },
                            onAddMember = {
                                targetGroupForAdd = g
                                showAddMemberSheet = true
                            }
                        )
                    }
                }

                // اعضای بدون گروه
                if (ungrouped.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(16.dp))
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardOrangeLight)
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.WarningAmber, null, tint = AbyarOrange)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "کاربران بدون گروه (${ungrouped.size})",
                                        fontWeight = FontWeight.Bold,
                                        color = AbyarOrange,
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                ungrouped.take(5).forEach { u ->
                                    Text("• ${u.fullName} (${u.shareHours} ساعت)", fontSize = 12.sp, color = TextSecondary)
                                }
                                if (ungrouped.size > 5) {
                                    Text("و ${ungrouped.size - 5} نفر دیگر...", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // دیالوگ ساخت گروه جدید
    if (showCreateDialog) {
        CreateGroupDialog(
            ungrouped = ungrouped,
            onDismiss = { showCreateDialog = false },
            onCreate = { name, leaderId ->
                vm.createGroup(wellId, name, leaderId) {
                    showCreateDialog = false
                    vm.loadGroups(wellId)
                }
            }
        )
    }

    // دیالوگ حذف گروه
    groupToDelete?.let { g ->
        AlertDialog(
            onDismissRequest = { groupToDelete = null },
            icon = { Icon(Icons.Default.Warning, null, tint = AbyarRed) },
            title = { Text("حذف گروه", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("آیا از حذف گروه «${g.group.name}» مطمئن هستید؟")
                    Spacer(Modifier.height(8.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = CardOrangeLight), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            "${g.membersCount} عضو این گروه به حالت «بدون گروه» برمی‌گردند.",
                            modifier = Modifier.padding(10.dp),
                            fontSize = 12.sp, color = AbyarOrange, fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.deleteGroup(g.group.id) {
                            groupToDelete = null
                            vm.loadGroups(wellId)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AbyarRed)
                ) { Text("حذف") }
            },
            dismissButton = {
                TextButton(onClick = { groupToDelete = null }) { Text("انصراف") }
            }
        )
    }

    // شیت افزودن عضو به گروه
    if (showAddMemberSheet && targetGroupForAdd != null) {
        AddMemberSheet(
            groupName = targetGroupForAdd!!.group.name,
            ungrouped = ungrouped,
            onDismiss = { showAddMemberSheet = false; targetGroupForAdd = null },
            onSelect = { user ->
                vm.addUserToGroup(user.id, targetGroupForAdd!!.group.id) {
                    showAddMemberSheet = false
                    targetGroupForAdd = null
                    vm.loadGroups(wellId)
                }
            }
        )
    }
}

@Composable
private fun StatBox(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, color = TextSecondary)
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun GroupCard(
    details: GroupWithDetails,
    expanded: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onAddMember: () -> Unit
) {
    val members by remember { mutableStateOf<List<User>>(emptyList()) }

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBlueLight),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable { onToggle() }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(AbyarBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Groups, null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(details.group.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AbyarBlueDark)
                    Text("سرگروه: ${details.leaderName}", fontSize = 12.sp, color = AbyarBlue)
                    Text(
                        "${details.membersCount} عضو • مجموع سهم: ${String.format("%.1f", details.totalShares)} ساعت",
                        fontSize = 11.sp, color = TextSecondary
                    )
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

            if (expanded) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    // لیست اعضا
                    var loadedMembers by remember { mutableStateOf<List<User>>(emptyList()) }
                    LaunchedEffect(details.group.id) {
                        val db = androidx.compose.ui.platform.LocalContext.current.let {
                            com.example.abyar.data.AppDatabase.getDatabase(it).userDao()
                        }
                        db.getUsersByGroup(details.group.id).collect { list ->
                            loadedMembers = list
                        }
                    }
                    loadedMembers.forEach { m ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(32.dp).clip(CircleShape).background(AbyarTeal.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, null, tint = AbyarTeal, modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(m.fullName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("${m.shareHours} ساعت", fontSize = 11.sp, color = AbyarTeal)
                            }
                            if (m.id == details.group.leaderId) {
                                Surface(color = AbyarBlue, shape = RoundedCornerShape(6.dp)) {
                                    Text("سرگروه", color = Color.White, fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onAddMember,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("افزودن عضو جدید", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateGroupDialog(
    ungrouped: List<User>,
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedLeaderId by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.GroupAdd, null, tint = AbyarGreen) },
        title = { Text("گروه جدید", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("نام گروه:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("مثلاً: گروه آقای احمدی") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(Modifier.height(14.dp))
                Text("انتخاب سرگروه:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))

                if (ungrouped.isEmpty()) {
                    Text(
                        "هیچ کاربر بدون گروهی برای انتخاب سرگروه وجود ندارد. ابتدا مالک اضافه کنید.",
                        fontSize = 12.sp, color = AbyarRed
                    )
                } else {
                    Card(
                        Modifier.fillMaxWidth().heightIn(max = 200.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = BackgroundLight)
                    ) {
                        LazyColumn(Modifier.padding(4.dp)) {
                            items(ungrouped) { user ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedLeaderId = user.id }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedLeaderId == user.id,
                                        onClick = { selectedLeaderId = user.id },
                                        colors = RadioButtonDefaults.colors(selectedColor = AbyarGreen)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(user.fullName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        Text("${user.shareHours} ساعت", fontSize = 11.sp, color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && selectedLeaderId != null) {
                        onCreate(name, selectedLeaderId!!)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AbyarGreen),
                enabled = name.isNotBlank() && selectedLeaderId != null
            ) { Text("ساخت گروه") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMemberSheet(
    groupName: String,
    ungrouped: List<User>,
    onDismiss: () -> Unit,
    onSelect: (User) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(20.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("افزودن عضو به «$groupName»", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AbyarBlueDark)
            Text("یک کاربر بدون گروه را انتخاب کنید:", fontSize = 12.sp, color = TextSecondary)

            if (ungrouped.isEmpty()) {
                Text("هیچ کاربر بدون گروهی وجود ندارد.", color = AbyarRed, fontSize = 13.sp)
            } else {
                ungrouped.forEach { u ->
                    Card(
                        Modifier.fillMaxWidth().clickable { onSelect(u) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(38.dp).clip(CircleShape).background(AbyarBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, null, tint = AbyarBlue, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(u.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("${u.shareHours} ساعت", fontSize = 11.sp, color = AbyarGreen)
                            }
                            Icon(Icons.Default.ChevronLeft, null, tint = AbyarBlue)
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}
