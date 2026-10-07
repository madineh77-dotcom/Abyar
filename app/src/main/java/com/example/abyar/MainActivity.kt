package com.example.abyar

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.abyar.ui.admin.AdminScreen
import com.example.abyar.ui.onboarding.OnboardingScreen
import com.example.abyar.ui.owner.OwnerScreen
import com.example.abyar.ui.theme.AbyarBlue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("abyar_prefs", Context.MODE_PRIVATE)
        val isFirstLaunch = prefs.getBoolean("first_launch", true)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF5F7FA)) {
                    var showOnboarding by remember { mutableStateOf(isFirstLaunch) }

                    if (showOnboarding) {
                        OnboardingScreen(onFinished = {
                            prefs.edit().putBoolean("first_launch", false).apply()
                            showOnboarding = false
                        })
                    } else {
                        MainTabs()
                    }
                }
            }
        }
    }
}

@Composable
fun MainTabs() {
    var tab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("پنل مدیر") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AbyarBlue,
                        selectedTextColor = AbyarBlue,
                        indicatorColor = Color(0xFFE3F2FD)
                    )
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text("پنل مالک") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AbyarBlue,
                        selectedTextColor = AbyarBlue,
                        indicatorColor = Color(0xFFE3F2FD)
                    )
                )
            }
        }
    ) { padding ->
        Surface(modifier = Modifier.padding(padding)) {
            if (tab == 0) AdminScreen() else OwnerScreen()
        }
    }
}
