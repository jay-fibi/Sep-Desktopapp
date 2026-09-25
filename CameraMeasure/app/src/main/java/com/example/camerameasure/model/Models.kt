package com.example.camerameasure.model

/**
 * A point on the photo expressed as a fraction of the image size.
 * (0,0) is the top-left corner of the photo, (1,1) the bottom-right corner.
 *
 * Normalised coordinates are used everywhere in the UI so that the geometry does not
 * depend on the resolution the photo was decoded at, or on the current zoom level.
 */
data class NormPoint(val x: Float, val y: Float)

/**
 * The known-size object the user marked on the photo, e.g. the long edge of an A4 sheet.
 *
 * @param millimetersPerPixel scale factor derived from the reference object.
 * @param pixelDistance distance between [start] and [end] in decoded-photo pixels.
 */
data class Calibration(
    val start: NormPoint,
    val end: NormPoint,
    val referenceMillimeters: Float,
    val referenceLabel: String,
    val millimetersPerPixel: Float,
    val pixelDistance: Float
)

/** A distance the user measured, kept in millimetres (the canonical internal unit). */
data class Measurement(
    val id: Int,
    val start: NormPoint,
    val end: NormPoint,
    val millimeters: Float
)

/** Units the result can be displayed in. */
enum class LengthUnit(val symbol: String, val decimals: Int) {
    MILLIMETERS("mm", 1),
    CENTIMETERS("cm", 2),
    METERS("m", 4),
    INCHES("in", 3)
}

/**
 * A reference object with a well-known size, so the user does not have to type a number.
 * Sizes are the standardised physical dimensions in millimetres.
 */
data class ReferencePreset(val label: String, val millimeters: Float)

object ReferencePresets {
    val all: List<ReferencePreset> = listOf(
        ReferencePreset("A4 sheet — long edge", 297f),
        ReferencePreset("A4 sheet — short edge", 210f),
        ReferencePreset("US Letter sheet — long edge", 279.4f),
        ReferencePreset("Bank card — long edge", 85.6f),
        ReferencePreset("Bank card — short edge", 53.98f),
        ReferencePreset("US quarter — diameter", 24.26f),
        ReferencePreset("US penny — diameter", 19.05f),
        ReferencePreset("1 euro coin — diameter", 23.25f),
        ReferencePreset("CD / DVD — diameter", 120f),
        ReferencePreset("Sticky note 3 x 3 in — edge", 76.2f)
    )
}
