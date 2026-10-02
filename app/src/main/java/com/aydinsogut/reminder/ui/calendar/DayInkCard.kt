package com.aydinsogut.reminder.ui.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.aydinsogut.reminder.ai.InkStroke
import com.aydinsogut.reminder.appContainer
import com.aydinsogut.reminder.ui.ink.InkCanvas
import com.aydinsogut.reminder.ui.ink.InkModelState
import com.aydinsogut.reminder.ui.ink.rememberInkModelState
import com.aydinsogut.reminder.ui.theme.soft
import com.aydinsogut.reminder.util.TimeFormats
import com.aydinsogut.reminder.util.TurkishReminderParser
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime

/** Kalemle yazdıktan sonra kendiliğinden kaydetmeden önce beklenen süre. */
private const val AUTO_SAVE_MS = 1_800

/**
 * Takvimde hep açık duran yazı alanı: kalemle yaz, yazı tanınır ve kısa bir bekleyişten
 * sonra seçili güne kendiliğinden kaydedilir (onay düğmesiyle hemen de kaydedilebilir).
 */
@Composable
fun DayInkCard(
    date: LocalDate,
    defaultTime: LocalTime,
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val recognizer = LocalContext.current.appContainer.handwriting
    val modelState by rememberInkModelState(recognizer)
    val strokes = remember { mutableStateListOf<InkStroke>() }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var text by remember { mutableStateOf("") }
    var recognizing by remember { mutableStateOf(false) }
    var penDowns by remember { mutableIntStateOf(0) }
    val countdown = remember { Animatable(0f) }

    fun submit() {
        val value = text.trim()
        if (value.isEmpty()) return
        strokes.clear()
        text = ""
        onSubmit(value)
    }

    LaunchedEffect(strokes.size, penDowns, modelState) {
        countdown.snapTo(0f)
        if (strokes.isEmpty()) {
            text = ""
            return@LaunchedEffect
        }
        if (modelState != InkModelState.READY) return@LaunchedEffect
        delay(650)
        recognizing = true
        text = recognizer.recognize(strokes.toList(), canvasSize.width.toFloat(), canvasSize.height.toFloat())
        recognizing = false
        if (text.isBlank()) return@LaunchedEffect
        countdown.animateTo(1f, tween(AUTO_SAVE_MS, easing = LinearEasing))
        submit()
    }

    val colors = MaterialTheme.colorScheme
    val paper = lerp(colors.surface, colors.tertiary, 0.06f)
    val preview = remember(text, date, defaultTime) {
        text.takeIf { it.isNotBlank() }?.let { TurkishReminderParser.parseForDay(it, date, defaultTime) }
    }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = paper,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(34.dp).clip(CircleShape).background(colors.tertiary.soft(0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Draw, contentDescription = null, tint = colors.tertiary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Bu güne yaz", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = when (modelState) {
                            InkModelState.DOWNLOADING -> "El yazısı modeli indiriliyor (bir kerelik)…"
                            InkModelState.FAILED -> "Model indirilemedi, internet bağlantını kontrol et"
                            else -> "${date.format(TimeFormats.dayTitle)} · yazınca kendiliğinden eklenir"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                if (strokes.isNotEmpty()) {
                    IconButton(onClick = { strokes.removeAt(strokes.lastIndex) }) {
                        Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = "Son çizgiyi geri al", tint = colors.onSurfaceVariant)
                    }
                    IconButton(onClick = { strokes.clear() }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Temizle", tint = colors.onSurfaceVariant)
                    }
                }
            }

            InkCanvas(
                strokes = strokes,
                onSize = { canvasSize = it },
                placeholder = "Kalemle buraya yaz, ör. \"10'da dişçi\"",
                lineSpacing = 50.dp,
                lineColor = colors.tertiary.soft(0.18f),
                onStrokeStart = { penDowns++ },
                modifier = Modifier.fillMaxWidth().height(170.dp).padding(end = 8.dp),
            )

            AnimatedVisibility(
                visible = preview != null || recognizing,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, end = 8.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.surface)
                        .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = preview?.title ?: "Okunuyor…",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        preview?.dateTime?.let {
                            Text(
                                text = "${it.format(TimeFormats.dateShort)} · ${it.format(TimeFormats.time)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.primary,
                            )
                        }
                    }
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { countdown.value },
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 2.5.dp,
                            trackColor = colors.primary.soft(0.12f),
                        )
                        FilledIconButton(
                            onClick = ::submit,
                            enabled = preview != null && !recognizing,
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = colors.primary),
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(Icons.Rounded.Check, contentDescription = "Kaydet")
                        }
                    }
                }
            }
        }
    }
}
