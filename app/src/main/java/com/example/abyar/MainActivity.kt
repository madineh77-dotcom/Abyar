package com.example.abyar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.abyar.data.Well
import com.example.abyar.ui.common.ComingSoonScreen
import com.example.abyar.ui.home.HomeScreen
import com.example.abyar.ui.onboarding.OnboardingScreen
import com.example.abyar.ui.profile.ProfileEditScreen
import com.example.abyar.ui.well.WellScreen
import com.example.abyar.util.UserPrefs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val onboardingPrefs = getSharedPreferences("abyar_prefs", MODE_PRIVATE)
        val userPrefs = UserPrefs(this)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF5F7FA)) {
                    var showOnboarding by remember {
                        mutableStateOf(onboardingPrefs.getBoolean("first_launch", true))
                    }
                    var currentScreen by remember { mutableStateOf("home") }
                    var screenTitle by remember { mutableStateOf("") }

                    // اطلاعات کاربر از SharedPreferences
                    var userName by remember { mutableStateOf(userPrefs.fullName) }
                    var userPhone by remember { mutableStateOf(userPrefs.phone) }
                    var userNationalCode by remember { mutableStateOf(userPrefs.nationalCode) }

                    when {
                        showOnboarding -> {
                            OnboardingScreen(onFinished = {
                                onboardingPrefs.edit().putBoolean("first_launch", false).apply()
                                showOnboarding = false
                            })
                        }

                        currentScreen == "home" -> {
                            HomeScreen(
                                userName = userName,
                                phone = userPhone,
                                wellName = "چاه نمونه",
                                wellCode = "12345678",
                                nextTurnInfo = "۱۴۰۳/۰۷/۱۶ - ۱۴:۰۰",
                                onEditProfile = { currentScreen = "profile" },
                                onPanelClick = { route ->
                                    screenTitle = titleFor(route)
                                    currentScreen = route
                                }
                            )
                        }

                        currentScreen == "profile" -> {
                            ProfileEditScreen(
                                initialName = userName,
                                initialPhone = userPhone,
                                initialNationalCode = userNationalCode,
                                onSave = { name, phone, nc ->
                                    userPrefs.fullName = name
                                    userPrefs.phone = phone
                                    userPrefs.nationalCode = nc
                                    userName = name
                                    userPhone = phone
                                    userNationalCode = nc
                                    currentScreen = "home"
                                },
                                onBack = { currentScreen = "home" }
                            )
                        }

                        currentScreen == "well" -> {
                            WellScreen(
                                onBack = { currentScreen = "home" },
                                onWellSaved = { _: Well ->
                                    currentScreen = "home"
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

    private fun titleFor(route: String): String = when (route) {
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
}
