package com.example.abyar.ui.farmer

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.abyar.data.IrrigationTurn
import com.example.abyar.data.Notification
import com.example.abyar.ui.theme.*
import com.example.abyar.util.formatPersianDate
import com.example.abyar.util.formatPersianTime

@Composable
fun FarmerDashboardScreen(
    userName: String,
    wellName: String,
    nextTurn: IrrigationTurn?,
    operatorName: String?,
    operatorPhone: String?,
    notifications: List<Notification>,
    onSwitchWell: () -> Unit,
    onEditProfile: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BackgroundLight),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ==================== هدر ====================
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .background(Brush.horizontalGradient(listOf(AbyarGreen, AbyarTeal)))
                    .padding(20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "سلام",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 14.sp
                            )
                            Text(
                                userName,
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Water,
                                    null,
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    wellName,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onSwitchWell,
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Default.SwapHoriz, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("تغییر چاه", fontSize = 13.sp)
                        }
                        OutlinedButton(
                            onClick = onEditProfile,
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Default.Person, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("پروفایل", fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // ==================== کارت نوبت بعدی ====================
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (nextTurn != null) Color.White else CardGreenLight
                ),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Column(
                    Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (nextTurn != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                null,
                                tint = AbyarBlue,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "نوبت بعدی شما",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = AbyarBlue
                            )
                        }
                        Spacer(Modifier.height(16.dp))

                        // تاریخ با فونت بزرگ
                        Text(
                            humanReadableTime(nextTurn.startTime),
                            fontSize = 44.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AbyarBlueDark,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(8.dp))

                        // ساعت با فونت بزرگ
                        Text(
                            "ساعت ${formatPersianTime(nextTurn.startTime)}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = AbyarOrange
                        )

                        Spacer(Modifier.height(6.dp))
                        Text(
                            "تا ساعت ${formatPersianTime(nextTurn.endTime)}",
                            fontSize = 16.sp,
                            color = TextSecondary
                        )

                        Spacer(Modifier.height(10.dp))
                        Surface(
                            color = CardBlueLight,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                formatPersianDate(nextTurn.startTime),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 12.sp,
                                color = AbyarBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Icon(
                            Icons.Default.CheckCircle,
                            null,
                            tint = AbyarGreen,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "نوبتی ثبت نشده",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = AbyarGreen
                        )
                        Text(
                            "نوبت بعدی شما وجود ندارد",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // ==================== کارت تماس با موتوربان ====================
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(Modifier.padding(18.dp).fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(AbyarPurple.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Engineering,
                                null,
                                tint = AbyarPurple,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "تماس با موتوربان",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                            Text(
                                operatorName ?: "موتوربانی ثبت نشده",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (operatorName != null) AbyarPurple else Color.Gray
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            val phone = operatorPhone
                            if (phone != null) {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                runCatching { context.startActivity(intent) }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AbyarGreen),
                        enabled = operatorPhone != null
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "تماس",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ==================== اطلاعیه‌ها ====================
        item {
            Row(
                Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Notifications,
                    null,
                    tint = AbyarOrange,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "اطلاعیه‌ها (${notifications.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (notifications.isEmpty()) {
            item {
                Card(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        Modifier.padding(20.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.NotificationsOff,
                            null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "اطلاعیه‌ای وجود ندارد",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            items(notifications.take(5), key = { it.id }) { notif ->
                Card(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(Modifier.padding(14.dp).fillMaxWidth()) {
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(CardOrangeLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Campaign,
                                null,
                                tint = AbyarOrange,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                notif.text,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(4.dp))
                            Row {
                                Text(
                                    "از: ${notif.senderName}",
                                    fontSize = 11.sp,
                                    color = AbyarBlue
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    formatPersianDate(notif.sentAt),
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * تبدیل زمان به متن خوانا (امروز / فردا / ۳ روز دیگر)
 */
private fun humanReadableTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diffMs = timestamp - now
    val diffDays = diffMs / (1000L * 60 * 60 * 24)
    return when {
        diffMs < 0 -> "گذشته"
        diffDays == 0L -> "امروز"
        diffDays == 1L -> "فردا"
        diffDays in 2..6 -> "$diffDays روز دیگر"
        else -> formatPersianDate(timestamp)
    }
}
