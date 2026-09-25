package com.example.camerameasure.camera

import android.content.Context
import android.view.Surface
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Owns the CameraX use cases (preview + still capture) for a single [PreviewView].
 *
 * Create one per preview, call [start] once the view is attached, and [stop] when the
 * screen goes away. [takePicture] saves a JPEG into the app cache and returns the file.
 */
class CameraSession(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView
) {

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null

    /** Directory holding the captured JPEGs; cleared by the system when space is needed. */
    private val outputDirectory: File
        get() = File(context.cacheDir, CAPTURE_DIRECTORY).apply { if (!exists()) mkdirs() }

    suspend fun start() {
        val provider = awaitCameraProvider()
        val selector = CameraSelector.DEFAULT_BACK_CAMERA
        check(provider.hasCamera(selector)) { "This device has no back-facing camera" }

        val preview = Preview.Builder()
            .build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setTargetRotation(currentDisplayRotation())
            .build()

        provider.unbindAll()
        camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
        cameraProvider = provider
        imageCapture = capture
    }

    fun stop() {
        cameraProvider?.unbindAll()
        cameraProvider = null
        imageCapture = null
        camera = null
    }

    /** Turns the torch on/off. Ignored when the device has no flash unit. */
    fun setTorchEnabled(enabled: Boolean) {
        camera?.cameraControl?.enableTorch(enabled)
    }

    suspend fun takePicture(): File = suspendCancellableCoroutine { continuation ->
        val capture = imageCapture
        if (capture == null) {
            continuation.resumeWithException(IllegalStateException("Camera is not ready yet"))
            return@suspendCancellableCoroutine
        }
        val file = File(outputDirectory, "measure_${TIMESTAMP.format(Date())}.jpg")
        // The device may have been rotated since the session was bound.
        capture.targetRotation = currentDisplayRotation()
        val options = ImageCapture.OutputFileOptions.Builder(file).build()
        capture.takePicture(
            options,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    if (continuation.isActive) continuation.resume(file) else file.delete()
                }

                override fun onError(exception: ImageCaptureException) {
                    file.delete()
                    if (continuation.isActive) continuation.resumeWithException(exception)
                }
            }
        )
    }

    private suspend fun awaitCameraProvider(): ProcessCameraProvider =
        suspendCancellableCoroutine { continuation ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener(
                {
                    try {
                        val provider = future.get()
                        if (continuation.isActive) continuation.resume(provider)
                    } catch (throwable: Throwable) {
                        if (continuation.isActive) continuation.resumeWithException(throwable)
                    }
                },
                ContextCompat.getMainExecutor(context)
            )
        }

    private fun currentDisplayRotation(): Int =
        previewView.display?.rotation ?: Surface.ROTATION_0

    private companion object {
        const val CAPTURE_DIRECTORY = "captures"
        val TIMESTAMP: SimpleDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US)
    }
}
