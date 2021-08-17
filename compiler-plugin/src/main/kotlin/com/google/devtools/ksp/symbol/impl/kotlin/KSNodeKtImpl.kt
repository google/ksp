package com.google.devtools.ksp.symbol.impl.kotlin

import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.impl.toLocation
import org.jetbrains.kotlin.psi.KtElement

abstract class KSNodeKtImpl(private val element: KtElement) : KSNode {
    override val location: Location by lazy {
        element.toLocation()
    }
}
