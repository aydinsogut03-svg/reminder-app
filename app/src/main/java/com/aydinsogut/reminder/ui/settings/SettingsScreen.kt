package com.aydinsogut.reminder.ui.settings

import android.content.Intent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.input.KeyboardType
import com.aydinsogut.reminder.share.ReminderSharing
import com.aydinsogut.reminder.widget.ReminderWidgetReceiver
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aydinsogut.reminder.ai.NanoStatus
import com.aydinsogut.reminder.alarm.NotificationHelper
import com.aydinsogut.reminder.appContainer
import com.aydinsogut.reminder.data.AppSettings
import com.aydinsogut.reminder.data.ThemeMode
import com.aydinsogut.reminder.ui.theme.ReminderPalette

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val container = context.appContainer
    val viewModel = viewModel { SettingsViewModel(container.settings, container.repository, container.gemini, container.speaker) }
    val nanoStatus by viewModel.nanoStatus.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var exactAlarmOk by remember { mutableStateOf(container.scheduler.canScheduleExact()) }
    LifecycleResumeEffect(Unit) {
        exactAlarmOk = container.scheduler.canScheduleExact()
        onPauseOrDispose { }
    }
    var editEmail by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "-"
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "Ayarlar",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp),
            )
        }

        item {
            SettingsGroup("Görünüm") {
                SettingRow(Icons.Rounded.DarkMode, ReminderPalette.color(4), "Tema", "Uygulamanın açık ya da koyu görünümü")
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = settings.themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                        ) { Text(mode.label) }
                    }
                }
            }
        }

        item {
            SettingsGroup("Hatırlatmalar") {
                SettingRow(Icons.Rounded.Snooze, ReminderPalette.color(2), "Erteleme süresi", "Bildirimdeki \"ertele\" düğmesi")
                FlowRow(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AppSettings.SnoozeOptions.forEach { minutes ->
                        FilterChip(
                            selected = settings.snoozeMinutes == minutes,
                            onClick = { viewModel.setSnoozeMinutes(minutes) },
                            label = { Text("$minutes dk") },
                        )
                    }
                }
                Divider()
                SettingRow(
                    icon = Icons.Rounded.Schedule,
                    tint = ReminderPalette.color(5),
                    title = "Varsayılan saat",
                    subtitle = "Takvimden veya saatsiz söylenen hatırlatmalar",
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalIconButton(onClick = { viewModel.setDefaultHour((settings.defaultHour + 23) % 24) }, modifier = Modifier.size(34.dp)) {
                            Text("−", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            "%02d:00".format(settings.defaultHour),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 10.dp),
                        )
                        FilledTonalIconButton(onClick = { viewModel.setDefaultHour((settings.defaultHour + 1) % 24) }, modifier = Modifier.size(34.dp)) {
                            Text("+", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                Divider()
                SettingRow(
                    icon = Icons.Rounded.TaskAlt,
                    tint = ReminderPalette.color(6),
                    title = "Tamamlananları göster",
                    subtitle = "Geçmiş sekmesinde biten hatırlatıcılar",
                ) {
                    Switch(checked = settings.showCompleted, onCheckedChange = viewModel::setShowCompleted)
                }
            }
        }

        item {
            SettingsGroup("Sesle ekleme") {
                SettingRow(
                    icon = Icons.Rounded.Mic,
                    tint = ReminderPalette.color(1),
                    title = "Direkt kaydet",
                    subtitle = "Mikrofon düğmesi, widget veya kısayoldan söylediğinde zaman anlaşıldıysa düzenleme ekranını atla",
                ) {
                    Switch(checked = settings.voiceAutoSave, onCheckedChange = viewModel::setVoiceAutoSave)
                }
            }
        }

        item {
            SettingsGroup("Akıllı özellikler") {
                SettingRow(
                    icon = Icons.Rounded.AutoAwesome,
                    tint = ReminderPalette.color(4),
                    title = "Gemini Nano",
                    subtitle = (nanoStatus?.label ?: "Kontrol ediliyor…") +
                        ". Sesle, kalemle veya paylaşarak eklediğin cümleleri cihaz içinde anlar, bir cümleden birden fazla hatırlatıcı çıkarabilir.",
                ) {
                    Switch(
                        checked = settings.useGemini,
                        onCheckedChange = viewModel::setUseGemini,
                        enabled = nanoStatus != NanoStatus.UNAVAILABLE,
                    )
                }
                if (nanoStatus == NanoStatus.DOWNLOADABLE || nanoStatus == NanoStatus.DOWNLOADING) {
                    Row(Modifier.padding(start = 68.dp, end = 16.dp, bottom = 12.dp)) {
                        FilledTonalButton(
                            onClick = viewModel::downloadGemini,
                            enabled = nanoStatus == NanoStatus.DOWNLOADABLE,
                        ) {
                            Text(if (nanoStatus == NanoStatus.DOWNLOADING) "İndiriliyor…" else "Modeli indir")
                        }
                    }
                }
                Divider()
                SettingRow(
                    icon = Icons.Rounded.RecordVoiceOver,
                    tint = ReminderPalette.color(1),
                    title = "Hatırlatmayı sesli oku",
                    subtitle = "Zamanı gelince başlığı ve notu Türkçe okur. Telefon sessizdeyken okumaz.",
                ) {
                    Switch(checked = settings.speakReminders, onCheckedChange = viewModel::setSpeakReminders)
                }
                Row(Modifier.padding(start = 68.dp, end = 16.dp, bottom = 12.dp)) {
                    FilledTonalButton(onClick = viewModel::testSpeech) {
                        Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Dinle")
                    }
                }
            }
        }

        item {
            SettingsGroup("Kalem") {
                SettingRow(
                    icon = Icons.Rounded.Draw,
                    tint = ReminderPalette.color(2),
                    title = "Kendiliğinden kaydet",
                    subtitle = "Kalemle yazıp bıraktıktan sonra beklenecek süre. Kapalıysa ✓ ile kaydedersin.",
                )
                FlowRow(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AppSettings.InkAutoSaveOptions.forEach { seconds ->
                        FilterChip(
                            selected = settings.inkAutoSaveSeconds == seconds,
                            onClick = { viewModel.setInkAutoSaveSeconds(seconds) },
                            label = { Text(if (seconds == 0) "Kapalı" else "$seconds sn") },
                        )
                    }
                }
                Divider()
                SettingRow(
                    icon = Icons.Rounded.TouchApp,
                    tint = ReminderPalette.color(3),
                    title = "Parmakla da yaz",
                    subtitle = if (settings.fingerDrawing) {
                        "Takvimdeki alana parmakla da yazılır; kaydırmak için alanın dışından kaydır"
                    } else {
                        "Kapalı: takvimde yalnızca S Pen yazar, parmakla kaydırırken çizmez"
                    },
                ) {
                    Switch(checked = settings.fingerDrawing, onCheckedChange = viewModel::setFingerDrawing)
                }
                Divider()
                SettingRow(
                    icon = Icons.Rounded.Widgets,
                    tint = ReminderPalette.color(0),
                    title = "Widget'ı ana ekrana ekle",
                    subtitle = "Bir güne dokun, kalemle yaz",
                    onClick = { requestPinWidget(context) },
                ) { Chevron() }
            }
        }

        item {
            SettingsGroup("Paylaşım") {
                SettingRow(
                    icon = Icons.Rounded.Email,
                    tint = ReminderPalette.color(5),
                    title = "E-posta alıcısı",
                    subtitle = settings.shareEmail.ifBlank { "Belirlenmedi, her seferinde yazarsın" },
                    onClick = { editEmail = true },
                ) { Chevron() }
                Divider()
                SettingRow(
                    icon = Icons.Rounded.Share,
                    tint = ReminderPalette.color(4),
                    title = "Paylaşım imzası",
                    subtitle = "Paylaşılan metnin sonuna \"Hatırlatıcı ile paylaşıldı\" ekler",
                ) {
                    Switch(checked = settings.shareSignature, onCheckedChange = viewModel::setShareSignature)
                }
            }
        }

        item {
            SettingsGroup("Veriler") {
                SettingRow(
                    icon = Icons.Rounded.IosShare,
                    tint = ReminderPalette.color(1),
                    title = "Tümünü dışa aktar",
                    subtitle = "Takvim dosyası (.ics): Google Takvim, Samsung Takvim veya e-postayla yedek",
                    onClick = { viewModel.exportAll { ReminderSharing.exportAll(context, it) } },
                ) { Chevron() }
                Divider()
                SettingRow(
                    icon = Icons.Rounded.DeleteSweep,
                    tint = MaterialTheme.colorScheme.error,
                    title = "Tamamlananları temizle",
                    subtitle = "Biten hatırlatıcıları kalıcı olarak siler",
                    onClick = { confirmClear = true },
                ) { Chevron() }
            }
        }

        item {
            SettingsGroup("Bildirimler") {
                SettingRow(
                    icon = Icons.Rounded.NotificationsActive,
                    tint = ReminderPalette.color(3),
                    title = "Bildirim sesi ve titreşim",
                    subtitle = "Android bildirim ayarlarını açar",
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                .putExtra(Settings.EXTRA_CHANNEL_ID, NotificationHelper.CHANNEL_ID),
                        )
                    },
                ) { Chevron() }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Divider()
                    SettingRow(
                        icon = Icons.Rounded.Alarm,
                        tint = ReminderPalette.color(0),
                        title = "Tam zamanlı alarm",
                        subtitle = if (exactAlarmOk) "İzin verildi, bildirimler dakikasında gelir" else "İzin yok, bildirimler gecikebilir. Açmak için dokun",
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")),
                            )
                        },
                    ) { Chevron() }
                }
            }
        }

        item {
            SettingsGroup("Hakkında") {
                SettingRow(Icons.Rounded.Info, MaterialTheme.colorScheme.onSurfaceVariant, "Hatırlatıcı", "Sürüm $versionName")
            }
        }
    }

    if (editEmail) {
        EmailDialog(
            initial = settings.shareEmail,
            onDismiss = { editEmail = false },
            onSave = {
                viewModel.setShareEmail(it)
                editEmail = false
            },
        )
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Tamamlananlar silinsin mi?") },
            text = { Text("Biten hatırlatıcılar kalıcı olarak silinir.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    viewModel.clearCompleted { count ->
                        Toast.makeText(context, "$count hatırlatıcı silindi", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Sil", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Vazgeç") } },
        )
    }
}

@Composable
private fun EmailDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("E-posta alıcısı") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                placeholder = { Text("ornek@mail.com") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            )
        },
        confirmButton = { TextButton(onClick = { onSave(value) }) { Text("Kaydet") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}

private fun requestPinWidget(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    if (manager.isRequestPinAppWidgetSupported) {
        manager.requestPinAppWidget(ComponentName(context, ReminderWidgetReceiver::class.java), null, null)
    } else {
        Toast.makeText(context, "Ana ekrana uzun bas, Widget'lar'dan Hatırlatıcı'yı seç", Toast.LENGTH_LONG).show()
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
        )
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val rowContent = @Composable {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(38.dp).clip(CircleShape).background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                trailing()
            }
        }
    }
    if (onClick != null) {
        Surface(onClick = onClick, color = Color.Transparent) { rowContent() }
    } else {
        rowContent()
    }
}

@Composable
private fun Divider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 68.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun Chevron() {
    Icon(
        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
