package com.example.abyar.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.example.abyar.ui.theme.*
import com.example.abyar.util.nationalCodeErrorMessage
import com.example.abyar.util.phoneErrorMessage
import com.example.abyar.util.toEnglishDigits

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    initialName: String,
    initialPhone: String,
    initialNationalCode: String,
    onSave: (name: String, phone: String, nationalCode: String) -> Unit,
    onBack: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf(initialPhone) }
    var nationalCode by remember { mutableStateOf(initialNationalCode) }
    var showError by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ویرایش پروفایل", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                        }
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Brush.linearGradient(listOf(AbyarBlue, AbyarTeal))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(60.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "اطلاعات شخصی خود را وارد کنید",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("نام و نام خانوادگی") },
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = AbyarBlue) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AbyarBlue,
                            focusedLabelColor = AbyarBlue
                        )
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { newValue ->
                            // تبدیل اعداد فارسی به انگلیسی + محدودیت 11 رقم
                            val converted = toEnglishDigits(newValue)
                            if (converted.length <= 11 && converted.all { it.isDigit() || it == ' ' }) {
                                phone = converted
                            }
                        },
                        label = { Text("شماره تماس (11 رقمی، شروع با 0)") },
                        leadingIcon = { Icon(Icons.Default.Phone, null, tint = AbyarGreen) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AbyarGreen,
                            focusedLabelColor = AbyarGreen
                        )
                    )

                    OutlinedTextField(
                        value = nationalCode,
                        onValueChange = { newValue ->
                            val converted = toEnglishDigits(newValue)
                            if (converted.length <= 10 && converted.all { it.isDigit() }) {
                                nationalCode = converted
                            }
                        },
                        label = { Text("کد ملی (10 رقمی)") },
                        leadingIcon = { Icon(Icons.Default.Badge, null, tint = AbyarOrange) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AbyarOrange,
                            focusedLabelColor = AbyarOrange
                        )
                    )

                    if (showError.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Error, null, tint = AbyarRed, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(showError, color = AbyarRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val phoneError = phoneErrorMessage(phone)
                    val ncError = nationalCodeErrorMessage(nationalCode)
                    when {
                        name.isBlank() -> showError = "نام و نام خانوادگی الزامی است"
                        phoneError != null -> showError = phoneError
                        ncError != null -> showError = ncError
                        else -> {
                            showError = ""
                            onSave(name.trim(), phone.trim(), nationalCode.trim())
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AbyarBlue)
            ) {
                Icon(Icons.Default.Save, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("ذخیره اطلاعات", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
