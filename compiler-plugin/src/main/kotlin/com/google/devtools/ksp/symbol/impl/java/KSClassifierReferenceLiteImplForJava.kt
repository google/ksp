package com.google.devtools.ksp.symbol.impl.java

import com.google.devtools.ksp.symbol.KSClassifierReference
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.NonExistLocation
import com.google.devtools.ksp.symbol.Origin
import com.google.devtools.ksp.symbol.impl.KSObjectCache

class KSClassifierReferenceLiteImplForJava(override val parent: KSTypeReferenceLiteJavaImpl) : KSClassifierReference {
    companion object : KSObjectCache<KSTypeReferenceLiteJavaImpl, KSClassifierReference>() {
        fun getCached(parent: KSTypeReferenceLiteJavaImpl) = KSClassifierReferenceLiteImplForJava.cache
            .getOrPut(parent) { KSClassifierReferenceLiteImplForJava(parent) }
    }
    override val qualifier: KSClassifierReference? = null

    override fun referencedName(): String {
        return parent.psiClass?.name ?: "<ERROR>"
    }

    override val typeArguments: List<KSTypeArgument> = emptyList()

    override val origin: Origin = Origin.JAVA

    override val location: Location = NonExistLocation

    override fun toString(): String {
        return referencedName()
    }
}
