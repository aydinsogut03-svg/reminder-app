package com.aydinsogut.reminder.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aydinsogut.reminder.ui.edit.EditReminderScreen
import com.aydinsogut.reminder.ui.home.HomeScreen
import com.aydinsogut.reminder.ui.ink.InkScreen
import java.time.LocalDate

private object Routes {
    const val HOME = "home"
    const val INK = "ink"
    const val EDIT = "edit?id={id}&date={date}&text={text}&voice={voice}"

    fun edit(
        id: Long = 0L,
        date: LocalDate? = null,
        text: String? = null,
        voice: Boolean = false,
    ): String = buildString {
        append("edit?id=$id&voice=$voice")
        if (date != null) append("&date=$date")
        if (text != null) append("&text=${Uri.encode(text)}")
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
            is LaunchRequest.NewReminder -> Routes.edit(text = launchRequest.text, voice = launchRequest.voice)
            is LaunchRequest.OpenReminder -> Routes.edit(id = launchRequest.id)
            LaunchRequest.Ink -> Routes.INK
            null -> return@LaunchedEffect
        }
        navController.navigate(route) { launchSingleTop = true }
        onLaunchRequestHandled()
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onAdd = { date -> navController.navigate(Routes.edit(date = date)) },
                onVoice = { navController.navigate(Routes.edit(voice = true)) },
                onInk = { navController.navigate(Routes.INK) },
                onOpen = { id -> navController.navigate(Routes.edit(id = id)) },
            )
        }
        composable(Routes.INK) {
            InkScreen(
                onClose = { navController.popBackStack() },
                onDone = { text ->
                    navController.navigate(Routes.edit(text = text)) {
                        popUpTo(Routes.INK) { inclusive = true }
                    }
                },
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
                navArgument("text") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("voice") {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) { entry ->
            val args = entry.arguments
            EditReminderScreen(
                id = args?.getLong("id") ?: 0L,
                initialDate = args?.getString("date")?.let(LocalDate::parse),
                initialText = args?.getString("text"),
                startWithVoice = args?.getBoolean("voice") ?: false,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
