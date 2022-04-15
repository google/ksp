/*
 * Copyright 2022 Google LLC
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devtools.ksp.impl

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.impl.symbol.kotlin.KSClassDeclarationEnumEntryImpl
import com.google.devtools.ksp.impl.symbol.kotlin.KSClassDeclarationImpl
import com.google.devtools.ksp.impl.symbol.kotlin.KSFileImpl
import com.google.devtools.ksp.impl.symbol.kotlin.KSNameImpl
import com.google.devtools.ksp.impl.symbol.kotlin.analyzeWithSymbolAsContext
import com.google.devtools.ksp.processing.KSBuiltIns
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSDeclarationContainer
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSPropertyAccessor
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Variance
import org.jetbrains.kotlin.analysis.api.symbols.KtFileSymbol

@OptIn(KspExperimental::class)
class ResolverAAImpl(
    val ktFiles: List<KtFileSymbol>
) : Resolver {
    private val ksFiles by lazy {
        ktFiles.map { KSFileImpl(it) }
    }
    override val builtIns: KSBuiltIns
        get() = TODO("Not yet implemented")

    override fun createKSTypeReferenceFromKSType(type: KSType): KSTypeReference {
        TODO("Not yet implemented")
    }

    override fun effectiveJavaModifiers(declaration: KSDeclaration): Set<Modifier> {
        TODO("Not yet implemented")
    }

    override fun getAllFiles(): Sequence<KSFile> {
        return ksFiles.asSequence()
    }

    override fun getClassDeclarationByName(name: KSName): KSClassDeclaration? {
        TODO("Not yet implemented")
    }

    override fun getDeclarationsFromPackage(packageName: String): Sequence<KSDeclaration> {
        TODO("Not yet implemented")
    }

    override fun getDeclarationsInSourceOrder(container: KSDeclarationContainer): Sequence<KSDeclaration> {
        TODO("Not yet implemented")
    }

    override fun getFunctionDeclarationsByName(
        name: KSName,
        includeTopLevel: Boolean
    ): Sequence<KSFunctionDeclaration> {
        TODO("Not yet implemented")
    }

    override fun getJavaWildcard(reference: KSTypeReference): KSTypeReference {
        TODO("Not yet implemented")
    }

    override fun getJvmCheckedException(accessor: KSPropertyAccessor): Sequence<KSType> {
        TODO("Not yet implemented")
    }

    override fun getJvmCheckedException(function: KSFunctionDeclaration): Sequence<KSType> {
        TODO("Not yet implemented")
    }

    override fun getJvmName(accessor: KSPropertyAccessor): String? {
        TODO("Not yet implemented")
    }

    override fun getJvmName(declaration: KSFunctionDeclaration): String? {
        TODO("Not yet implemented")
    }

    override fun getKSNameFromString(name: String): KSName {
        TODO("Not yet implemented")
    }

    // FIXME: correct implementation after incremental is ready.
    override fun getNewFiles(): Sequence<KSFile> {
        return getAllFiles().asSequence()
    }

    override fun getOwnerJvmClassName(declaration: KSFunctionDeclaration): String? {
        TODO("Not yet implemented")
    }

    override fun getOwnerJvmClassName(declaration: KSPropertyDeclaration): String? {
        TODO("Not yet implemented")
    }

    override fun getPropertyDeclarationByName(name: KSName, includeTopLevel: Boolean): KSPropertyDeclaration? {
        TODO("Not yet implemented")
    }

    override fun getSymbolsWithAnnotation(annotationName: String, inDepth: Boolean): Sequence<KSAnnotated> {
        TODO("Not yet implemented")
    }

    override fun getTypeArgument(typeRef: KSTypeReference, variance: Variance): KSTypeArgument {
        TODO("Not yet implemented")
    }

    override fun isJavaRawType(type: KSType): Boolean {
        TODO("Not yet implemented")
    }

    override fun mapJavaNameToKotlin(javaName: KSName): KSName? {
        TODO("Not yet implemented")
    }

    override fun mapKotlinNameToJava(kotlinName: KSName): KSName? {
        TODO("Not yet implemented")
    }

    override fun mapToJvmSignature(declaration: KSDeclaration): String? {
        TODO("Not yet implemented")
    }

    override fun overrides(overrider: KSDeclaration, overridee: KSDeclaration): Boolean {
        TODO("Not yet implemented")
    }

    override fun overrides(
        overrider: KSDeclaration,
        overridee: KSDeclaration,
        containingClass: KSClassDeclaration
    ): Boolean {
        TODO("Not yet implemented")
    }
}
