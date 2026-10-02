package com.aydinsogut.reminder.data

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.aydinsogut.reminder.alarm.AlarmScheduler
import com.aydinsogut.reminder.alarm.NotificationHelper
import com.aydinsogut.reminder.alarm.ReminderSpeaker
import com.aydinsogut.reminder.ui.theme.ReminderPalette
import com.aydinsogut.reminder.util.TurkishReminderParser
import com.aydinsogut.reminder.util.startOfDayMillis
import com.aydinsogut.reminder.util.toEpochMillis
import com.aydinsogut.reminder.widget.ReminderWidget
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Hatırlatıcılarla ilgili tek giriş noktası: veritabanı, alarmlar, bildirimler ve
 * widget güncellemesi burada birlikte yönetilir.
 */
class ReminderRepository(
    private val context: Context,
    private val dao: ReminderDao,
    private val scheduler: AlarmScheduler,
    private val notifications: NotificationHelper,
    private val settings: SettingsRepository,
    private val speaker: ReminderSpeaker,
) {
    fun observeAll(): Flow<List<Reminder>> = dao.observeAll()

    suspend fun get(id: Long): Reminder? = dao.getById(id)

    /** Bugünün başından itibaren tamamlanmamış hatırlatıcılar (widget için). */
    suspend fun upcoming(limit: Int): List<Reminder> =
        dao.getUpcoming(LocalDate.now().startOfDayMillis(), limit)

    /**
     * Kalemle bir güne yazılan metni kaydeder: yazıda saat varsa o saate, yoksa varsayılan
     * saate kurulur. Kaydedileni döndürür (geri almak için).
     */
    suspend fun addFromText(text: String, date: LocalDate): Reminder? {
        if (text.isBlank()) return null
        val parsed = TurkishReminderParser.parseForDay(text, date, settings.current().defaultTime)
        val dateTime = parsed.dateTime ?: return null
        val reminder = Reminder(
            title = parsed.title,
            triggerAt = dateTime.toEpochMillis(),
            colorIndex = (date.dayOfMonth + parsed.title.length).mod(ReminderPalette.colors.size),
        )
        return reminder.copy(id = save(reminder))
    }

    suspend fun save(reminder: Reminder): Long {
        val id = if (reminder.id == 0L) {
            dao.insert(reminder)
        } else {
            dao.update(reminder)
            reminder.id
        }
        scheduler.schedule(reminder.copy(id = id))
        refreshWidget()
        return id
    }

    suspend fun delete(reminder: Reminder) {
        scheduler.cancel(reminder.id)
        notifications.cancel(reminder.id)
        dao.delete(reminder)
        refreshWidget()
    }

    /** Silmeyi geri almak için hatırlatıcıyı aynı kimlikle geri ekler. */
    suspend fun restore(reminder: Reminder) {
        dao.insert(reminder)
        scheduler.schedule(reminder)
        refreshWidget()
    }

    suspend fun setDone(id: Long, done: Boolean) {
        dao.setDone(id, done)
        val reminder = dao.getById(id) ?: return
        if (done) {
            scheduler.cancel(id)
            notifications.cancel(id)
        } else {
            scheduler.schedule(reminder)
        }
        refreshWidget()
    }

    /** Alarm çaldığında: bildirimi göster, tekrarlıysa bir sonrakini kur. */
    suspend fun onAlarmFired(id: Long) {
        val reminder = dao.getById(id) ?: return
        if (reminder.isDone) return
        val prefs = settings.current()
        notifications.show(reminder, prefs.snoozeMinutes)
        if (prefs.speakReminders && speaker.canSpeakNow()) speaker.speak(reminder)
        val now = System.currentTimeMillis()
        if (reminder.repeat != RepeatRule.NONE && reminder.triggerAt <= now) {
            save(reminder.copy(triggerAt = reminder.nextTriggerAfter(now)))
        } else {
            refreshWidget()
        }
    }

    /** Bildirimdeki "Tamamlandı" düğmesi. Tekrarlayanlar sadece kapatılır, seri devam eder. */
    suspend fun completeFromNotification(id: Long) {
        val reminder = dao.getById(id) ?: return
        if (reminder.repeat == RepeatRule.NONE) {
            setDone(id, true)
        } else {
            notifications.cancel(id)
        }
    }

    suspend fun snooze(id: Long) {
        val minutes = settings.current().snoozeMinutes
        notifications.cancel(id)
        scheduler.scheduleSnooze(id, System.currentTimeMillis() + minutes * 60_000L)
    }

    /** Yeniden başlatma veya izin değişikliğinden sonra tüm alarmları yeniden kurar. */
    suspend fun rescheduleAll() {
        val now = System.currentTimeMillis()
        dao.getActive().forEach { reminder ->
            if (reminder.repeat != RepeatRule.NONE && reminder.triggerAt <= now) {
                val updated = reminder.copy(triggerAt = reminder.nextTriggerAfter(now))
                dao.update(updated)
                scheduler.schedule(updated)
            } else {
                scheduler.schedule(reminder)
            }
        }
        refreshWidget()
    }

    private suspend fun refreshWidget() {
        runCatching { ReminderWidget().updateAll(context) }
    }
}
