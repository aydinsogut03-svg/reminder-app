package com.aydinsogut.reminder.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.aydinsogut.reminder.alarm.ReminderIntents
import com.aydinsogut.reminder.appContainer
import com.aydinsogut.reminder.data.Reminder
import com.aydinsogut.reminder.ui.MainActivity
import com.aydinsogut.reminder.util.formatReminderTime

class ReminderWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val reminders = context.appContainer.repository.upcoming(limit = 8)
        provideContent {
            GlanceTheme {
                WidgetContent(context, reminders)
            }
        }
    }
}

class ReminderWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ReminderWidget()
}

@Composable
private fun WidgetContent(context: Context, reminders: List<Reminder>) {
    val openApp = Intent(context, MainActivity::class.java)
    val addNew = Intent(context, MainActivity::class.java)
        .setAction(ReminderIntents.ACTION_ADD)
        .setData(Uri.parse("reminder://add"))

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(16.dp)
            .padding(12.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Hatırlatıcılar",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = GlanceTheme.colors.onSurface,
                ),
                modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity(openApp)),
            )
            Text(
                text = "+ Ekle",
                style = TextStyle(
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = GlanceTheme.colors.primary,
                ),
                modifier = GlanceModifier.padding(4.dp).clickable(actionStartActivity(addNew)),
            )
        }
        Spacer(GlanceModifier.height(8.dp))
        if (reminders.isEmpty()) {
            Text(
                text = "Yaklaşan hatırlatıcı yok",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
            )
        } else {
            LazyColumn {
                items(reminders, itemId = { it.id }) { reminder ->
                    val open = Intent(context, MainActivity::class.java)
                        .setAction(ReminderIntents.ACTION_OPEN)
                        .setData(Uri.parse("reminder://open/${reminder.id}"))
                        .putExtra(ReminderIntents.EXTRA_ID, reminder.id)
                    Column(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable(actionStartActivity(open)),
                    ) {
                        Text(
                            text = reminder.title,
                            maxLines = 1,
                            style = TextStyle(
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = GlanceTheme.colors.onSurface,
                            ),
                        )
                        Text(
                            text = formatReminderTime(reminder.triggerAt),
                            style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant),
                        )
                    }
                }
            }
        }
    }
}
