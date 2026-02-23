package com.rayliu0712.saveit.main

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    setContent {
      MainContent()
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun MainContent() {
  val context = LocalContext.current
  val activity = LocalActivity.current!!

  var expanded by remember { mutableStateOf(false) }
  var notificationState by remember {
    mutableStateOf(context.checkNotificationPermission())
  }

  val notificationPermissionRequester = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    notificationState =
      if (isGranted)
        true
      else if (
        activity.shouldShowRequestPermissionRationale(POST_NOTIFICATIONS)
      )
        false
      else
        null
  }

  val intentLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.StartActivityForResult()
  ) { result ->
    notificationState = context.checkNotificationPermission()
  }

  LaunchedEffect(Unit) {
    if (SDK_INT >= TIRAMISU) {
      notificationPermissionRequester.launch(POST_NOTIFICATIONS)
    }
  }

  MyTheme {
    Scaffold(
      topBar = {
        TopAppBar(
          title = { Text("Save It") },
          actions = {
            Box {
              IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = null)
              }
              DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = MaterialTheme.colorScheme.surface
              ) {
                DropdownMenuItem(
                  text = { Text("GitHub") },
                  onClick = {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                      data = "https://github.com/rayliu0712/SaveIt".toUri()
                    }
                    context.startActivity(intent)
                  },
                  leadingIcon = {
                    Icon(
                      Icons.Default.Coffee,
                      contentDescription = null
                    )
                  },
                )
              }
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
          )
        )
      }
    ) { padding ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        when (notificationState) {
          true -> Text("已允許通知")

          // SDK < TIRAMISU will not go to this branch
          false -> Button(onClick = {
            notificationPermissionRequester.launch(POST_NOTIFICATIONS)
          }) {
            Text("允許通知")
          }

          null -> Button(onClick = {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
              putExtra(Settings.EXTRA_APP_PACKAGE, "com.rayliu0712.saveit")
            }
            intentLauncher.launch(intent)
          }) {
            Text("手動允許通知")
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = {
          val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
          context.startActivity(intent)
        }) {
          Icon(
            Icons.Filled.FolderOpen,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize),
          )
          Spacer(Modifier.size(ButtonDefaults.IconSpacing))
          Text("打開下載資料夾")
        }
      }
    }
  }
}

fun Context.checkNotificationPermission(): Boolean? {
  val isGranted = NotificationManagerCompat.from(this)
    .areNotificationsEnabled()

  return if (SDK_INT >= TIRAMISU)
    isGranted
  else if (isGranted)
    true
  else
    null
}