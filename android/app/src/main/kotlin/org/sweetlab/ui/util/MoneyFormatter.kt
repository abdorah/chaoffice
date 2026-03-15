package org.sweetlab.ui.util

/**
 * Shared Money display utilities for converting between i64 cents
 * and user-facing decimal strings.
 */

/** Convert i64 cents to "X.XX" display format. */
fun Long.toMoneyDisplay(): String = "%.2f".format(this / 100.0)

/** Convert i64 cents to "+X.XX" / "-X.XX" display format with sign prefix. */
fun Long.toMoneyDisplayWithSign(): String = "%+.2f".format(this / 100.0)

/** Convert user-entered decimal (e.g. "12.50") to i64 cents (1250). */
fun Double.toMoneyCents(): Long = (this * 100).toLong()
