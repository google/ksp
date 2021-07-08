package com.google.devtools.ksp.symbol.impl.kotlin

import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFileSymbol
import org.jetbrains.kotlin.psi.KtPureElement

internal class KSFileSymbolImpl(val element: KtPureElement) : KSFileSymbol {

    override val containingFile: KSFile? by lazy {
        KSFileImpl.getCached(element.containingKtFile)
    }

}
