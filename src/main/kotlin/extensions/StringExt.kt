package extensions

/**
 * Recursively remove double spaces
 */
fun String.removeDoubleSpaces(): String = if ("  " in this) this.replace("  ", " ").removeDoubleSpaces() else this

/**
 * Recursively remove all spaces before a period
 */
fun String.removeSpacesBeforePeriod(): String = if (" ." in this) this.replace(" .", ".").removeSpacesBeforePeriod() else this