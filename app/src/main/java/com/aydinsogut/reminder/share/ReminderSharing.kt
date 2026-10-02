package com.aydinsogut.reminder.share

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast
import androidx.core.content.FileProvider
import com.aydinsogut.reminder.data.Reminder
import com.aydinsogut.reminder.data.RepeatRule
import com.aydinsogut.reminder.util.TurkishLocale
import com.aydinsogut.reminder.util.toLocalDateTime
import java.io.File
import java.time.format.DateTimeFormatter

/** Hatırlatıcıyı WhatsApp, e-posta, diğer uygulamalar ya da telefonun takvimiyle paylaşır. */
object ReminderSharing {
    private val dateTimeFormat = DateTimeFormatter.ofPattern("d MMMM yyyy EEEE, HH:mm", TurkishLocale)
    private const val WHATSAPP = "com.whatsapp"
    private const val WHATSAPP_BUSINESS = "com.whatsapp.w4b"

    fun text(reminder: Reminder, signature: Boolean): String = buildString {
        append("📌 ").append(reminder.title).append('\n')
        append("🗓 ").append(reminder.triggerAt.toLocalDateTime().format(dateTimeFormat))
        if (reminder.repeat != RepeatRule.NONE) append("\n🔁 ").append(reminder.repeat.label)
        if (reminder.note.isNotBlank()) append("\n\n").append(reminder.note)
        if (signature) append("\n\n— Hatırlatıcı ile paylaşıldı")
    }

    fun shareAny(context: Context, reminder: Reminder, signature: Boolean) {
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, reminder.title)
            .putExtra(Intent.EXTRA_TEXT, text(reminder, signature))
        start(context, Intent.createChooser(send, "Paylaş"))
    }

    fun shareWhatsApp(context: Context, reminder: Reminder, signature: Boolean) {
        val text = text(reminder, signature)
        for (pkg in listOf(WHATSAPP, WHATSAPP_BUSINESS)) {
            val intent = Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .setPackage(pkg)
                .putExtra(Intent.EXTRA_TEXT, text)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
                return
            } catch (_: ActivityNotFoundException) {
                // Sıradaki paketi dene.
            }
        }
        Toast.makeText(context, "WhatsApp bulunamadı, diğer uygulamalarla paylaşılıyor", Toast.LENGTH_SHORT).show()
        shareAny(context, reminder, signature)
    }

    /** E-posta: metin gövdede, takvim dosyası (.ics) ekte; alıcı ayarlardaysa önceden yazılır. */
    fun shareEmail(context: Context, reminder: Reminder, signature: Boolean, recipient: String) {
        val uri = writeIcs(context, "hatirlatici-${reminder.id}.ics", listOf(reminder))
        val intent = Intent(Intent.ACTION_SEND)
            .setType("text/calendar")
            .putExtra(Intent.EXTRA_SUBJECT, "Hatırlatma: ${reminder.title}")
            .putExtra(Intent.EXTRA_TEXT, text(reminder, signature))
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (recipient.isNotBlank()) intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
        intent.clipData = ClipData.newRawUri("", uri)
        // Yalnızca e-posta uygulamaları listelensin.
        intent.selector = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            intent.selector = null
            start(context, Intent.createChooser(intent, "E-postayla gönder"))
        }
    }

    /** Telefonun takvim uygulamasında, bilgileri doldurulmuş yeni etkinlik ekranını açar. */
    fun addToCalendar(context: Context, reminder: Reminder) {
        val intent = Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, reminder.triggerAt)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, reminder.triggerAt + IcsBuilder.EVENT_MINUTES * 60_000)
            .putExtra(CalendarContract.Events.TITLE, reminder.title)
            .putExtra(CalendarContract.Events.DESCRIPTION, reminder.note)
            .putExtra(CalendarContract.Events.HAS_ALARM, 1)
        IcsBuilder.rrule(reminder.repeat)?.let { intent.putExtra(CalendarContract.Events.RRULE, it) }
        start(context, intent)
    }

    /** Tüm hatırlatıcıları tek bir .ics dosyası olarak dışa aktarır (yedek ya da başka takvime aktarma). */
    fun exportAll(context: Context, reminders: List<Reminder>) {
        val uri = writeIcs(context, "hatirlaticilar.ics", reminders)
        val intent = Intent(Intent.ACTION_SEND)
            .setType("text/calendar")
            .putExtra(Intent.EXTRA_SUBJECT, "Hatırlatıcılar (${reminders.size})")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.clipData = ClipData.newRawUri("", uri)
        start(context, Intent.createChooser(intent, "Dışa aktar"))
    }

    private fun writeIcs(context: Context, name: String, reminders: List<Reminder>): Uri {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, name)
        file.writeText(IcsBuilder.build(reminders))
        return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }

    private fun start(context: Context, intent: Intent) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "Bunu açabilecek bir uygulama bulunamadı", Toast.LENGTH_SHORT).show()
        }
    }
}
