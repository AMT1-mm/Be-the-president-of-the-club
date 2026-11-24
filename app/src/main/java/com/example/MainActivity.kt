package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.ui.CoachGameScreen
import com.example.ui.theme.BeACoachTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val darkBarColor = 0xFF070D1A.toInt()
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(darkBarColor),
      navigationBarStyle = SystemBarStyle.dark(darkBarColor),
    )
    setContent {
      CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        BeACoachTheme { CoachGameScreen() }
      }
    }
  }
}
