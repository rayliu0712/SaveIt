package com.rayliu0712.saveit

import android.app.DownloadManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val mime = intent.type

    setContent {
      when (intent.action) {
        Intent.ACTION_VIEW -> MyContent(intent.data, mime)

        Intent.ACTION_SEND -> MyContent(
          intent.getParcelableExtra(Intent.EXTRA_STREAM),
          mime
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyContent(uri: Uri?, mime: String?) {
  val context = LocalContext.current
  if (uri == null) {
    Toast.makeText(context, "No uri", Toast.LENGTH_LONG).show()
    context.finishApp()
    return
  }

  var filename by remember { mutableStateOf<String?>(null) }
  var fileSize by remember { mutableStateOf<Long?>(null) }
  var copiedLen by remember { mutableStateOf<Long?>(null) }
  var expanded by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()

  LaunchedEffect(Unit) {
    val pair = withContext(Dispatchers.IO) {
      context.getFilenameAndSize(uri)
    }
    filename = pair.first
    fileSize = pair.second
  }

  fun saveAndFinish(openDownloadFolder: Boolean) {
    copiedLen = 0

    scope.launch {
      context.saveToDownload(uri, filename!!, mime, fileSize!!)
        .flowOn(Dispatchers.IO)
        .collect { value ->
          copiedLen = value
        }

      Toast.makeText(context, "Completed", Toast.LENGTH_LONG).show()
      context.finishApp()

      if (openDownloadFolder) {
        val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
        context.startActivity(intent)
      }
    }
  }

  fun launchAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    intent.data = Uri.fromParts("package", "com.rayliu0712.saveit", null)
    context.startActivity(intent)
  }

  fun launchRepo() {
    val intent = Intent(Intent.ACTION_VIEW)
    intent.data = "https://github.com/rayliu0712/SaveIt".toUri()
    context.startActivity(intent)
  }

  MyTheme {
    Scaffold(
      topBar = {
        TopAppBar(
          title = { Text("Save It") },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
          ),
          actions = {
            Box {
              IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "more")
              }

              DropdownMenu(
                modifier = Modifier.width(150.dp),
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = MaterialTheme.colorScheme.surface
              ) {
                DropdownMenuItem(
                  onClick = ::launchAppSettings,
                  leadingIcon = {
                    Icon(
                      Icons.Default.Settings,
                      contentDescription = "settings"
                    )
                  },
                  text = { Text("Settings") },
                )
                DropdownMenuItem(
                  onClick = ::launchRepo,
                  leadingIcon = { Icon(Icons.Default.Coffee, "GitHub") },
                  text = { Text("GitHub") },
                )
              }
            }
          }
        )
      }
    ) { padding ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(filename ?: "Querying file name")
        Spacer(Modifier.height(10.dp))
        Text(fileSize?.fileSizeFormat ?: "Querying file size")
        Spacer(Modifier.height(20.dp))

        if (copiedLen == null) {
          Button(
            modifier = Modifier.width(150.dp),
            enabled = filename != null && fileSize != null,
            onClick = { saveAndFinish(openDownloadFolder = false) },
          ) {
            Text("Save")
          }
          Spacer(Modifier.height(10.dp))
          FilledTonalButton(
            modifier = Modifier.width(150.dp),
            enabled = filename != null && fileSize != null,
            onClick = { saveAndFinish(openDownloadFolder = true) },
          ) {
            Text("Save & Open")
          }
        } else
          CircularProgressIndicator(progress = {
            copiedLen!!.toFloat() / fileSize!!
          })
      }
    }
  }
}
