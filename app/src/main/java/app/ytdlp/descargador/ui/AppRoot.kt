package app.ytdlp.descargador.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.ytdlp.descargador.engine.DownloadManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(
    sharedUrl: String?,
    onSharedConsumed: () -> Unit,
    vm: HomeViewModel = viewModel(),
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var inSettings by rememberSaveable { mutableStateOf(false) }
    val jobs by DownloadManager.jobs.collectAsStateWithLifecycle()

    LaunchedEffect(sharedUrl) {
        if (sharedUrl != null) {
            vm.url = sharedUrl
            tab = 0
            inSettings = false
            onSharedConsumed()
        }
    }

    BackHandler(enabled = inSettings) { inSettings = false }

    if (inSettings) {
        SettingsScreen(onBack = { inSettings = false })
        return
    }

    val activeCount = jobs.count { it.isActive }

    // En horizontal se usa un riel lateral: libera altura para el contenido y el botón Descargar.
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth > maxHeight

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            if (tab == 0) "Descargador" else "Descargas",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = { inSettings = true },
                            modifier = Modifier.testTag("settings_button"),
                        ) {
                            Icon(Icons.Rounded.Settings, contentDescription = "Abrir ajustes")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            },
            bottomBar = {
                if (!wide) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                        NavigationBarItem(
                            selected = tab == 0,
                            onClick = { tab = 0 },
                            icon = { Icon(Icons.Rounded.Link, contentDescription = null) },
                            label = { Text("Descargar") },
                        )
                        NavigationBarItem(
                            selected = tab == 1,
                            onClick = { tab = 1 },
                            icon = { QueueIcon(activeCount) },
                            label = { Text("Descargas") },
                        )
                    }
                }
            },
        ) { padding ->
            Row(
                Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding)
            ) {
                if (wide) {
                    NavigationRail(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                        NavigationRailItem(
                            selected = tab == 0,
                            onClick = { tab = 0 },
                            icon = { Icon(Icons.Rounded.Link, contentDescription = null) },
                            label = { Text("Descargar") },
                        )
                        NavigationRailItem(
                            selected = tab == 1,
                            onClick = { tab = 1 },
                            icon = { QueueIcon(activeCount) },
                            label = { Text("Descargas") },
                        )
                    }
                }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .imePadding()
                ) {
                    when (tab) {
                        0 -> HomeScreen(vm = vm, onViewQueue = { tab = 1 })
                        else -> DownloadsScreen(onGoHome = { tab = 0 })
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueIcon(activeCount: Int) {
    BadgedBox(badge = { if (activeCount > 0) Badge { Text("$activeCount") } }) {
        Icon(Icons.Rounded.Download, contentDescription = null)
    }
}
