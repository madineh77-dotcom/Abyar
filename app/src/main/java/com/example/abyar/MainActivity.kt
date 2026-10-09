package com.example.abyar

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.abyar.ui.common.ComingSoonScreen
import com.example.abyar.ui.home.HomeScreen
import com.example.abyar.ui.onboarding.OnboardingScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("abyar_prefs", Context.MODE_PRIVATE)
        val isFirstLaunch = prefs.getBoolean("first_launch", true)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF5F7FA)) {
                    var showOnboarding by remember { mutableStateOf(isFirstLaunch) }
                    var currentScreen by remember { mutableStateOf("home") }
                    var screenTitle by remember { mutableStateOf("") }

                    when {
                        showOnboarding -> {
                            OnboardingScreen(onFinished = {
                                prefs.edit().putBoolean("first_launch", false).apply()
                                showOnboarding = false
                            })
                        }
                        currentScreen == "home" -> {
                            HomeScreen(
                                userName = "کاربر آبیار",
                                phone = "۰۹۱۲۳۴۵۶۷۸۹",
                                wellName = "چاه نمونه",
                                wellCode = "12345678",
                                nextTurnInfo = "۱۴۰۳/۰۷/۱۶ - ۱۴:۰۰",
                                onPanelClick = { route ->
                                    screenTitle = when (route) {
                                        "well" -> "ثبت چاه"
                                        "owners" -> "مدیریت مالکان"
                                        "schedule" -> "برنامه آبیاری"
                                        "finance" -> "مدیریت مالی"
                                        "operator" -> "پنل موتوربان"
                                        "abyar" -> "پنل آبیار"
                                        "notify" -> "اطلاع‌رسانی"
                                        "contacts" -> "دفترچه تلفن"
                                        "rules" -> "قوانین و ضوابط"
                                        "reports" -> "گزارش‌ها"
                                        "settings" -> "تنظیمات"
                                        "support" -> "پشتیبانی"
                                        else -> "پنل"
                                    }
                                    currentScreen = route
                                }
                            )
                        }
                        else -> {
                            ComingSoonScreen(
                                title = screenTitle,
                                onBack = { currentScreen = "home" }
                            )
                        }
                    }
                }
            }
        }
    }
}
