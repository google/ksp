package com.google.devtools.ksp.common

import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument

inline fun <E> errorTypeOnInconsistentArguments(
    arguments: List<KSTypeArgument>,
    placeholdersProvider: () -> List<KSTypeArgument>,
    withCorrectedArguments: (corrected: List<KSTypeArgument>) -> KSType,
    errorType: (name: String, message: String) -> E,
): E? {
    if (arguments.isNotEmpty()) {
        val placeholders = placeholdersProvider()
        val diff = arguments.size - placeholders.size
        if (diff > 0) {
            val wouldBeType = withCorrectedArguments(arguments.dropLast(diff))
            return errorType(wouldBeType.toString(), "Unexpected extra $diff type argument(s)")
        } else if (diff < 0) {
            val wouldBeType = withCorrectedArguments(arguments + placeholders.drop(arguments.size))
            return errorType(wouldBeType.toString(), "Missing ${-diff} type argument(s)")
        }
    }
    return null
}
