package com.aydinsogut.reminder.ui.ink

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.aydinsogut.reminder.ai.InkPoint
import com.aydinsogut.reminder.ai.InkStroke
import com.aydinsogut.reminder.appContainer
import kotlinx.coroutines.delay

/** S Pen ya da parmakla el yazısı; yazılanı metne çevirip hatırlatıcıya dönüştürür. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun InkScreen(
    onClose: () -> Unit,
    onDone: (String) -> Unit,
) {
    val recognizer = LocalContext.current.appContainer.handwriting
    val strokes = remember { mutableStateListOf<InkStroke>() }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var text by remember { mutableStateOf("") }
    var recognizing by remember { mutableStateOf(false) }
    val modelState by rememberInkModelState(recognizer)

    LaunchedEffect(strokes.size, modelState) {
        if (strokes.isEmpty()) {
            text = ""
            return@LaunchedEffect
        }
        if (modelState != InkModelState.READY) return@LaunchedEffect
        delay(700)
        recognizing = true
        text = recognizer.recognize(strokes.toList(), canvasSize.width.toFloat(), canvasSize.height.toFloat())
        recognizing = false
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Kalemle yaz") },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, contentDescription = "Kapat") }
                },
                actions = {
                    IconButton(onClick = { if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex) }, enabled = strokes.isNotEmpty()) {
                        Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = "Geri al")
                    }
                    IconButton(onClick = { strokes.clear() }, enabled = strokes.isNotEmpty()) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = "Temizle")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Algılanan yazı") },
                placeholder = {
                    Text(
                        when (modelState) {
                            InkModelState.CHECKING -> "Hazırlanıyor…"
                            InkModelState.DOWNLOADING -> "Türkçe el yazısı modeli indiriliyor (bir kerelik)…"
                            InkModelState.FAILED -> "Model indirilemedi, internet bağlantını kontrol et"
                            InkModelState.READY -> "Aşağıya yaz, ör. \"Yarın 10'da dişçi\""
                        },
                    )
                },
                trailingIcon = {
                    if (recognizing || modelState == InkModelState.DOWNLOADING) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                InkCanvas(
                    strokes = strokes,
                    onSize = { canvasSize = it },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Button(
                onClick = { onDone(text.trim()) },
                enabled = text.isNotBlank() && !recognizing,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 0.dp),
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Hatırlatıcı yap", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}
