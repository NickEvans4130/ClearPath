package com.clearpath.ui.theme

import androidx.compose.ui.graphics.Color

// ── Background / Surface ──────────────────────────────────────────────────────
val Background     = Color(0xFF0D1117)   // deep navy-black
val Surface        = Color(0xFF161B22)   // slightly lighter card surface
val SurfaceVariant = Color(0xFF21262D)   // borders, dividers
val OnSurface      = Color(0xFFE6EDF3)   // primary text
val OnSurfaceMuted = Color(0xFF8B949E)   // secondary / metadata text

// ── Accent ────────────────────────────────────────────────────────────────────
val Amber          = Color(0xFFF59E0B)   // warnings, selected
val AmberDim       = Color(0x40F59E0B)   // subtle amber tint

// ── Exposure / route status ───────────────────────────────────────────────────
val ExposureLow    = Color(0xFF10B981)   // green  — safe route
val ExposureMed    = Color(0xFFF59E0B)   // amber  — moderate exposure
val ExposureHigh   = Color(0xFFEF4444)   // red    — high exposure

// ── Camera type overlay colours ───────────────────────────────────────────────
val CameraANPR     = Color(0xFFEF4444)   // red
val CameraFixed    = Color(0xFFF97316)   // orange
val CameraPTZ      = Color(0xFFF59E0B)   // yellow-amber
val CameraDome     = Color(0xFFF59E0B)   // same as PTZ
val CameraUnknown  = Color(0xFF6B7280)   // grey

// ── Map UI chrome ─────────────────────────────────────────────────────────────
val FabBackground  = Color(0xFF21262D)
val FabIcon        = Color(0xFFE6EDF3)
val SheetHandle    = Color(0xFF30363D)

// ── Status indicators ─────────────────────────────────────────────────────────
val StatusOnline   = Color(0xFF10B981)
val StatusOffline  = Color(0xFF6B7280)
val StatusWarning  = Color(0xFFF59E0B)
val StatusDanger   = Color(0xFFEF4444)
