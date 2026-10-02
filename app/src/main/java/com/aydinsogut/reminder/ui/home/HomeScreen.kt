package com.aydinsogut.reminder.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.aydinsogut.reminder.ui.calendar.CalendarScreen
import com.aydinsogut.reminder.ui.components.PermissionBanners
import com.aydinsogut.reminder.ui.list.ReminderListScreen
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAdd: (LocalDate?) -> Unit,
    onOpen: (Long) -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Hatırlatıcı") }) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.Notifications, contentDescription = null) },
                    label = { Text("Hatırlatıcılar") },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                    label = { Text("Takvim") },
                )
            }
        },
        floatingActionButton = {
            if (tab == 0) {
                FloatingActionButton(onClick = { onAdd(null) }) {
                    Icon(Icons.Default.Add, contentDescription = "Hatırlatıcı ekle")
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            PermissionBanners()
            when (tab) {
                0 -> ReminderListScreen(onOpen = onOpen, modifier = Modifier.weight(1f))
                else -> CalendarScreen(onAddForDate = onAdd, onOpen = onOpen, modifier = Modifier.weight(1f))
            }
        }
    }
}
