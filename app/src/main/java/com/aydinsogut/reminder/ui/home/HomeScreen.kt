package com.aydinsogut.reminder.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aydinsogut.reminder.ui.calendar.CalendarScreen
import com.aydinsogut.reminder.ui.components.PermissionBanners
import com.aydinsogut.reminder.ui.list.ReminderListScreen
import com.aydinsogut.reminder.ui.settings.SettingsScreen
import java.time.LocalDate

@Composable
fun HomeScreen(
    onAdd: (LocalDate?) -> Unit,
    onVoice: () -> Unit,
    onOpen: (Long) -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                val itemColors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Rounded.NotificationsActive, contentDescription = null) },
                    label = { Text("Hatırlatıcılar") },
                    colors = itemColors,
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null) },
                    label = { Text("Takvim") },
                    colors = itemColors,
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                    label = { Text("Ayarlar") },
                    colors = itemColors,
                )
            }
        },
        floatingActionButton = {
            if (tab == 0) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SmallFloatingActionButton(
                        onClick = onVoice,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ) {
                        Icon(Icons.Rounded.Mic, contentDescription = "Sesle ekle")
                    }
                    ExtendedFloatingActionButton(
                        onClick = { onAdd(null) },
                        icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                        text = { Text("Yeni") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            PermissionBanners()
            when (tab) {
                0 -> ReminderListScreen(
                    onOpen = onOpen,
                    snackbarHostState = snackbarHostState,
                    modifier = Modifier.weight(1f),
                )
                1 -> CalendarScreen(
                    onAddForDate = onAdd,
                    onOpen = onOpen,
                    modifier = Modifier.weight(1f),
                )
                else -> SettingsScreen(modifier = Modifier.weight(1f))
            }
        }
    }
}
