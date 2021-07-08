package com.google.devtools.ksp.symbol.impl.java

import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFileSymbol
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaFile

internal class KSFileSymbolJavaImpl(val element: PsiElement) : KSFileSymbol {

    override val containingFile: KSFile? by lazy {
        KSFileJavaImpl.getCached(element.containingFile as PsiJavaFile)
    }

}
