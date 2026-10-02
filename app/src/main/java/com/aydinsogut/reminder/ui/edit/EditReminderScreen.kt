package com.aydinsogut.reminder.ui.edit

import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Weekend
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aydinsogut.reminder.appContainer
import com.aydinsogut.reminder.ui.components.ReminderActionsSheet
import com.aydinsogut.reminder.data.RepeatRule
import com.aydinsogut.reminder.ui.theme.ReminderPalette
import com.aydinsogut.reminder.util.TimeFormats
import com.aydinsogut.reminder.util.formatReminderTime
import com.aydinsogut.reminder.util.toEpochMillis
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditReminderScreen(
    id: Long,
    initialDate: LocalDate?,
    onBack: () -> Unit,
    initialText: String? = null,
    startWithVoice: Boolean = false,
) {
    val context = LocalContext.current
    val container = context.appContainer
    val viewModel = viewModel(key = "edit-$id-$initialDate-${initialText.hashCode()}") {
        EditReminderViewModel(container.repository, container.settings, container.understanding, id, initialDate, initialText)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (spoken != null) {
            viewModel.onSpokenText(spoken, quickSave = startWithVoice) { message ->
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                onBack()
            }
        }
    }
    val startVoice = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
            .putExtra(RecognizerIntent.EXTRA_PROMPT, "Ne hatırlatayım? Örneğin: yarın saat 9'da annemi ara")
        try {
            voiceLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Bu telefonda ses tanıma bulunamadı", Toast.LENGTH_LONG).show()
        }
    }
    var voiceStarted by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (startWithVoice && !voiceStarted) {
            voiceStarted = true
            startVoice()
        }
    }
    val accent = ReminderPalette.color(state.colorIndex)

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showShare by remember { mutableStateOf(false) }

    if (showShare) {
        ReminderActionsSheet(reminder = state.asReminder(), onDismiss = { showShare = false })
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (state.isNew) "Yeni hatırlatıcı" else "Düzenle") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.Close, contentDescription = "Kapat")
                    }
                },
                actions = {
                    if (state.canSave) {
                        IconButton(onClick = { showShare = true }) {
                            Icon(Icons.Rounded.Share, contentDescription = "Paylaş")
                        }
                    }
                    if (!state.isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Sil", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Button(
                    onClick = { viewModel.save(onBack) },
                    enabled = state.canSave,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(16.dp)
                        .height(56.dp),
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Kaydet", style = MaterialTheme.typography.titleMedium)
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Başlık ve not kartı
            Surface(
                shape = MaterialTheme.shapes.large,
                color = lerp(MaterialTheme.colorScheme.surface, accent, 0.07f),
            ) {
                Row {
                    Box(
                        Modifier
                            .padding(start = 16.dp, top = 22.dp)
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(accent),
                    )
                    Column {
                        TextField(
                            value = state.title,
                            onValueChange = viewModel::onTitleChange,
                            placeholder = { Text("Ne hatırlatayım?", style = MaterialTheme.typography.titleLarge) },
                            textStyle = MaterialTheme.typography.titleLarge,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            colors = transparentFieldColors(),
                            trailingIcon = {
                                IconButton(onClick = startVoice) {
                                    Icon(Icons.Rounded.Mic, contentDescription = "Sesle söyle", tint = accent)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        TextField(
                            value = state.note,
                            onValueChange = viewModel::onNoteChange,
                            placeholder = { Text("Not ekle (isteğe bağlı)") },
                            textStyle = MaterialTheme.typography.bodyLarge,
                            minLines = 2,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            colors = transparentFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            if (state.isThinking) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Anlaşılıyor…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (state.usedGemini) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Gemini Nano ile anlaşıldı", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }

            if (state.extraReminders.isNotEmpty()) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Column(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp, end = 4.dp)) {
                        Text(
                            "Kaydedince bunlar da eklenecek",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        state.extraReminders.forEachIndexed { index, extra ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(extra.title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    extra.dateTime?.let {
                                        Text(
                                            formatReminderTime(it.toEpochMillis()),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                        )
                                    }
                                }
                                IconButton(onClick = { viewModel.removeExtra(index) }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Çıkar", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }
                    }
                }
            }

            SectionLabel("Ne zaman?")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PickerCard(
                    icon = Icons.Rounded.CalendarMonth,
                    label = "Tarih",
                    value = state.date.format(TimeFormats.dateShort),
                    accent = accent,
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1.3f),
                )
                PickerCard(
                    icon = Icons.Rounded.Schedule,
                    label = "Saat",
                    value = state.time.format(TimeFormats.time),
                    accent = accent,
                    onClick = { showTimePicker = true },
                    modifier = Modifier.weight(1f),
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                quickPresets().forEach { preset ->
                    AssistChip(
                        onClick = { viewModel.onDateTimeChange(preset.dateTime()) },
                        label = { Text(preset.label) },
                        leadingIcon = {
                            Icon(preset.icon, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize))
                        },
                    )
                }
            }
            if (state.isInPast) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Seçilen zaman geçmişte, bildirim gelmez.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            SectionLabel("Tekrar")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RepeatRule.entries.forEach { rule ->
                    FilterChip(
                        selected = state.repeat == rule,
                        onClick = { viewModel.onRepeatChange(rule) },
                        label = { Text(rule.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent.copy(alpha = 0.18f),
                            selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    )
                }
            }

            SectionLabel("Renk")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ReminderPalette.colors.forEachIndexed { index, color ->
                    ColorDot(
                        color = color,
                        selected = state.colorIndex == index,
                        onClick = { viewModel.onColorChange(index) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        viewModel.onDateChange(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showDatePicker = false
                }) { Text("Tamam") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("İptal") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showTimePicker) {
        val pickerState = rememberTimePickerState(
            initialHour = state.time.hour,
            initialMinute = state.time.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onTimeChange(LocalTime.of(pickerState.hour, pickerState.minute))
                    showTimePicker = false
                }) { Text("Tamam") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("İptal") }
            },
            text = { TimePicker(state = pickerState) },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Silinsin mi?") },
            text = { Text("\"${state.title}\" kalıcı olarak silinecek.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete(onBack)
                }) { Text("Sil") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Vazgeç") }
            },
        )
    }
}

@Composable
private fun transparentFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
)

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
    )
}

@Composable
private fun PickerCard(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = lerp(MaterialTheme.colorScheme.surface, accent, 0.06f),
        modifier = modifier,
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(38.dp).clip(CircleShape).background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ColorDot(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .then(
                if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f), CircleShape) else Modifier,
            )
            .padding(4.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Icon(Icons.Rounded.Check, contentDescription = "Seçili", tint = Color.White, modifier = Modifier.size(18.dp))
    }
}

private class QuickPreset(val label: String, val icon: ImageVector, val dateTime: () -> LocalDateTime)

private fun quickPresets(): List<QuickPreset> = listOf(
    QuickPreset("1 saat sonra", Icons.Rounded.HourglassTop) {
        val t = LocalDateTime.now().plusHours(1)
        t.withMinute((t.minute / 5) * 5)
    },
    QuickPreset("Bu akşam", Icons.Rounded.Bedtime) {
        val evening = LocalDate.now().atTime(20, 0)
        if (evening.isAfter(LocalDateTime.now())) evening else evening.plusDays(1)
    },
    QuickPreset("Yarın sabah", Icons.Rounded.WbSunny) {
        LocalDate.now().plusDays(1).atTime(9, 0)
    },
    QuickPreset("Hafta sonu", Icons.Rounded.Weekend) {
        LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SATURDAY)).atTime(10, 0)
    },
)
