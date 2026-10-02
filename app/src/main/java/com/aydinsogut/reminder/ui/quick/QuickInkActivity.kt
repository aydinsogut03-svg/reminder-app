package com.aydinsogut.reminder.ui.quick

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.aydinsogut.reminder.appContainer
import com.aydinsogut.reminder.data.AppSettings
import com.aydinsogut.reminder.data.ThemeMode
import com.aydinsogut.reminder.ui.calendar.DayInkCard
import com.aydinsogut.reminder.ui.theme.ReminderTheme
import com.aydinsogut.reminder.util.TimeFormats
import com.aydinsogut.reminder.util.TurkishLocale
import com.aydinsogut.reminder.util.capitalizeTr
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle

/**
 * Widget'tan açılan küçük kalem penceresi: seçilen güne yazılanı varsayılan saatle
 * (ya da yazıdaki saatle) ekler ve kapanır. Ana uygulamayı açmaz.
 */
class QuickInkActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialDate = intent.getStringExtra(EXTRA_DATE)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: LocalDate.now()
        val container = appContainer

        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
            val dark = when (settings.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                else -> isSystemInDarkTheme()
            }
            var date by rememberSaveable { mutableStateOf(initialDate) }
            ReminderTheme(darkTheme = dark) {
                QuickInkSheet(
                    date = date,
                    onDateChange = { date = it },
                    defaultTime = settings.defaultTime,
                    autoSaveSeconds = settings.inkAutoSaveSeconds,
                    onSubmit = { text -> save(text, date) },
                    onDismiss = ::finish,
                )
            }
        }
    }

    private fun save(text: String, date: LocalDate) {
        lifecycleScope.launch {
            val saved = appContainer.repository.addFromText(text, date)
            if (saved != null) {
                val whenText = java.time.Instant.ofEpochMilli(saved.triggerAt)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime()
                    .format(TimeFormats.short)
                Toast.makeText(this@QuickInkActivity, "Eklendi: ${saved.title} · $whenText", Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }

    companion object {
        const val EXTRA_DATE = "com.aydinsogut.reminder.extra.DATE"

        fun intent(context: Context, date: LocalDate): Intent =
            Intent(context, QuickInkActivity::class.java)
                .setData(Uri.parse("reminder://ink/$date"))
                .putExtra(EXTRA_DATE, date.toString())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
}

@Composable
private fun QuickInkSheet(
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    defaultTime: java.time.LocalTime,
    autoSaveSeconds: Int,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.32f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                // Kart içindeki dokunuşlar pencereyi kapatmasın.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        ) {
            Column(
                modifier = Modifier.navigationBarsPadding().padding(top = 10.dp, bottom = 16.dp),
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 40.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                )
                Spacer(Modifier.height(12.dp))
                DayStrip(selected = date, onSelect = onDateChange)
                Spacer(Modifier.height(12.dp))
                DayInkCard(
                    date = date,
                    defaultTime = defaultTime,
                    autoSaveSeconds = autoSaveSeconds,
                    onSubmit = onSubmit,
                    padHeight = 230.dp,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        }
    }
}

/** Bugünden başlayan iki haftalık gün şeridi. */
@Composable
private fun DayStrip(selected: LocalDate, onSelect: (LocalDate) -> Unit) {
    val today = LocalDate.now()
    val days = remember(today) { (0L until 14L).map { today.plusDays(it) } }
    val colors = MaterialTheme.colorScheme
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(days, key = { it.toEpochDay() }) { day ->
            val isSelected = day == selected
            Column(
                modifier = Modifier
                    .width(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) colors.primary else colors.surfaceContainerLow)
                    .clickable { onSelect(day) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = when (day) {
                        today -> "Bugün"
                        today.plusDays(1) -> "Yarın"
                        else -> day.dayOfWeek.getDisplayName(TextStyle.SHORT, TurkishLocale).capitalizeTr()
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) colors.onPrimary else colors.onSurfaceVariant,
                    maxLines = 1,
                )
                Text(
                    text = day.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) colors.onPrimary else colors.onSurface,
                )
            }
        }
    }
}
