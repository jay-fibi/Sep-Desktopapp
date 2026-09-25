package com.example.camerameasure.ui

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.camerameasure.camera.PhotoDecoder
import com.example.camerameasure.measure.MeasureMath
import com.example.camerameasure.model.Calibration
import com.example.camerameasure.model.LengthUnit
import com.example.camerameasure.model.Measurement
import com.example.camerameasure.model.NormPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Holds the state of a measuring session: the captured photo, the calibration derived from
 * the reference object and the list of measurements taken on that photo.
 *
 * All geometry is stored in normalised photo coordinates, so rotating the device or changing
 * the zoom level never affects the results.
 */
class MeasureViewModel : ViewModel() {

    enum class Mode { CALIBRATE, MEASURE }

    var photoPath: String? by mutableStateOf(null)
        private set

    var bitmap: Bitmap? by mutableStateOf(null)
        private set

    var isLoadingPhoto: Boolean by mutableStateOf(false)
        private set

    var loadFailed: Boolean by mutableStateOf(false)
        private set

    var mode: Mode by mutableStateOf(Mode.CALIBRATE)
        private set

    var unit: LengthUnit by mutableStateOf(LengthUnit.CENTIMETERS)
        private set

    var calibration: Calibration? by mutableStateOf(null)
        private set

    var measurements: List<Measurement> by mutableStateOf(emptyList())
        private set

    /** First point of the reference pair that has already been tapped. */
    var pendingCalibration: List<NormPoint> by mutableStateOf(emptyList())
        private set

    /** First point of the measurement that has already been tapped. */
    var pendingMeasurement: List<NormPoint> by mutableStateOf(emptyList())
        private set

    /** `true` while the "how long is your reference object?" dialog is open. */
    var awaitingReferenceLength: Boolean by mutableStateOf(false)
        private set

    private var nextMeasurementId = 1

    private val imageWidth: Int get() = bitmap?.width ?: 0
    private val imageHeight: Int get() = bitmap?.height ?: 0

    /** Decodes [path] in the background and starts a fresh session for that photo. */
    fun loadPhoto(path: String) {
        photoPath = path
        bitmap = null
        loadFailed = false
        isLoadingPhoto = true
        resetSession()
        viewModelScope.launch {
            val decoded = withContext(Dispatchers.IO) { PhotoDecoder.decode(path) }
            if (photoPath == path) {
                bitmap = decoded
                loadFailed = decoded == null
                isLoadingPhoto = false
            }
        }
    }

    /** Back to the camera. */
    fun discardPhoto() {
        photoPath = null
        bitmap = null
        loadFailed = false
        isLoadingPhoto = false
        resetSession()
    }

    fun selectMode(newMode: Mode) {
        if (newMode == mode) return
        mode = newMode
        pendingCalibration = emptyList()
        pendingMeasurement = emptyList()
        awaitingReferenceLength = false
    }

    fun selectUnit(newUnit: LengthUnit) {
        unit = newUnit
    }

    /** Handles a tap on the photo. [point] is in normalised photo coordinates. */
    fun onPhotoTap(point: NormPoint) {
        if (bitmap == null) return
        if (point.x !in 0f..1f || point.y !in 0f..1f) return

        when (mode) {
            Mode.CALIBRATE -> {
                if (awaitingReferenceLength) return
                pendingCalibration = pendingCalibration.take(1).plus(point)
                awaitingReferenceLength = pendingCalibration.size == 2
            }

            Mode.MEASURE -> {
                val scale = calibration?.millimetersPerPixel ?: return
                val points = pendingMeasurement.take(1).plus(point)
                if (points.size < 2) {
                    pendingMeasurement = points
                    return
                }
                measurements = measurements + Measurement(
                    id = nextMeasurementId++,
                    start = points[0],
                    end = points[1],
                    millimeters = MeasureMath.millimetersBetween(
                        points[0],
                        points[1],
                        imageWidth,
                        imageHeight,
                        scale
                    )
                )
                pendingMeasurement = emptyList()
            }
        }
    }

    /** Confirms the length of the reference object that was marked with two taps. */
    fun confirmReferenceLength(referenceMillimeters: Float, referenceLabel: String) {
        val points = pendingCalibration
        if (points.size != 2 || referenceMillimeters <= 0f || imageWidth <= 0 || imageHeight <= 0) {
            cancelReferenceLength()
            return
        }
        val pixelDistance = MeasureMath.pixelDistance(points[0], points[1], imageWidth, imageHeight)
        if (pixelDistance <= 0f) {
            cancelReferenceLength()
            return
        }
        calibration = Calibration(
            start = points[0],
            end = points[1],
            referenceMillimeters = referenceMillimeters,
            referenceLabel = referenceLabel,
            millimetersPerPixel = MeasureMath.millimetersPerPixel(pixelDistance, referenceMillimeters),
            pixelDistance = pixelDistance
        )
        pendingCalibration = emptyList()
        awaitingReferenceLength = false
        mode = Mode.MEASURE
    }

    fun cancelReferenceLength() {
        awaitingReferenceLength = false
        pendingCalibration = emptyList()
    }

    /** Removes the last pending point, or the last finished measurement when there is none. */
    fun undo() {
        when (mode) {
            Mode.CALIBRATE -> {
                if (pendingCalibration.isNotEmpty()) {
                    pendingCalibration = pendingCalibration.dropLast(1)
                    awaitingReferenceLength = false
                } else {
                    calibration = null
                }
            }

            Mode.MEASURE -> {
                if (pendingMeasurement.isNotEmpty()) {
                    pendingMeasurement = pendingMeasurement.dropLast(1)
                } else if (measurements.isNotEmpty()) {
                    measurements = measurements.dropLast(1)
                }
            }
        }
    }

    fun deleteMeasurement(id: Int) {
        measurements = measurements.filterNot { it.id == id }
    }

    fun clearMeasurements() {
        measurements = emptyList()
        pendingMeasurement = emptyList()
    }

    private fun resetSession() {
        calibration = null
        measurements = emptyList()
        pendingCalibration = emptyList()
        pendingMeasurement = emptyList()
        awaitingReferenceLength = false
        mode = Mode.CALIBRATE
        unit = LengthUnit.CENTIMETERS
        nextMeasurementId = 1
    }
}
