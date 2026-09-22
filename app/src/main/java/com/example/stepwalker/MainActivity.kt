package com.example.stepwalker

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.stepwalker.ui.HistoryScreen
import com.example.stepwalker.ui.HomeScreen
import com.example.stepwalker.ui.MapScreen
import com.example.stepwalker.ui.ThemePicker

class MainActivity : ComponentActivity() {
    private val vm: StepViewModel by viewModels()

    private var preciseLocationGranted by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        preciseLocationGranted =
            result[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }

    private val createGpxLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/gpx+xml")
    ) { uri ->
        if (uri != null) {
            GpxExporter.write(this, uri, vm.state.value.route)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissions = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        permissionLauncher.launch(permissions.toTypedArray())

        setContent {
            StepWalkerApp(
                vm = vm,
                preciseLocationGranted = preciseLocationGranted,
                onExportGpx = ::exportCurrentRoute
            )
        }
    }

    private fun exportCurrentRoute() {
        if (vm.state.value.route.size < 2) return
        createGpxLauncher.launch("StepWalker-${System.currentTimeMillis()}.gpx")
    }
}

@Composable
fun StepWalkerApp(
    vm: StepViewModel,
    preciseLocationGranted: Boolean,
    onExportGpx: () -> Unit
) {
    var theme by remember { mutableStateOf(AppTheme.MIST) }
    var showPicker by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showMap by remember { mutableStateOf(false) }
    var showPreciseDialog by remember { mutableStateOf(false) }

    val state by vm.state.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle(initialValue = emptyList())

    val context = androidx.compose.ui.platform.LocalContext.current

    val wrappedStart: () -> Unit = {
        if (!preciseLocationGranted) {
            showPreciseDialog = true
        } else {
            vm.startTracking()
        }
    }

    if (showPreciseDialog) {
        AlertDialog(
            onDismissRequest = { showPreciseDialog = false },
            title = { Text("Нужно точное местоположение") },
            text = {
                Text(
                    "Для максимальной точности трекинга приложение использует " +
                        "только ACCESS_FINE_LOCATION. Включите «Точное местоположение» " +
                        "в разрешениях и режим «Высокая точность» в настройках геолокации."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showPreciseDialog = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) { Text("Открыть настройки") }
            },
            dismissButton = {
                TextButton(onClick = { showPreciseDialog = false }) { Text("Отмена") }
            }
        )
    }

    when {
        showPicker -> ThemePicker(
            current = theme,
            onPick = { theme = it; showPicker = false },
            onClose = { showPicker = false }
        )

        showHistory -> HistoryScreen(
            theme = theme,
            history = history,
            onClose = { showHistory = false }
        )

        showMap -> MapScreen(
            state = state,
            theme = theme,
            onBack = { showMap = false },
            onExportGpx = onExportGpx
        )

        else -> HomeScreen(
            state = state,
            theme = theme,
            onStart = wrappedStart,
            onStop = vm::stopTracking,
            onReset = vm::reset,
            onTheme = { showPicker = true },
            onHistory = { showHistory = true },
            onMap = { showMap = true },
            onExportGpx = onExportGpx
        )
    }
}
