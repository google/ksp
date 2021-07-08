package com.google.devtools.ksp.symbol

/**
 * A symbol that can be found inside a [KSFile].
 */
interface KSFileSymbol {
    /**
     * The containing source file of this declaration, can be null if symbol does not come from a source file, i.e. from a class file.
     */
    val containingFile: KSFile?

}
