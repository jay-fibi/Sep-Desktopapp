package com.example.camerameasure.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camerameasure.measure.Units
import com.example.camerameasure.model.NormPoint
import com.example.camerameasure.ui.theme.CalibrationColor
import com.example.camerameasure.ui.theme.MeasurementColors
import kotlin.math.roundToInt

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 8f

/**
 * The captured photo with its measuring overlay: pinch to zoom, drag to pan, tap to place the
 * two ends of the reference object (calibration) or of the distance to measure.
 *
 * The photo and the overlay are drawn on one canvas in screen coordinates, so the mapping
 * between a finger position and the normalised photo coordinates stays exact at any zoom level.
 */
@OptIn(ExperimentalTextApi::class)
@Composable
fun PhotoMeasurementCanvas(
    bitmap: Bitmap,
    viewModel: MeasureViewModel,
    modifier: Modifier = Modifier
) {
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    val aspectRatio = remember(bitmap) { bitmap.width.toFloat() / bitmap.height.toFloat() }
    val textMeasurer = rememberTextMeasurer()

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var scale by remember(bitmap) { mutableFloatStateOf(MIN_SCALE) }
    var offset by remember(bitmap) { mutableStateOf(Offset.Zero) }

    fun toScreen(point: NormPoint): Offset = Offset(
        x = offset.x + point.x * canvasSize.width * scale,
        y = offset.y + point.y * canvasSize.height * scale
    )

    fun toPhoto(position: Offset): NormPoint? {
        if (canvasSize.width == 0 || canvasSize.height == 0) return null
        return NormPoint(
            x = (position.x - offset.x) / (canvasSize.width * scale),
            y = (position.y - offset.y) / (canvasSize.height * scale)
        )
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .aspectRatio(aspectRatio)
                .onSizeChanged { canvasSize = it }
                .pointerInput(bitmap) {
                    detectTapGestures { position ->
                        val point = toPhoto(position) ?: return@detectTapGestures
                        if (point.x in 0f..1f && point.y in 0f..1f) {
                            viewModel.onPhotoTap(point)
                        }
                    }
                }
                .pointerInput(bitmap) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                        scale = newScale
                        offset = clampOffset(offset + pan, canvasSize, newScale)
                    }
                }
        ) {
            drawPhoto(imageBitmap, offset, scale)

            viewModel.measurements.forEachIndexed { index, measurement ->
                val color = MeasurementColors[index % MeasurementColors.size]
                val start = toScreen(measurement.start)
                val end = toScreen(measurement.end)
                drawSegment(start, end, color, dashed = false)
                drawLabel(
                    measurer = textMeasurer,
                    text = "${index + 1}. " + Units.format(measurement.millimeters, viewModel.unit),
                    anchor = midpoint(start, end),
                    color = color
                )
            }

            viewModel.calibration?.let { calibration ->
                val start = toScreen(calibration.start)
                val end = toScreen(calibration.end)
                drawSegment(start, end, CalibrationColor, dashed = true)
                drawLabel(
                    measurer = textMeasurer,
                    text = calibration.referenceLabel,
                    anchor = midpoint(start, end),
                    color = CalibrationColor
                )
            }

            viewModel.pendingCalibration.forEach { drawPendingMarker(toScreen(it), CalibrationColor) }
            val pendingColor = MeasurementColors[viewModel.measurements.size % MeasurementColors.size]
            viewModel.pendingMeasurement.forEach { drawPendingMarker(toScreen(it), pendingColor) }
        }
    }
}

/** Keeps the (already zoomed) photo covering the viewport. */
private fun clampOffset(candidate: Offset, canvasSize: IntSize, scale: Float): Offset {
    if (canvasSize.width == 0 || canvasSize.height == 0) return Offset.Zero
    val minX = canvasSize.width - canvasSize.width * scale
    val minY = canvasSize.height - canvasSize.height * scale
    return Offset(candidate.x.coerceIn(minX, 0f), candidate.y.coerceIn(minY, 0f))
}

private fun midpoint(start: Offset, end: Offset): Offset =
    Offset((start.x + end.x) / 2f, (start.y + end.y) / 2f)

private fun DrawScope.drawPhoto(image: ImageBitmap, offset: Offset, scale: Float) {
    drawImage(
        image = image,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(image.width, image.height),
        dstOffset = IntOffset(offset.x.roundToInt(), offset.y.roundToInt()),
        dstSize = IntSize(
            (size.width * scale).roundToInt().coerceAtLeast(1),
            (size.height * scale).roundToInt().coerceAtLeast(1)
        ),
        filterQuality = FilterQuality.Medium
    )
}

private fun DrawScope.drawSegment(start: Offset, end: Offset, color: Color, dashed: Boolean) {
    drawLine(
        color = color,
        start = start,
        end = end,
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round,
        pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(18f, 12f), 0f) else null
    )
    drawEndPoint(start, color)
    drawEndPoint(end, color)
}

private fun DrawScope.drawEndPoint(center: Offset, color: Color) {
    drawCircle(color = Color.White, radius = 11.dp.toPx(), center = center)
    drawCircle(color = color, radius = 8.dp.toPx(), center = center)
    drawCircle(color = Color.White, radius = 3.dp.toPx(), center = center)
}

/** Crosshair shown for a point that still waits for its partner. */
private fun DrawScope.drawPendingMarker(center: Offset, color: Color) {
    val radius = 12.dp.toPx()
    val arm = radius * 1.7f
    val stroke = 2.dp.toPx()
    drawCircle(color = color, radius = radius, center = center, style = Stroke(width = stroke))
    drawLine(color, Offset(center.x - arm, center.y), Offset(center.x + arm, center.y), strokeWidth = stroke)
    drawLine(color, Offset(center.x, center.y - arm), Offset(center.x, center.y + arm), strokeWidth = stroke)
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawLabel(measurer: TextMeasurer, text: String, anchor: Offset, color: Color) {
    val layout = measurer.measure(
        text = AnnotatedString(text),
        style = TextStyle(color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    )
    val horizontalPadding = 8.dp.toPx()
    val verticalPadding = 4.dp.toPx()
    val topLeft = Offset(
        x = anchor.x - layout.size.width / 2f,
        y = anchor.y - layout.size.height - 16.dp.toPx()
    )
    drawRoundRect(
        color = Color(0xCC000000),
        topLeft = Offset(topLeft.x - horizontalPadding, topLeft.y - verticalPadding),
        size = Size(
            layout.size.width + horizontalPadding * 2,
            layout.size.height + verticalPadding * 2
        ),
        cornerRadius = CornerRadius(8.dp.toPx())
    )
    drawText(textLayoutResult = layout, topLeft = topLeft)
}

