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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.abyar.ui.common.ComingSoonScreen
import com.example.abyar.ui.home.HomeScreen
import com.example.abyar.ui.home.HomeViewModel
import com.example.abyar.ui.onboarding.OnboardingScreen
import com.example.abyar.ui.owners.OwnersScreen
import com.example.abyar.ui.profile.ProfileEditScreen
import com.example.abyar.ui.well.WellScreen
import com.example.abyar.ui.wells.MyWellsScreen
import com.example.abyar.util.UserPrefs
import com.example.abyar.util.formatPersianDateTime

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

                    var userName by remember { mutableStateOf(userPrefs.fullName) }
                    var userPhone by remember { mutableStateOf(userPrefs.phone) }
                    var userNationalCode by remember { mutableStateOf(userPrefs.nationalCode) }
                    var currentWellId by remember { mutableStateOf(userPrefs.currentWellId) }

                    val homeVm: HomeViewModel = viewModel()

                    // بارگذاری داده‌های چاه فعال
                    LaunchedEffect(currentWellId, userNationalCode) {
                        val wid = currentWellId
                        if (wid != null && userNationalCode.isNotBlank()) {
                            homeVm.loadWell(wid, userNationalCode)
                        } else {
                            homeVm.clear()
                        }
                    }

                    val well by homeVm.well.collectAsState()
                    val nextTurn by homeVm.nextTurn.collectAsState()
                    val currentUser by homeVm.currentUser.collectAsState()

                    val nextTurnInfo = remember(nextTurn) {
                        val nt = nextTurn
                        if (nt == null) "نوبتی ثبت نشده"
                        else "${formatPersianDateTime(nt.startTime)} تا ${formatPersianDateTime(nt.endTime)}"
                    }

                    when {
                        showOnboarding -> {
                            OnboardingScreen(onFinished = {
                                onboardingPrefs.edit().putBoolean("first_launch", false).apply()
                                showOnboarding = false
                            })
                        }

                        // اگر کد ملی ندارد → صفحه پروفایل
                        userNationalCode.isBlank() -> {
                            ProfileEditScreen(
                                initialName = userName,
                                initialPhone = userPhone,
                                initialNationalCode = "",
                                onSave = { name, phone, nc ->
                                    userPrefs.fullName = name
                                    userPrefs.phone = phone
                                    userPrefs.nationalCode = nc
                                    userName = name
                                    userPhone = phone
                                    userNationalCode = nc
                                    currentScreen = "wells"
                                },
                                onBack = { }
                            )
                        }

                        // اگر چاه فعال ندارد → صفحه چاه‌های من
                        currentWellId == null -> {
                            MyWellsScreen(
                                nationalCode = userNationalCode,
                                fullName = userName,
                                phone = userPhone,
                                currentWellId = null,
                                onBack = { currentScreen = "profile" },
                                onSelectWell = { wellId ->
                                    userPrefs.currentWellId = wellId
                                    currentWellId = wellId
                                    currentScreen = "home"
                                }
                            )
                        }

                        currentScreen == "home" -> {
                            HomeScreen(
                                userName = userName,
                                phone = userPhone,
                                wellName = well?.name ?: "",
                                wellCode = well?.code ?: "",
                                nextTurnInfo = nextTurnInfo,
                                userShareHours = currentUser?.shareHours ?: 0.0,
                                onEditProfile = { currentScreen = "profile" },
                                onSwitchWell = { currentScreen = "wells" },
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

                        currentScreen == "wells" -> {
                            MyWellsScreen(
                                nationalCode = userNationalCode,
                                fullName = userName,
                                phone = userPhone,
                                currentWellId = currentWellId,
                                onBack = { currentScreen = "home" },
                                onSelectWell = { wellId ->
                                    userPrefs.currentWellId = wellId
                                    currentWellId = wellId
                                    currentScreen = "home"
                                }
                            )
                        }

                        currentScreen == "well" -> {
                            WellScreen(
                                onBack = { currentScreen = "home" },
                                onWellSaved = { currentScreen = "home" }
                            )
                        }

                        currentScreen == "owners" -> {
                            val wid = currentWellId
                            if (wid != null) {
                                OwnersScreen(
                                    wellId = wid,
                                    onBack = { currentScreen = "home" }
                                )
                            } else {
                                ComingSoonScreen(
                                    title = "مدیریت مالکان",
                                    onBack = { currentScreen = "home" }
                                )
                            }
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
