package com.aydinsogut.reminder.ai

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.Ink
import com.google.mlkit.vision.digitalink.RecognitionContext
import com.google.mlkit.vision.digitalink.WritingArea
import kotlinx.coroutines.tasks.await

data class InkPoint(val x: Float, val y: Float, val t: Long)

typealias InkStroke = List<InkPoint>

/** S Pen / parmakla yazılan Türkçe el yazısını cihaz içinde metne çevirir (ML Kit Digital Ink). */
class HandwritingRecognizer {
    private val model: DigitalInkRecognitionModel? by lazy {
        runCatching { DigitalInkRecognitionModelIdentifier.fromLanguageTag("tr") }
            .getOrNull()
            ?.let { DigitalInkRecognitionModel.builder(it).build() }
    }

    private val recognizer: DigitalInkRecognizer? by lazy {
        model?.let { DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(it).build()) }
    }

    private val modelManager = RemoteModelManager.getInstance()

    suspend fun isModelReady(): Boolean {
        val m = model ?: return false
        return runCatching { modelManager.isModelDownloaded(m).await() }.getOrDefault(false)
    }

    /** Türkçe el yazısı modelini (bir kerelik, birkaç MB) indirir. */
    suspend fun ensureModel(): Boolean {
        val m = model ?: return false
        if (isModelReady()) return true
        return runCatching {
            modelManager.download(m, DownloadConditions.Builder().build()).await()
            true
        }.getOrDefault(false)
    }

    /** Çizgileri satırlara ayırıp her satırı ayrı tanır, sonucu tek metin olarak döndürür. */
    suspend fun recognize(strokes: List<InkStroke>, width: Float, height: Float): String {
        val client = recognizer ?: return ""
        val lines = splitIntoLines(strokes.filter { it.isNotEmpty() })
        val texts = mutableListOf<String>()
        for (line in lines) {
            val ink = Ink.builder().apply {
                line.forEach { stroke ->
                    val builder = Ink.Stroke.builder()
                    stroke.forEach { p -> builder.addPoint(Ink.Point.create(p.x, p.y, p.t)) }
                    addStroke(builder.build())
                }
            }.build()
            val context = RecognitionContext.builder()
                .setPreContext(texts.joinToString(" ").takeLast(20))
                .setWritingArea(WritingArea(width, height))
                .build()
            val text = runCatching { client.recognize(ink, context).await().candidates.firstOrNull()?.text }
                .getOrNull()
            if (!text.isNullOrBlank()) texts += text.trim()
        }
        return texts.joinToString(" ")
    }

    companion object {
        /** Yazı sırasına göre ilerler; bir çizgi önceki satırın altından başlıyorsa yeni satır sayılır. */
        fun splitIntoLines(strokes: List<InkStroke>): List<List<InkStroke>> {
            val lines = mutableListOf<MutableList<InkStroke>>()
            var lineTop = 0f
            var lineBottom = 0f
            for (stroke in strokes) {
                val top = stroke.minOf { it.y }
                val bottom = stroke.maxOf { it.y }
                val center = (top + bottom) / 2f
                if (lines.isEmpty() || center > lineBottom + (lineBottom - lineTop) * 0.15f) {
                    lines += mutableListOf(stroke)
                    lineTop = top
                    lineBottom = bottom
                } else {
                    lines.last() += stroke
                    lineTop = minOf(lineTop, top)
                    lineBottom = maxOf(lineBottom, bottom)
                }
            }
            return lines
        }
    }
}
