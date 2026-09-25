package com.example.camerameasure.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File

/**
 * Loads a captured JPEG into a display-sized [Bitmap] and applies the EXIF orientation, so
 * that the picture the user measures on is upright and matches what was on screen.
 */
object PhotoDecoder {

    /** Longest edge of the decoded bitmap. Big enough for measuring, small enough for memory. */
    const val DEFAULT_MAX_DIMENSION = 2048

    fun decode(path: String, maxDimension: Int = DEFAULT_MAX_DIMENSION): Bitmap? {
        val file = File(path)
        if (!file.exists() || file.length() == 0L) return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxDimension)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeFile(path, options) ?: return null
        return applyOrientation(decoded, readOrientation(path))
    }

    /**
     * Largest power-of-two sub-sampling factor that keeps the longest edge at or above
     * [maxDimension] when possible (never below one).
     */
    fun sampleSizeFor(width: Int, height: Int, maxDimension: Int): Int {
        require(maxDimension > 0) { "maxDimension must be positive" }
        val largestEdge = maxOf(width, height)
        var sampleSize = 1
        while (largestEdge / sampleSize > maxDimension) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun readOrientation(path: String): Int = try {
        ExifInterface(path).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
    } catch (io: Exception) {
        ExifInterface.ORIENTATION_NORMAL
    }

    private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = matrixForOrientation(orientation) ?: return bitmap
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    private fun matrixForOrientation(orientation: Int): Matrix? = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> Matrix().apply { postRotate(90f) }
        ExifInterface.ORIENTATION_ROTATE_180 -> Matrix().apply { postRotate(180f) }
        ExifInterface.ORIENTATION_ROTATE_270 -> Matrix().apply { postRotate(270f) }
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> Matrix().apply { postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> Matrix().apply { postScale(1f, -1f) }
        ExifInterface.ORIENTATION_TRANSPOSE -> Matrix().apply {
            postRotate(90f)
            postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_TRANSVERSE -> Matrix().apply {
            postRotate(270f)
            postScale(-1f, 1f)
        }
        else -> null
    }
}
