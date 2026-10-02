package com.aydinsogut.reminder.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.aydinsogut.reminder.R
import com.aydinsogut.reminder.alarm.ReminderIntents
import com.aydinsogut.reminder.appContainer
import com.aydinsogut.reminder.data.Reminder
import com.aydinsogut.reminder.data.RepeatRule
import com.aydinsogut.reminder.ui.MainActivity
import com.aydinsogut.reminder.ui.quick.QuickInkActivity
import com.aydinsogut.reminder.ui.theme.ReminderPalette
import com.aydinsogut.reminder.util.TimeFormats
import com.aydinsogut.reminder.util.TurkishLocale
import com.aydinsogut.reminder.util.capitalizeTr
import com.aydinsogut.reminder.util.formatReminderTime
import com.aydinsogut.reminder.util.toLocalDate
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle as JavaTextStyle
import androidx.glance.color.ColorProvider as DayNightColor

private val Small = DpSize(180.dp, 110.dp)
private val Medium = DpSize(250.dp, 180.dp)
private val Large = DpSize(250.dp, 300.dp)

class ReminderWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(Small, Medium, Large))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val upcoming = context.appContainer.repository.upcoming(limit = 40)
        val data = WidgetData(
            items = upcoming.take(10),
            busyDays = upcoming.groupingBy { it.triggerAt.toLocalDate() }.eachCount(),
            missed = upcoming.count { it.repeat == RepeatRule.NONE && it.triggerAt < System.currentTimeMillis() },
        )
        provideContent { WidgetContent(context, data) }
    }
}

class ReminderWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ReminderWidget()
}

/** Widget'taki ✓: hatırlatıcıyı uygulamayı açmadan tamamlar. */
class CompleteReminderAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[ReminderIdKey] ?: return
        context.appContainer.repository.completeOnce(id)
    }
}

private val ReminderIdKey = ActionParameters.Key<Long>("reminder_id")

private data class WidgetData(
    val items: List<Reminder>,
    val busyDays: Map<LocalDate, Int>,
    val missed: Int,
)

private val WidgetBackground = DayNightColor(day = Color(0xFFFFFFFF), night = Color(0xFF171A23))
private val WidgetText = DayNightColor(day = Color(0xFF1C1F2A), night = Color(0xFFE7E9F1))
private val WidgetSubtle = DayNightColor(day = Color(0xFF6B7083), night = Color(0xFFA3A9BC))
private val WidgetPrimary = DayNightColor(day = Color(0xFF5B5FE3), night = Color(0xFFA5B4FC))
private val WidgetOnPrimary = DayNightColor(day = Color.White, night = Color(0xFF1E1B4B))
private val WidgetItemBackground = DayNightColor(day = Color(0xFFF5F5FA), night = Color(0xFF222634))
private val WidgetPaper = DayNightColor(day = Color(0xFFFFF7EA), night = Color(0xFF2A2418))
private val WidgetMint = DayNightColor(day = Color(0xFFE4F6F2), night = Color(0xFF1D3B36))
private val WidgetError = DayNightColor(day = Color(0xFFE5484D), night = Color(0xFFF87171))
private val Transparent = DayNightColor(day = Color.Transparent, night = Color.Transparent)

@Composable
private fun WidgetContent(context: Context, data: WidgetData) {
    val size = LocalSize.current
    val compact = size.height < Medium.height
    val large = size.height >= Large.height

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetBackground)
            .cornerRadius(24.dp)
            .padding(12.dp),
    ) {
        Header(context, data, compact)
        Spacer(GlanceModifier.height(8.dp))
        if (!compact) {
            WeekStrip(context, data.busyDays)
            Spacer(GlanceModifier.height(8.dp))
        }
        PenPad(context, tall = large)
        Spacer(GlanceModifier.height(8.dp))
        if (data.items.isEmpty()) {
            if (!compact) {
                Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Yaklaşan hatırlatıcı yok",
                        style = TextStyle(color = WidgetSubtle, fontSize = 13.sp, textAlign = TextAlign.Center),
                    )
                }
            }
        } else {
            LazyColumn {
                items(data.items, itemId = { it.id }) { reminder ->
                    WidgetItem(context, reminder)
                }
            }
        }
    }
}

