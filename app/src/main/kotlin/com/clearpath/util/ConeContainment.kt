package com.clearpath.util

/**
 * Determines whether a point lies within a camera's directional coverage cone.
 *
 * Coordinate system: bearings are clockwise from north, 0–360°.
 *
 * Edge cases handled:
 *  - Wrap-around: cones centred near 0°/360° (e.g. direction=5°, halfAngle=20°
 *    covers 345°–25° — the range wraps midnight).
 *  - direction == null: treat as full 360° coverage.
 *  - coneAngle == 360: always in cone.
 */
object ConeContainment {

    /**
     * Returns true if [bearingToPoint] (degrees from camera to point) falls within
     * the coverage cone defined by [cameraDirection] ± [halfConeAngle].
     *
     * @param cameraDirection  Central facing direction of camera (0–359°), or null for omnidirectional.
     * @param coneAngle        Total angular width of the coverage cone (degrees), e.g. 90.
     * @param bearingToPoint   Bearing from camera position to the candidate point (0–359°).
     */
    fun isInCone(
        cameraDirection: Int?,
        coneAngle: Int,
        bearingToPoint: Double,
    ): Boolean {
        if (cameraDirection == null) return true  // omnidirectional
        if (coneAngle >= 360) return true

        val halfAngle = coneAngle / 2.0
        val diff = angleDiff(bearingToPoint, cameraDirection.toDouble())
        return diff <= halfAngle
    }

    /**
     * Minimum angular difference between two bearings, result in [0, 180].
     */
    fun angleDiff(a: Double, b: Double): Double {
        val d = Math.abs(a - b) % 360.0
        return if (d > 180.0) 360.0 - d else d
    }
}
