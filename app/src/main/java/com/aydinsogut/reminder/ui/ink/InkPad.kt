package com.aydinsogut.reminder.ui.ink

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.aydinsogut.reminder.ai.HandwritingRecognizer
import com.aydinsogut.reminder.ai.InkPoint
import com.aydinsogut.reminder.ai.InkStroke

enum class InkModelState { CHECKING, DOWNLOADING, READY, FAILED }

/** Türkçe el yazısı modelinin durumunu izler; yoksa bir kerelik indirir. */
@Composable
fun rememberInkModelState(recognizer: HandwritingRecognizer): State<InkModelState> {
    val state = remember { mutableStateOf(InkModelState.CHECKING) }
    LaunchedEffect(recognizer) {
        state.value = if (recognizer.isModelReady()) {
            InkModelState.READY
        } else {
            state.value = InkModelState.DOWNLOADING
            if (recognizer.ensureModel()) InkModelState.READY else InkModelState.FAILED
        }
    }
    return state
}

/**
 * Çizgili yazı alanı. S Pen bir kez dokunduktan sonra parmak dokunuşları yazı sayılmaz
 * (avuç içi); bu dokunuşlar tüketilmediği için içinde bulunduğu liste kaydırılabilir kalır.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun InkCanvas(
    strokes: SnapshotStateList<InkStroke>,
    onSize: (IntSize) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "S Pen ya da parmağınla buraya yaz",
    lineSpacing: Dp = 56.dp,
    inkColor: Color = MaterialTheme.colorScheme.onSurface,
    lineColor: Color = MaterialTheme.colorScheme.outlineVariant,
    onStrokeStart: () -> Unit = {},
) {
    var current by remember { mutableStateOf<InkStroke?>(null) }
    var stylusSeen by remember { mutableStateOf(false) }

    Box(modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged(onSize)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val isStylus = down.type == PointerType.Stylus || down.type == PointerType.Eraser
                        if (stylusSeen && !isStylus) return@awaitEachGesture
                        if (isStylus) stylusSeen = true
                        onStrokeStart()
                        val points = mutableListOf(InkPoint(down.position.x, down.position.y, down.uptimeMillis))
                        current = points.toList()
                        down.consume()
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            change.historical.forEach { h ->
                                points += InkPoint(h.position.x, h.position.y, h.uptimeMillis)
                            }
                            points += InkPoint(change.position.x, change.position.y, change.uptimeMillis)
                            change.consume()
                            current = points.toList()
                            if (!change.pressed) break
                        }
                        current = null
                        strokes += points.toList()
                    }
                },
        ) {
            val spacing = lineSpacing.toPx()
            var y = spacing
            while (y < size.height) {
                drawLine(lineColor, Offset(24f, y), Offset(size.width - 24f, y), strokeWidth = 1.dp.toPx())
                y += spacing
            }
            val style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            strokes.forEach { drawPath(it.toPath(), inkColor, style = style) }
            current?.let { drawPath(it.toPath(), inkColor, style = style) }
        }
        if (strokes.isEmpty() && current == null) {
            Row(
                modifier = Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Draw, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                Spacer(Modifier.width(8.dp))
                Text(
                    placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }
    }
}

private fun InkStroke.toPath(): Path = Path().apply {
    if (isEmpty()) return@apply
    moveTo(first().x, first().y)
    if (size == 1) {
        lineTo(first().x + 0.1f, first().y + 0.1f)
        return@apply
    }
    for (i in 1 until size) {
        val prev = this@toPath[i - 1]
        val point = this@toPath[i]
        quadraticTo(prev.x, prev.y, (prev.x + point.x) / 2f, (prev.y + point.y) / 2f)
    }
    lineTo(last().x, last().y)
}
