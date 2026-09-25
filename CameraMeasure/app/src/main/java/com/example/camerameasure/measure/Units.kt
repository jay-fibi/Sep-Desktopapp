package com.example.camerameasure.measure

import com.example.camerameasure.model.LengthUnit
import java.util.Locale

/** Converts and formats lengths. Millimetres are the canonical unit inside the app. */
object Units {

    const val MILLIMETERS_PER_INCH = 25.4f
    private const val MILLIMETERS_PER_CENTIMETER = 10f
    private const val MILLIMETERS_PER_METER = 1000f

    fun convert(millimeters: Float, unit: LengthUnit): Float = when (unit) {
        LengthUnit.MILLIMETERS -> millimeters
        LengthUnit.CENTIMETERS -> millimeters / MILLIMETERS_PER_CENTIMETER
        LengthUnit.METERS -> millimeters / MILLIMETERS_PER_METER
        LengthUnit.INCHES -> millimeters / MILLIMETERS_PER_INCH
    }

    /** e.g. `Units.format(297f, LengthUnit.CENTIMETERS) == "29.70 cm"`. */
    fun format(millimeters: Float, unit: LengthUnit): String = String.format(
        Locale.US,
        "%.${unit.decimals}f %s",
        convert(millimeters, unit),
        unit.symbol
    )

    /** Shortest representation of a preset length in millimetres, e.g. `297` or `53.98`. */
    fun formatMillimeters(millimeters: Float): String =
        if (millimeters % 1f == 0f) millimeters.toInt().toString() else millimeters.toString()
}
