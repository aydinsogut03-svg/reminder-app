package com.aydinsogut.reminder.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aydinsogut.reminder.ui.edit.EditReminderScreen
import com.aydinsogut.reminder.ui.home.HomeScreen
import java.time.LocalDate

private object Routes {
    const val HOME = "home"
    const val EDIT = "edit?id={id}&date={date}"

    fun edit(id: Long = 0L, date: LocalDate? = null): String = buildString {
        append("edit?id=$id")
        if (date != null) append("&date=$date")
    }
}

@Composable
fun ReminderNavHost(
    launchRequest: LaunchRequest?,
    onLaunchRequestHandled: () -> Unit,
) {
    val navController = rememberNavController()

    LaunchedEffect(launchRequest) {
        val route = when (launchRequest) {
            LaunchRequest.NewReminder -> Routes.edit()
            is LaunchRequest.OpenReminder -> Routes.edit(id = launchRequest.id)
            null -> return@LaunchedEffect
        }
        navController.navigate(route) { launchSingleTop = true }
        onLaunchRequestHandled()
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onAdd = { date -> navController.navigate(Routes.edit(date = date)) },
                onOpen = { id -> navController.navigate(Routes.edit(id = id)) },
            )
        }
        composable(
            route = Routes.EDIT,
            arguments = listOf(
                navArgument("id") {
                    type = NavType.LongType
                    defaultValue = 0L
                },
                navArgument("date") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            val date = entry.arguments?.getString("date")?.let(LocalDate::parse)
            EditReminderScreen(
                id = id,
                initialDate = date,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
