package com.frostbyte.launcher.ui.common

/** Formats whole-number GB without a trailing ".0", e.g. 4.0 -> "4", 4.5 -> "4.5". */
fun formatRamGb(gb: Float): String =
    if (gb == gb.toInt().toFloat()) gb.toInt().toString() else gb.toString()
