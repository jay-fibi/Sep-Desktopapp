package com.example.camerameasure.ui

import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.camerameasure.R
import com.example.camerameasure.camera.CameraSession
import kotlinx.coroutines.launch

/**
 * Live camera preview with a framing grid. The shutter captures a JPEG whose absolute path is
 * handed to [onPhotoCaptured].
 */
@Composable
fun CameraScreen(
    onPhotoCaptured: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var session by remember { mutableStateOf<CameraSession?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var torchEnabled by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(previewView, lifecycleOwner) {
        val view = previewView ?: return@LaunchedEffect
        cameraError = null
        try {
            val created = CameraSession(context.applicationContext, lifecycleOwner, view)
            created.start()
            session = created
        } catch (throwable: Throwable) {
            cameraError = throwable.message ?: "Camera could not be started"
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            session?.stop()
            session = null
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }.also { previewView = it }
            },
            modifier = Modifier.fillMaxSize()
        )

        FramingGrid(Modifier.fillMaxSize())

        CameraHintCard(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 36.dp)
        ) {
            TorchToggle(
                enabled = torchEnabled,
                onToggle = {
                    torchEnabled = !torchEnabled
                    session?.setTorchEnabled(torchEnabled)
                },
                modifier = Modifier.align(Alignment.CenterStart)
            )
            ShutterButton(
                isBusy = isCapturing,
                onClick = {
                    val activeSession = session
                    if (activeSession == null) {
                        cameraError = "Camera is not ready yet"
                    } else {
                        isCapturing = true
                        scope.launch {
                            try {
                                onPhotoCaptured(activeSession.takePicture().absolutePath)
                            } catch (throwable: Throwable) {
                                cameraError = throwable.message ?: "Capture failed"
                            } finally {
                                isCapturing = false
                            }
                        }
                    }
                },
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (session == null && cameraError == null) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }

        cameraError?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onError,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.85f))
                    .padding(12.dp)
            )
        }
    }
}

/** Rule-of-thirds grid, which helps to keep the camera parallel to the measured surface. */
@Composable
private fun FramingGrid(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val color = Color.White.copy(alpha = 0.28f)
        val stroke = 1.dp.toPx()
        for (index in 1..2) {
            val x = size.width * index / 3f
            val y = size.height * index / 3f
            drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth = stroke)
            drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = stroke)
        }
    }
}

@Composable
private fun CameraHintCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(14.dp)
    ) {
        Text(
            text = stringResource(R.string.capture_title),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.capture_hint),
            color = Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ShutterButton(
    isBusy: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(78.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.18f))
            .border(width = 3.dp, color = Color.White, shape = CircleShape)
            .clickable(enabled = !isBusy, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(if (isBusy) Color.Gray else Color.White)
        )
    }
}

@Composable
private fun TorchToggle(
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(if (enabled) R.string.torch_on else R.string.torch_off),
        color = if (enabled) Color.Black else Color.White,
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (enabled) Color(0xFFFFC107) else Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    )
}
