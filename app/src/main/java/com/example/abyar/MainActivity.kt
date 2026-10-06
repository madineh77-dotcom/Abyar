package com.example.abyar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.abyar.ui.admin.AdminScreen
import com.example.abyar.ui.owner.OwnerScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var tab by remember { mutableIntStateOf(0) }
                    val tabs = listOf("پنل مدیر", "پنل مالک")

                    Scaffold(
                        bottomBar = {
                            NavigationBar {
                                tabs.forEachIndexed { index, title ->
                                    NavigationBarItem(
                                        selected = tab == index,
                                        onClick = { tab = index },
                                        icon = { Text(if (index == 0) "👨‍💼" else "👨‍🌾") },
                                        label = { Text(title) }
                                    )
                                }
                            }
                        }
                    ) { padding ->
                        Surface(modifier = Modifier.padding(padding)) {
                            if (tab == 0) AdminScreen() else OwnerScreen()
                        }
                    }
                }
            }
        }
    }
}
