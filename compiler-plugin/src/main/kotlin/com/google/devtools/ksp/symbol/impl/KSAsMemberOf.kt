package com.google.devtools.ksp.symbol.impl

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.Nullability
import com.google.devtools.ksp.symbol.impl.synthetic.KSTypeReferenceSyntheticImpl

internal fun KSType.asMemberOf(
    resolver: Resolver,
    declaration: KSClassDeclaration,
    containing: KSType
): KSType {
    val declarationQName = declaration.qualifiedName ?: return this
    val matchingAncestor: KSType = (containing.declaration as? KSClassDeclaration)
        ?.getAllSuperTypes()
        ?.firstOrNull {
            it.starProjection().declaration.qualifiedName == declarationQName
        } ?: return this
    // create a map of replacements.
    val replacements = declaration.typeParameters.mapIndexed { index, ksTypeParameter ->
        ksTypeParameter.name to matchingAncestor.arguments.getOrNull(index)
    }.toMap()
    return replaceFromMap(resolver, replacements)
}

private fun KSTypeArgument.replaceFromMap(
    resolver: Resolver,
    arguments: Map<KSName, KSTypeArgument?>
): KSTypeArgument {
    val resolvedType = type?.resolve()
    val myTypeDeclaration = resolvedType?.declaration
    if (myTypeDeclaration is KSTypeParameter) {
        val match = arguments[myTypeDeclaration.name] ?: return this
        // workaround for https://github.com/google/ksp/issues/82
        val explicitNullable = resolvedType.makeNullable() == resolvedType
        return if (explicitNullable) {
            match.makeNullable(resolver)
        } else {
            match
        }
    }
    return this
}

private fun KSType.replaceFromMap(
    resolver: Resolver,
    arguments: Map<KSName, KSTypeArgument?>
): KSType {
    val myDeclaration = this.declaration
    if (myDeclaration is KSTypeParameter) {
        val match = arguments[myDeclaration.name]?.type?.resolve() ?: return this
        // workaround for https://github.com/google/ksp/issues/82
        val explicitNullable = this.makeNullable() == this
        return if (explicitNullable) {
            match.makeNullable()
        } else {
            match
        }
    }
    if (this.arguments.isEmpty()) {
        return this
    }
    return replace(this.arguments.map {
        it.replaceFromMap(resolver, arguments)
    })
}

private fun KSTypeArgument.makeNullable(resolver: Resolver): KSTypeArgument {
    val myType = type
    val resolved = myType?.resolve() ?: return this
    if (resolved.nullability == Nullability.NULLABLE) {
        return this
    }
    return resolver.getTypeArgument(KSTypeReferenceSyntheticImpl(resolved), variance)
}
