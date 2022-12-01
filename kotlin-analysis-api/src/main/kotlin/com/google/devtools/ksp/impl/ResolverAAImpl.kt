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
import com.google.devtools.ksp.impl.symbol.kotlin.KSFileJavaImpl
import com.google.devtools.ksp.impl.symbol.kotlin.KSFunctionDeclarationImpl
import com.google.devtools.ksp.impl.symbol.kotlin.KSPropertyDeclarationImpl
import com.google.devtools.ksp.impl.symbol.kotlin.KSPropertyDeclarationJavaImpl
import com.google.devtools.ksp.impl.symbol.kotlin.KSTypeAliasImpl
import com.google.devtools.ksp.impl.symbol.kotlin.KSTypeArgumentLiteImpl
import com.google.devtools.ksp.impl.symbol.kotlin.KSTypeImpl
import com.google.devtools.ksp.impl.symbol.kotlin.analyze
import com.google.devtools.ksp.impl.symbol.kotlin.toKtClassSymbol
import com.google.devtools.ksp.processing.KSBuiltIns
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.impl.KSNameImpl
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
import com.google.devtools.ksp.toKSName
import com.google.devtools.ksp.visitor.CollectAnnotatedSymbolsVisitor
import com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.analysis.api.symbols.KtEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtJavaFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeAliasSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithMembers
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name

@OptIn(KspExperimental::class)
class ResolverAAImpl(
    val ktFiles: List<KtFileSymbol>,
    val javaFiles: List<PsiJavaFile>
) : Resolver {
    companion object {
        lateinit var instance: ResolverAAImpl
        lateinit var ktModule: KtModule
    }

    private val ksFiles by lazy {
        ktFiles.map { KSFileImpl.getCached(it) } + javaFiles.map { KSFileJavaImpl.getCached(it) }
    }

    // TODO: fix in upstream for builtin types.
    override val builtIns: KSBuiltIns by lazy {
        val builtIns = analyze { analysisSession.builtinTypes }
        object : KSBuiltIns {
            override val anyType: KSType by lazy { KSTypeImpl.getCached(builtIns.ANY) }
            override val nothingType: KSType by lazy { KSTypeImpl.getCached(builtIns.NOTHING) }
            override val unitType: KSType by lazy { KSTypeImpl.getCached(builtIns.UNIT) }
            override val numberType: KSType by lazy { TODO() }
            override val byteType: KSType by lazy { KSTypeImpl.getCached(builtIns.BYTE) }
            override val shortType: KSType by lazy { KSTypeImpl.getCached(builtIns.SHORT) }
            override val intType: KSType by lazy { KSTypeImpl.getCached(builtIns.INT) }
            override val longType: KSType by lazy { KSTypeImpl.getCached(builtIns.LONG) }
            override val floatType: KSType by lazy { KSTypeImpl.getCached(builtIns.FLOAT) }
            override val doubleType: KSType by lazy { KSTypeImpl.getCached(builtIns.DOUBLE) }
            override val charType: KSType by lazy { KSTypeImpl.getCached(builtIns.CHAR) }
            override val booleanType: KSType by lazy { KSTypeImpl.getCached(builtIns.BOOLEAN) }
            override val stringType: KSType by lazy { KSTypeImpl.getCached(builtIns.STRING) }
            override val iterableType: KSType by lazy { TODO() }
            override val annotationType: KSType by lazy { TODO() }
            override val arrayType: KSType by lazy { TODO() }
        }
    }

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
        fun findClass(name: KSName): KtSymbolWithMembers? {
            if (name.asString() == "") {
                return null
            }
            val parent = name.getQualifier()
            val simpleName = name.getShortName()
            val classId = ClassId(FqName(parent), Name.identifier(simpleName))
            analyze {
                classId.toKtClassSymbol()
            }?.let { return it }
            return (findClass(KSNameImpl.getCached(name.getQualifier())) as? KtNamedClassOrObjectSymbol)?.let {
                analyze {
                    (
                        it.getStaticMemberScope().getClassifierSymbols { it.asString() == simpleName }.singleOrNull()
                            ?: it.getMemberScope().getClassifierSymbols { it.asString() == simpleName }.singleOrNull()
                        ) as? KtNamedClassOrObjectSymbol
                        ?: it.getStaticMemberScope().getCallableSymbols { it.asString() == simpleName }.singleOrNull()
                            as? KtEnumEntrySymbol
                }
            }
        }
        return findClass(name)?.let {
            when (it) {
                is KtNamedClassOrObjectSymbol -> KSClassDeclarationImpl.getCached(it)
                is KtEnumEntrySymbol -> KSClassDeclarationEnumEntryImpl.getCached(it)
                else -> throw IllegalStateException()
            }
        }
    }

    @KspExperimental
    override fun getDeclarationsFromPackage(packageName: String): Sequence<KSDeclaration> {
        return analyze {
            val packageNames = packageName.split(".")
            var packages = listOf(analysisSession.ROOT_PACKAGE_SYMBOL)
            for (curName in packageNames) {
                packages = packages
                    .flatMap { it.getPackageScope().getPackageSymbols { it.asString() == curName } }
                    .distinct()
            }
            packages.flatMap {
                it.getPackageScope().getAllSymbols().distinct().mapNotNull { symbol ->
                    when (symbol) {
                        is KtNamedClassOrObjectSymbol -> KSClassDeclarationImpl.getCached(symbol)
                        is KtFunctionLikeSymbol -> KSFunctionDeclarationImpl.getCached(symbol)
                        is KtPropertySymbol -> KSPropertyDeclarationImpl.getCached(symbol)
                        is KtTypeAliasSymbol -> KSTypeAliasImpl.getCached(symbol)
                        is KtJavaFieldSymbol -> KSPropertyDeclarationJavaImpl.getCached(symbol)
                        else -> null
                    }
                }
            }.asSequence()
        }
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
        return KSNameImpl.getCached(name)
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

    // TODO: optimization and type alias handling.
    override fun getSymbolsWithAnnotation(annotationName: String, inDepth: Boolean): Sequence<KSAnnotated> {
        val visitor = CollectAnnotatedSymbolsVisitor(inDepth)

        for (file in getNewFiles()) {
            file.accept(visitor, Unit)
        }

        return visitor.symbols.asSequence().filter {
            it.annotations.any {
                it.annotationType.resolve().declaration.qualifiedName?.asString() == annotationName
            }
        }
    }

    override fun getTypeArgument(typeRef: KSTypeReference, variance: Variance): KSTypeArgument {
        return KSTypeArgumentLiteImpl.getCached(typeRef, variance)
    }

    override fun isJavaRawType(type: KSType): Boolean {
        TODO("Not yet implemented")
    }

    @KspExperimental
    override fun mapJavaNameToKotlin(javaName: KSName): KSName? {
        return JavaToKotlinClassMap.mapJavaToKotlin(FqName(javaName.asString()))?.toKSName()
    }

    @KspExperimental
    override fun mapKotlinNameToJava(kotlinName: KSName): KSName? {
        return JavaToKotlinClassMap.mapKotlinToJava(FqNameUnsafe(kotlinName.asString()))?.toKSName()
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
