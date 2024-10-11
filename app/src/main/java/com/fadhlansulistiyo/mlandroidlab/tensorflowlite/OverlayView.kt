package com.fadhlansulistiyo.mlandroidlab.tensorflowlite

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.fadhlansulistiyo.mlandroidlab.R
import org.tensorflow.lite.task.gms.vision.detector.Detection
import java.text.NumberFormat
import kotlin.math.max

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val boxPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.bounding_box_color)
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }

    private val textBackgroundPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
        textSize = 50f
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        textSize = 50f
    }

    private var results: List<Detection> = emptyList()
    private var scaleFactor = 1f
    private var widthScaleFactor = 1f
    private var heightScaleFactor = 1f
    private val bounds = Rect()

    fun setResults(
        detectionResults: List<Detection>,
        imageHeight: Int,
        imageWidth: Int
    ) {
        results = detectionResults

        // Calculate scaling factors to match bounding boxes with displayed image size.
        scaleFactor = max(width.toFloat() / imageWidth, height.toFloat() / imageHeight)
        widthScaleFactor = width.toFloat() / imageWidth
        heightScaleFactor = height.toFloat() / imageHeight

        invalidate() // Trigger a redraw when new results are set.
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        results.forEach { result ->
            drawBoundingBox(canvas, result)
            drawLabel(canvas, result)
        }
    }

    private fun drawBoundingBox(canvas: Canvas, result: Detection) {
        val boundingBox = result.boundingBox
        val left = boundingBox.left * widthScaleFactor
        val top = boundingBox.top * heightScaleFactor
        val right = boundingBox.right * widthScaleFactor
        val bottom = boundingBox.bottom * heightScaleFactor

        val drawableRect = RectF(left, top, right, bottom)
        canvas.drawRect(drawableRect, boxPaint)
    }

    private fun drawLabel(canvas: Canvas, result: Detection) {
        val labelText = "${result.categories[0].label} " +
                NumberFormat.getPercentInstance().format(result.categories[0].score)

        textBackgroundPaint.getTextBounds(labelText, 0, labelText.length, bounds)

        val left = result.boundingBox.left * widthScaleFactor
        val top = result.boundingBox.top * heightScaleFactor

        val textWidth = bounds.width()
        val textHeight = bounds.height()

        canvas.drawRect(
            left,
            top,
            left + textWidth + BOUNDING_RECT_TEXT_PADDING,
            top + textHeight + BOUNDING_RECT_TEXT_PADDING,
            textBackgroundPaint
        )

        canvas.drawText(labelText, left, top + textHeight, textPaint)
    }

    fun clear() {
        results = emptyList()
        invalidate() // Force a redraw with no results
    }

    companion object {
        private const val BOUNDING_RECT_TEXT_PADDING = 8
    }
}
