package com.google.devtools.ksp.symbol.impl.java

import com.google.devtools.ksp.ExceptionMessage
import com.google.devtools.ksp.symbol.KSClassifierReference
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.NonExistLocation
import com.google.devtools.ksp.symbol.Origin
import com.google.devtools.ksp.symbol.impl.KSObjectCache
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiMethod

class KSClassifierReferenceLiteImplForJava(override val parent: KSTypeReferenceLiteJavaImpl) : KSClassifierReference {
    companion object : KSObjectCache<KSTypeReferenceLiteJavaImpl, KSClassifierReference>() {
        fun getCached(parent: KSTypeReferenceLiteJavaImpl) = KSClassifierReferenceLiteImplForJava.cache
            .getOrPut(parent) { KSClassifierReferenceLiteImplForJava(parent) }
    }
    override val qualifier: KSClassifierReference? = null

    override fun referencedName(): String {
        return when (parent.psiElement) {
            is PsiAnnotation -> parent.psiElement.nameReferenceElement?.referenceName ?: "<ERROR>"
            is PsiMethod -> parent.psiElement.name
            else -> throw IllegalStateException(
                "Unexpected psi type in KSTypeReferenceLiteJavaImpl: ${parent.psiElement.javaClass}, $ExceptionMessage"
            )
        }
    }

    override val typeArguments: List<KSTypeArgument> = emptyList()

    override val origin: Origin = Origin.JAVA

    override val location: Location = NonExistLocation

    override fun toString(): String {
        return referencedName()
    }
}
