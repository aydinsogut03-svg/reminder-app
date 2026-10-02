package com.aydinsogut.reminder.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider as DayNightColor
import androidx.glance.layout.Box
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.TextAlign
import androidx.glance.unit.ColorProvider
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
import com.aydinsogut.reminder.R
import com.aydinsogut.reminder.alarm.ReminderIntents
import com.aydinsogut.reminder.appContainer
import com.aydinsogut.reminder.data.Reminder
import com.aydinsogut.reminder.ui.MainActivity
import com.aydinsogut.reminder.ui.theme.ReminderPalette
import com.aydinsogut.reminder.util.TimeFormats
import com.aydinsogut.reminder.util.capitalizeTr
import java.time.LocalDate
import com.aydinsogut.reminder.util.formatReminderTime

class ReminderWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val reminders = context.appContainer.repository.upcoming(limit = 8)
        provideContent {
            WidgetContent(context, reminders)
        }
    }
}

class ReminderWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ReminderWidget()
}

private val WidgetBackground = DayNightColor(day = Color(0xFFFFFFFF), night = Color(0xFF171A23))
private val WidgetText = DayNightColor(day = Color(0xFF111827), night = Color(0xFFE7E9F1))
private val WidgetSubtle = DayNightColor(day = Color(0xFF5B6275), night = Color(0xFFA3A9BC))
private val WidgetPrimary = DayNightColor(day = Color(0xFF4F46E5), night = Color(0xFFA5B4FC))
private val WidgetItemBackground = DayNightColor(day = Color(0xFFF3F4FA), night = Color(0xFF222634))

@Composable
private fun WidgetContent(context: Context, reminders: List<Reminder>) {
    val openApp = Intent(context, MainActivity::class.java)
    val addNew = Intent(context, MainActivity::class.java)
        .setAction(ReminderIntents.ACTION_ADD)
        .setData(Uri.parse("reminder://add"))
    val voice = Intent(context, MainActivity::class.java)
        .setAction(ReminderIntents.ACTION_VOICE)
        .setData(Uri.parse("reminder://voice"))

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetBackground)
            .cornerRadius(24.dp)
            .padding(14.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().clickable(actionStartActivity(openApp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = LocalDate.now().format(TimeFormats.dayTitle).capitalizeTr(),
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = WidgetPrimary),
                    maxLines = 1,
                )
                Text(
                    text = if (reminders.isEmpty()) "Hatırlatıcılar" else "${reminders.size} hatırlatıcı",
                    style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = WidgetText),
                    maxLines = 1,
                )
            }
            Box(
                modifier = GlanceModifier
                    .size(36.dp)
                    .background(DayNightColor(day = Color(0xFFCCFBF1), night = Color(0xFF5EEAD4)))
                    .cornerRadius(18.dp)
                    .clickable(actionStartActivity(voice)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_mic),
                    contentDescription = "Sesle ekle",
                    modifier = GlanceModifier.size(20.dp),
                )
            }
            Spacer(GlanceModifier.width(8.dp))
            Box(
                modifier = GlanceModifier
                    .size(36.dp)
                    .background(WidgetPrimary)
                    .cornerRadius(18.dp)
                    .clickable(actionStartActivity(addNew)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+",
                    style = TextStyle(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = DayNightColor(day = Color.White, night = Color(0xFF1E1B4B)),
                    ),
                )
            }
        }
        Spacer(GlanceModifier.height(10.dp))
        if (reminders.isEmpty()) {
            Box(
                modifier = GlanceModifier.fillMaxSize().clickable(actionStartActivity(addNew)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Yaklaşan hatırlatıcı yok.\nEklemek için dokun.",
                    style = TextStyle(color = WidgetSubtle, fontSize = 13.sp, textAlign = TextAlign.Center),
                )
            }
        } else {
            LazyColumn {
                items(reminders, itemId = { it.id }) { reminder ->
                    WidgetItem(context, reminder)
                }
            }
        }
    }
}

@Composable
private fun WidgetItem(context: Context, reminder: Reminder) {
    val open = Intent(context, MainActivity::class.java)
        .setAction(ReminderIntents.ACTION_OPEN)
        .setData(Uri.parse("reminder://open/${reminder.id}"))
        .putExtra(ReminderIntents.EXTRA_ID, reminder.id)
    val accent = ReminderPalette.color(reminder.colorIndex)

    Column(modifier = GlanceModifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(WidgetItemBackground)
                .cornerRadius(14.dp)
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .clickable(actionStartActivity(open)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = GlanceModifier
                    .width(4.dp)
                    .height(30.dp)
                    .background(accent)
                    .cornerRadius(2.dp),
            ) {}
            Spacer(GlanceModifier.width(10.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = reminder.title,
                    maxLines = 1,
                    style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, color = WidgetText),
                )
                Text(
                    text = formatReminderTime(reminder.triggerAt),
                    maxLines = 1,
                    style = TextStyle(fontSize = 12.sp, color = ColorProvider(accent)),
                )
            }
        }
    }
}
