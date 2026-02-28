package com.rayliu0712.saveit.compose

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.S
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun MyTheme(
  // Dynamic color is available on Android 12+
  content: @Composable () -> Unit
) {
  val isDark = isSystemInDarkTheme()

  val colorScheme = when {
    SDK_INT >= S -> {
      val context = LocalContext.current

      if (isDark)
        dynamicDarkColorScheme(context)
      else
        dynamicLightColorScheme(context)
    }

    isDark -> darkColorScheme()
    else -> lightColorScheme()
  }

  MaterialTheme(
    colorScheme = colorScheme.copy(
      background = colorScheme.surfaceContainer,
    ),
    content = content
  )
}