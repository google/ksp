package com.google.devtools.ksp.symbol.impl.binary

import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.NonExistLocation

abstract class KSNodeDescriptorImpl(override val parent: KSNode?) : KSNode {
    override val location: Location = NonExistLocation
}