@Composable
private fun Header(context: Context, data: WidgetData, compact: Boolean) {
    val openApp = Intent(context, MainActivity::class.java)
    val voice = Intent(context, MainActivity::class.java)
        .setAction(ReminderIntents.ACTION_VOICE)
        .setData(Uri.parse("reminder://voice"))
    val addNew = Intent(context, MainActivity::class.java)
        .setAction(ReminderIntents.ACTION_ADD)
        .setData(Uri.parse("reminder://add"))
    val hour = LocalTime.now().hour
    val greeting = when (hour) {
        in 5..11 -> "Günaydın"
        in 12..17 -> "İyi günler"
        in 18..22 -> "İyi akşamlar"
        else -> "İyi geceler"
    }
    val today = data.busyDays[LocalDate.now()] ?: 0

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
                text = if (compact) greeting else "$greeting · bugün $today",
                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = WidgetText),
                maxLines = 1,
            )
            if (data.missed > 0 && !compact) {
                Text(
                    text = "${data.missed} kaçırılan uyarı",
                    style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = WidgetError),
                    maxLines = 1,
                )
            }
        }
        CircleButton(WidgetMint, actionIntent = voice) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_mic),
                contentDescription = "Sesle ekle",
                modifier = GlanceModifier.size(18.dp),
            )
        }
        Spacer(GlanceModifier.width(6.dp))
        CircleButton(WidgetPrimary, actionIntent = addNew) {
            Text(
                text = "+",
                style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = WidgetOnPrimary),
            )
        }
    }
}

@Composable
private fun CircleButton(color: ColorProvider, actionIntent: Intent, content: @Composable () -> Unit) {
    Box(
        modifier = GlanceModifier
            .size(34.dp)
            .background(color)
            .cornerRadius(17.dp)
            .clickable(actionStartActivity(actionIntent)),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Bugünden başlayan 7 gün; bir güne dokununca o gün için kalem penceresi açılır. */
@Composable
private fun WeekStrip(context: Context, busyDays: Map<LocalDate, Int>) {
    val today = LocalDate.now()
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        for (offset in 0 until 7) {
            val day = today.plusDays(offset.toLong())
            val isToday = offset == 0
            val count = busyDays[day] ?: 0
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .padding(horizontal = 2.dp)
                    .clickable(actionStartActivity(QuickInkActivity.intent(context, day))),
            ) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(if (isToday) WidgetPrimary else WidgetItemBackground)
                        .cornerRadius(12.dp)
                        .padding(vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = day.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, TurkishLocale).capitalizeTr(),
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = if (isToday) WidgetOnPrimary else WidgetSubtle,
                            textAlign = TextAlign.Center,
                        ),
                        maxLines = 1,
                    )
                    Text(
                        text = day.dayOfMonth.toString(),
                        style = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isToday) WidgetOnPrimary else WidgetText,
                            textAlign = TextAlign.Center,
                        ),
                    )
                    Box(
                        modifier = GlanceModifier
                            .size(width = if (count > 1) 10.dp else 4.dp, height = 4.dp)
                            .cornerRadius(2.dp)
                            .background(
                                when {
                                    count == 0 -> Transparent
                                    isToday -> WidgetOnPrimary
                                    else -> WidgetPrimary
                                },
                            ),
                    ) {}
                }
            }
        }
    }
}

/** Kalem alanı: dokununca bugün için kalem penceresi açılır. */
@Composable
private fun PenPad(context: Context, tall: Boolean) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(if (tall) 64.dp else 48.dp)
            .background(WidgetPaper)
            .cornerRadius(16.dp)
            .padding(horizontal = 14.dp)
            .clickable(actionStartActivity(QuickInkActivity.intent(context, LocalDate.now()))),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_widget_pen),
            contentDescription = null,
            modifier = GlanceModifier.size(20.dp),
        )
        Spacer(GlanceModifier.width(10.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = "Kalemle yaz",
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = WidgetText),
                maxLines = 1,
            )
            if (tall) {
                Text(
                    text = "Bugüne eklenir · başka gün için güne dokun",
                    style = TextStyle(fontSize = 11.sp, color = WidgetSubtle),
                    maxLines = 1,
                )
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
    val overdue = reminder.repeat == RepeatRule.NONE && reminder.triggerAt < System.currentTimeMillis()
    val tint = DayNightColor(day = accent.copy(alpha = 0.10f), night = accent.copy(alpha = 0.18f))

    Column(modifier = GlanceModifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(tint)
                .cornerRadius(14.dp)
                .padding(start = 6.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // ✓: uygulamayı açmadan tamamla.
            Box(
                modifier = GlanceModifier
                    .size(32.dp)
                    .clickable(actionRunCallback<CompleteReminderAction>(actionParametersOf(ReminderIdKey to reminder.id))),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = GlanceModifier
                        .size(20.dp)
                        .background(ColorProvider(accent))
                        .cornerRadius(10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = GlanceModifier.size(14.dp).background(WidgetBackground).cornerRadius(7.dp),
                    ) {}
                }
            }
            Spacer(GlanceModifier.width(6.dp))
            Column(modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity(open))) {
                Text(
                    text = reminder.title,
                    maxLines = 1,
                    style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, color = WidgetText),
                )
                Text(
                    text = if (overdue) "Kaçırıldı · ${formatReminderTime(reminder.triggerAt)}" else formatReminderTime(reminder.triggerAt),
                    maxLines = 1,
                    style = TextStyle(fontSize = 12.sp, color = if (overdue) WidgetError else ColorProvider(accent)),
                )
            }
        }
    }
}
