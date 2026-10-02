package com.aydinsogut.reminder.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.aydinsogut.reminder.ui.calendar.CalendarScreen
import com.aydinsogut.reminder.ui.components.PermissionBanners
import com.aydinsogut.reminder.ui.settings.SettingsScreen
import java.time.LocalDate

private data class Tab(val label: String, val icon: ImageVector)

private val Tabs = listOf(
    Tab("Ana sayfa", Icons.Rounded.Home),
    Tab("Takvim", Icons.Rounded.CalendarMonth),
    Tab("Ayarlar", Icons.Rounded.Settings),
)

@Composable
fun HomeScreen(
    onAdd: (LocalDate?) -> Unit,
    onVoice: () -> Unit,
    onOpen: (Long) -> Unit,
    calendarSignal: Int = 0,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(calendarSignal) { if (calendarSignal > 0) tab = 1 }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                    val itemColors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                    )
                    Tabs.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = tab == index,
                            onClick = { tab = index },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(item.label) },
                            colors = itemColors,
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            PermissionBanners()
            when (tab) {
                0 -> DashboardScreen(
                    onOpen = onOpen,
                    onAdd = { onAdd(null) },
                    onVoice = onVoice,
                    onInk = { tab = 1 },
                    snackbarHostState = snackbarHostState,
                    modifier = Modifier.weight(1f),
                )
                1 -> CalendarScreen(
                    onAddForDate = onAdd,
                    onOpen = onOpen,
                    snackbarHostState = snackbarHostState,
                    modifier = Modifier.weight(1f),
                )
                else -> SettingsScreen(modifier = Modifier.weight(1f))
            }
        }
    }
}
