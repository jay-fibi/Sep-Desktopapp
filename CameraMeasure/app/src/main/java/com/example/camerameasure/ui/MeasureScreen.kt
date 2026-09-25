package com.example.camerameasure.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.camerameasure.R
import com.example.camerameasure.measure.Units
import com.example.camerameasure.model.LengthUnit
import com.example.camerameasure.model.ReferencePresets
import com.example.camerameasure.ui.MeasureViewModel.Mode
import com.example.camerameasure.ui.theme.MeasurementColors
import java.util.Locale

/**
 * Shows the captured photo with the measuring overlay and the controls below it.
 *
 * Workflow: calibrate (two taps on a reference object of known size) → measure (two taps per
 * distance) → read or share the results.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasureScreen(
    viewModel: MeasureViewModel,
    onRetake: () -> Unit
) {
    val context = LocalContext.current
    val chooserTitle = stringResource(R.string.share_chooser)
    val canShare = viewModel.measurements.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.measure_title)) },
                navigationIcon = {
                    IconButton(onClick = onRetake) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.retake)
                        )
                    }
                },
                actions = {
                    IconButton(
                        enabled = canShare,
                        onClick = { shareReport(context, buildReport(context, viewModel), chooserTitle) }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = stringResource(R.string.share_report)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black)
                    .clipToBounds(),
                contentAlignment = Alignment.Center
            ) {
                val bitmap = viewModel.bitmap
                when {
                    bitmap != null -> PhotoMeasurementCanvas(
                        bitmap = bitmap,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )

                    viewModel.loadFailed -> Text(
                        text = stringResource(R.string.photo_error),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )

                    else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.loading_photo),
                            color = Color.White
                        )
                    }
                }
            }
            ControlPanel(viewModel = viewModel, onRetake = onRetake)
        }
    }

    if (viewModel.awaitingReferenceLength) {
        ReferenceLengthDialog(
            onConfirm = viewModel::confirmReferenceLength,
            onDismiss = viewModel::cancelReferenceLength
        )
    }
}

@Composable
private fun hintFor(viewModel: MeasureViewModel): String = when {
    viewModel.mode == Mode.CALIBRATE && viewModel.awaitingReferenceLength ->
        stringResource(R.string.reference_length_title)

    viewModel.mode == Mode.CALIBRATE && viewModel.calibration != null &&
        viewModel.pendingCalibration.isEmpty() ->
        stringResource(R.string.step_calibrate_done)

    viewModel.mode == Mode.CALIBRATE && viewModel.pendingCalibration.isEmpty() ->
        stringResource(R.string.step_calibrate_hint)

    viewModel.mode == Mode.CALIBRATE ->
        stringResource(R.string.step_calibrate_wait)

    viewModel.calibration == null ->
        stringResource(R.string.no_calibration)

    viewModel.pendingMeasurement.isEmpty() ->
        stringResource(R.string.step_measure_hint)

    else ->
        stringResource(R.string.step_measure_wait)
}

@Composable
private fun ControlPanel(viewModel: MeasureViewModel, onRetake: () -> Unit) {
    val calibration = viewModel.calibration

    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = viewModel.mode == Mode.CALIBRATE,
                    onClick = { viewModel.selectMode(Mode.CALIBRATE) },
                    label = { Text(stringResource(R.string.mode_calibrate)) }
                )
                FilterChip(
                    selected = viewModel.mode == Mode.MEASURE,
                    onClick = { viewModel.selectMode(Mode.MEASURE) },
                    enabled = calibration != null,
                    label = { Text(stringResource(R.string.mode_measure)) }
                )
            }

            Text(
                text = hintFor(viewModel),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = stringResource(R.string.zoom_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = calibration?.let {
                    stringResource(
                        R.string.calibration_summary,
                        it.referenceLabel,
                        Units.formatMillimeters(it.referenceMillimeters),
                        formatNumber(it.pixelDistance, 1),
                        formatNumber(it.millimetersPerPixel, 4)
                    )
                } ?: stringResource(R.string.no_calibration),
                style = MaterialTheme.typography.bodySmall
            )

            if (calibration != null) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LengthUnit.entries.forEach { unit ->
                        FilterChip(
                            selected = viewModel.unit == unit,
                            onClick = { viewModel.selectUnit(unit) },
                            label = { Text(unit.symbol) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(onClick = viewModel::undo) {
                    Text(stringResource(R.string.undo_point))
                }
                TextButton(
                    onClick = viewModel::clearMeasurements,
                    enabled = viewModel.measurements.isNotEmpty()
                ) {
                    Text(stringResource(R.string.clear_measurements))
                }
                TextButton(onClick = onRetake) {
                    Text(stringResource(R.string.retake))
                }
            }

            MeasurementList(viewModel)
        }
    }
}

@Composable
private fun MeasurementList(viewModel: MeasureViewModel) {
    val measurements = viewModel.measurements
    if (measurements.isEmpty()) {
        Text(
            text = stringResource(R.string.measurements_empty),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 160.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        itemsIndexed(measurements, key = { _, measurement -> measurement.id }) { index, measurement ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MeasurementColors[index % MeasurementColors.size])
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "${index + 1}. " + Units.format(measurement.millimeters, viewModel.unit),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { viewModel.deleteMeasurement(measurement.id) }) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.delete_measurement)
                    )
                }
            }
        }
    }
}

private fun formatNumber(value: Float, decimals: Int): String =
    String.format(Locale.US, "%.${decimals}f", value)

/**
 * Asks for the real length of the reference object that was just marked with two taps.
 * A preset can be picked instead of typing the number.
 */
@Composable
private fun ReferenceLengthDialog(
    onConfirm: (Float, String) -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    var presetLabel by remember { mutableStateOf<String?>(null) }
    val millimeters = input.toFloatOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reference_length_title)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { entered ->
                        input = entered.filter { it.isDigit() || it == '.' }
                        presetLabel = null
                    },
                    label = { Text(stringResource(R.string.reference_length_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                ReferencePresets.all.forEach { preset ->
                    TextButton(
                        onClick = {
                            input = Units.formatMillimeters(preset.millimeters)
                            presetLabel = preset.label
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = preset.label + " — " +
                                Units.formatMillimeters(preset.millimeters) + " mm",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = millimeters != null && millimeters > 0f,
                onClick = {
                    val length = millimeters ?: return@TextButton
                    onConfirm(length, presetLabel ?: Units.formatMillimeters(length) + " mm")
                }
            ) {
                Text(stringResource(R.string.reference_length_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.reference_length_cancel))
            }
        }
    )
}

/** Plain-text summary that can be shared with any messaging or notes app. */
private fun buildReport(context: Context, viewModel: MeasureViewModel): String = buildString {
    appendLine(context.getString(R.string.report_header))
    val calibration = viewModel.calibration
    if (calibration != null) {
        appendLine(
            context.getString(
                R.string.calibration_summary,
                calibration.referenceLabel,
                Units.formatMillimeters(calibration.referenceMillimeters),
                formatNumber(calibration.pixelDistance, 1),
                formatNumber(calibration.millimetersPerPixel, 4)
            )
        )
    }
    if (viewModel.measurements.isEmpty()) {
        appendLine(context.getString(R.string.measurements_empty))
    } else {
        viewModel.measurements.forEachIndexed { index, measurement ->
            appendLine("${index + 1}. " + Units.format(measurement.millimeters, viewModel.unit))
        }
    }
    append(context.getString(R.string.report_note))
}

private fun shareReport(context: Context, report: String, chooserTitle: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, chooserTitle)
        putExtra(Intent.EXTRA_TEXT, report)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}


