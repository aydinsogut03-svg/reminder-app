package com.aydinsogut.reminder

import android.app.Application
import android.content.Context

class ReminderApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notifications.createChannel()
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as ReminderApp).container
