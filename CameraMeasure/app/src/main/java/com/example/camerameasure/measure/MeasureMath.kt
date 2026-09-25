package com.example.camerameasure.measure

import com.example.camerameasure.model.NormPoint
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Geometry behind the reference-scale measurement method.
 *
 * The photo is a perspective projection of the scene, so a single "millimetres per pixel"
 * factor is only valid for objects that lie in the same plane as the reference object and
 * roughly parallel to the camera sensor. Under that assumption:
 *
 *     mm_per_pixel = reference_length_mm / reference_distance_px
 *     measured_mm  = distance_px * mm_per_pixel
 *
 * Points are normalised (0..1) over the decoded photo, therefore the pixel distance is
 * recovered by multiplying the normalised delta with the decoded bitmap size.
 */
object MeasureMath {

    /** Euclidean distance between two normalised points, in decoded-photo pixels. */
    fun pixelDistance(
        start: NormPoint,
        end: NormPoint,
        imageWidth: Int,
        imageHeight: Int
    ): Float {
        require(imageWidth > 0 && imageHeight > 0) { "Image size must be positive" }
        val dx = (end.x - start.x) * imageWidth
        val dy = (end.y - start.y) * imageHeight
        return hypot(dx, dy)
    }

    /** Scale factor (millimetres per pixel) implied by a reference object. */
    fun millimetersPerPixel(pixelDistance: Float, referenceMillimeters: Float): Float {
        require(pixelDistance > 0f) { "The two reference points must not coincide" }
        require(referenceMillimeters > 0f) { "The reference length must be positive" }
        return referenceMillimeters / pixelDistance
    }

    /** Real-world length of the segment between [start] and [end]. */
    fun millimetersBetween(
        start: NormPoint,
        end: NormPoint,
        imageWidth: Int,
        imageHeight: Int,
        millimetersPerPixel: Float
    ): Float {
        require(millimetersPerPixel > 0f) { "The scale must be positive" }
        return pixelDistance(start, end, imageWidth, imageHeight) * millimetersPerPixel
    }

    /**
     * Angle of the segment in degrees, measured in image space (0° points "right",
     * positive angles go down since y grows downwards).
     */
    fun angleDegrees(
        start: NormPoint,
        end: NormPoint,
        imageWidth: Int,
        imageHeight: Int
    ): Float {
        val dx = (end.x - start.x) * imageWidth
        val dy = (end.y - start.y) * imageHeight
        return Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    }
}
