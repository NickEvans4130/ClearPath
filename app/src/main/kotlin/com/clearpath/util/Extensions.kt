package com.clearpath.util

import org.osmdroid.util.GeoPoint
import com.clearpath.data.route.LatLon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun LatLon.toGeoPoint() = GeoPoint(lat, lon)
fun GeoPoint.toLatLon() = LatLon(latitude, longitude)

fun Long.toRelativeDate(): String {
    if (this == 0L) return "Never"
    val now = System.currentTimeMillis()
    val diff = now - this
    return when {
        diff < 60_000             -> "Just now"
        diff < 3_600_000          -> "${diff / 60_000}m ago"
        diff < 86_400_000         -> "${diff / 3_600_000}h ago"
        diff < 7 * 86_400_000     -> "${diff / 86_400_000}d ago"
        else -> SimpleDateFormat("dd MMM yyyy", Locale.UK).format(Date(this))
    }
}

fun Int.formatDistance(): String = when {
    this >= 1000 -> "%.1f km".format(this / 1000f)
    else         -> "${this}m"
}

fun Int.formatDuration(): String {
    val h = this / 3600
    val m = (this % 3600) / 60
    val s = this % 60
    return when {
        h > 0  -> "${h}h ${m}m"
        m > 0  -> "${m}m"
        else   -> "${s}s"
    }
}

fun Float.formatExposureScore(): String = "%.0f".format(this)
