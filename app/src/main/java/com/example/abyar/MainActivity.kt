package com.example.abyar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.abyar.data.UserRole
import com.example.abyar.ui.common.ComingSoonScreen
import com.example.abyar.ui.farmer.FarmerDashboardScreen
import com.example.abyar.ui.home.HomeScreen
import com.example.abyar.ui.home.HomeViewModel
import com.example.abyar.ui.notify.NotifyScreen
import com.example.abyar.ui.onboarding.OnboardingScreen
import com.example.abyar.ui.operator.OperatorDashboardScreen
import com.example.abyar.ui.owners.OwnersScreen
import com.example.abyar.ui.profile.ProfileEditScreen
import com.example.abyar.ui.schedule.ScheduleScreen
import com.example.abyar.ui.well.WellScreen
import com.example.abyar.ui.wells.MyWellsScreen
import com.example.abyar.util.UserPrefs
import com.example.abyar.util.formatPersianDate
import com.example.abyar.util.formatPersianTime

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val onboardingPrefs = getSharedPreferences("abyar_prefs", MODE_PRIVATE)
        val userPrefs = UserPrefs(this)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF5F7FA)) {
                    var showOnboarding by rememberSaveable {
                        mutableStateOf(onboardingPrefs.getBoolean("first_launch", true))
                    }
                    var currentScreen by rememberSaveable { mutableStateOf("home") }
                    var screenTitle by rememberSaveable { mutableStateOf("") }

                    var userName by rememberSaveable { mutableStateOf(userPrefs.fullName) }
                    var userPhone by rememberSaveable { mutableStateOf(userPrefs.phone) }
                    var userNationalCode by rememberSaveable { mutableStateOf(userPrefs.nationalCode) }
                    var currentWellId by rememberSaveable { mutableStateOf(userPrefs.currentWellId) }

                    val homeVm: HomeViewModel = viewModel()

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
                    val operator by homeVm.operator.collectAsState()
                    val notifications by homeVm.notifications.collectAsState()
                    val wellNotFound by homeVm.wellNotFound.collectAsState()

                    LaunchedEffect(wellNotFound, currentWellId) {
                        if (wellNotFound && currentWellId != null) {
                            userPrefs.currentWellId = null
                            currentWellId = null
                        }
                    }

                    val startText = remember(nextTurn) {
                        val nt = nextTurn
                        if (nt == null) "" else "${formatPersianDate(nt.startTime)} - ${formatPersianTime(nt.startTime)}"
                    }
                    val endText = remember(nextTurn) {
                        val nt = nextTurn
                        if (nt == null) "" else "${formatPersianDate(nt.endTime)} - ${formatPersianTime(nt.endTime)}"
                    }
                    val hasTurn = remember(nextTurn) { nextTurn != null }

                    // BackHandler: بازگشت از صفحات داخلی به home
                    val shouldHandleBack = when {
                        showOnboarding -> false
                        userNationalCode.isBlank() -> false
                        currentWellId == null -> false
                        currentScreen == "home" -> false
                        else -> true
                    }

                    if (shouldHandleBack) {
                        BackHandler {
                            currentScreen = "home"
                        }
                    }

                    when {
                        showOnboarding -> {
                            OnboardingScreen(onFinished = {
                                onboardingPrefs.edit().putBoolean("first_launch", false).apply()
                                showOnboarding = false
                            })
                        }

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
                                onBack = null
                            )
                        }

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
                            val wid = currentWellId!!
                            when (currentUser?.role) {
                                UserRole.OPERATOR -> {
                                    OperatorDashboardScreen(
                                        wellId = wid,
                                        wellName = well?.name ?: "",
                                        onBack = { },
                                        onManageGroups = { currentScreen = "groups" },
                                        onFullSchedule = { currentScreen = "schedule" },
                                        onNotify = { currentScreen = "notify" }
                                    )
                                }
                                UserRole.SMALL_OWNER, UserRole.ABYAR -> {
                                    FarmerDashboardScreen(
                                        userName = userName,
                                        wellName = well?.name ?: "",
                                        nextTurn = nextTurn,
                                        operatorName = operator?.fullName,
                                        operatorPhone = operator?.phone,
                                        notifications = notifications,
                                        onSwitchWell = { currentScreen = "wells" },
                                        onEditProfile = { currentScreen = "profile" }
                                    )
                                }
                                UserRole.MAIN_ADMIN, UserRole.GROUP_LEADER, null -> {
                                    HomeScreen(
                                        userName = userName,
                                        phone = userPhone,
                                        wellName = well?.name ?: "",
                                        wellCode = well?.code ?: "",
                                        startTurnText = startText,
                                        endTurnText = endText,
                                        hasTurn = hasTurn,
                                        userShareHours = currentUser?.shareHours ?: 0.0,
                                        onEditProfile = { currentScreen = "profile" },
                                        onSwitchWell = { currentScreen = "wells" },
                                        onPanelClick = { route ->
                                            screenTitle = titleFor(route)
                                            currentScreen = route
                                        }
                                    )
                                }
                            }
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

                        currentScreen == "schedule" -> {
                            val wid = currentWellId
                            if (wid != null) {
                                ScheduleScreen(
                                    wellId = wid,
                                    onBack = { currentScreen = "home" }
                                )
                            } else {
                                ComingSoonScreen(
                                    title = "برنامه آبیاری",
                                    onBack = { currentScreen = "home" }
                                )
                            }
                        }

                        currentScreen == "operator" -> {
                            val wid = currentWellId
                            if (wid != null) {
                                OperatorDashboardScreen(
                                    wellId = wid,
                                    wellName = well?.name ?: "",
                                    onBack = { currentScreen = "home" },
                                    onManageGroups = { currentScreen = "groups" },
                                    onFullSchedule = { currentScreen = "schedule" },
                                    onNotify = { currentScreen = "notify" }
                                )
                            } else {
                                ComingSoonScreen(
                                    title = "پنل موتوربان",
                                    onBack = { currentScreen = "home" }
                                )
                            }
                        }

                        currentScreen == "groups" -> {
                            val wid = currentWellId
                            if (wid != null) {
                                com.example.abyar.ui.groups.GroupManagementScreen(
                                    wellId = wid,
                                    onBack = { currentScreen = "home" }
                                )
                            } else {
                                ComingSoonScreen(
                                    title = "مدیریت گروه‌ها",
                                    onBack = { currentScreen = "home" }
                                )
                            }
                        }

                        currentScreen == "notify" -> {
                            val wid = currentWellId
                            if (wid != null) {
                                NotifyScreen(
                                    wellId = wid,
                                    onBack = { currentScreen = "home" }
                                )
                            } else {
                                ComingSoonScreen(
                                    title = "اطلاع‌رسانی",
                                    onBack = { currentScreen = "home" }
                                )
                            }
                        }

                        else -> {
                            ComingSoonScreen(
                                title = screenTitle.ifBlank { "پنل" },
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
        "groups" -> "مدیریت گروه‌ها"
        else -> "پنل"
    }
}
