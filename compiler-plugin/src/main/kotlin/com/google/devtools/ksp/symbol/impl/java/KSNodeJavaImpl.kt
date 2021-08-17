package com.google.devtools.ksp.symbol.impl.java

import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.impl.toLocation
import com.intellij.psi.PsiElement

abstract class KSNodeJavaImpl(private val psi: PsiElement, override val parent: KSNode?) : KSNode {
    override val location: Location by lazy {
        psi.toLocation()
    }
}
