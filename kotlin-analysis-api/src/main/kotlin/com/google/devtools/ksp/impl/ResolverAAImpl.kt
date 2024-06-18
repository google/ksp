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
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.google.devtools.ksp.impl

import com.google.devtools.ksp.*
import com.google.devtools.ksp.common.JVM_DEFAULT_ANNOTATION_FQN
import com.google.devtools.ksp.common.JVM_DEFAULT_WITHOUT_COMPATIBILITY_ANNOTATION_FQN
import com.google.devtools.ksp.common.JVM_STATIC_ANNOTATION_FQN
import com.google.devtools.ksp.common.JVM_STRICTFP_ANNOTATION_FQN
import com.google.devtools.ksp.common.JVM_SYNCHRONIZED_ANNOTATION_FQN
import com.google.devtools.ksp.common.JVM_TRANSIENT_ANNOTATION_FQN
import com.google.devtools.ksp.common.JVM_VOLATILE_ANNOTATION_FQN
import com.google.devtools.ksp.common.extractThrowsAnnotation
import com.google.devtools.ksp.common.impl.KSNameImpl
import com.google.devtools.ksp.common.impl.KSTypeReferenceSyntheticImpl
import com.google.devtools.ksp.common.impl.RefPosition
import com.google.devtools.ksp.common.impl.findOuterMostRef
import com.google.devtools.ksp.common.impl.findRefPosition
import com.google.devtools.ksp.common.impl.isReturnTypeOfAnnotationMethod
import com.google.devtools.ksp.common.javaModifiers
import com.google.devtools.ksp.common.memoized
import com.google.devtools.ksp.common.visitor.CollectAnnotatedSymbolsVisitor
import com.google.devtools.ksp.impl.symbol.java.KSAnnotationJavaImpl
import com.google.devtools.ksp.impl.symbol.kotlin.*
import com.google.devtools.ksp.impl.symbol.util.BinaryClassInfoCache
import com.google.devtools.ksp.impl.symbol.util.DeclarationOrdering
import com.google.devtools.ksp.impl.symbol.util.extractThrowsFromClassFile
import com.google.devtools.ksp.impl.symbol.util.hasAnnotation
import com.google.devtools.ksp.processing.KSBuiltIns
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.file.impl.JavaFileManager
import org.jetbrains.kotlin.analysis.api.components.buildSubstitutor
import org.jetbrains.kotlin.analysis.api.fir.types.KaFirType
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.decompiler.stub.file.ClsKotlinBinaryClassCache
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getFirResolveSession
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCliJavaFileManagerImpl
import org.jetbrains.kotlin.fir.types.isRaw
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightAccessorMethod
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightSimpleMethod
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.load.kotlin.getOptimalModeForReturnType
import org.jetbrains.kotlin.load.kotlin.getOptimalModeForValueParameter
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.org.objectweb.asm.Opcodes

@OptIn(KspExperimental::class)
class ResolverAAImpl(
    val allKSFiles: List<KSFile>,
    val newKSFiles: List<KSFile>,
    val deferredSymbols: Map<SymbolProcessor, List<Restorable>>,
    val project: Project,
    val incrementalContext: IncrementalContextAA,
) : Resolver {
    companion object {
        val instance_prop: ThreadLocal<ResolverAAImpl> = ThreadLocal()
        private val ktModule_prop: ThreadLocal<KtSourceModule> = ThreadLocal()
        var instance
            get() = instance_prop.get()
            set(value) {
                instance_prop.set(value)
            }
        var ktModule: KtSourceModule
            get() = ktModule_prop.get()
            set(value) {
                ktModule_prop.set(value)
            }
    }
    lateinit var propertyAsMemberOfCache: MutableMap<Pair<KSPropertyDeclaration, KSType>, KSType>
    lateinit var functionAsMemberOfCache: MutableMap<Pair<KSFunctionDeclaration, KSType>, KSFunction>
    val javaFileManager = project.getService(JavaFileManager::class.java) as KotlinCliJavaFileManagerImpl
    val classBinaryCache = ClsKotlinBinaryClassCache()
    private val packageInfoFiles by lazy {
        allKSFiles.filter { it.fileName == "package-info.java" }.asSequence().memoized()
    }

    private val aliasingFqNs: Map<String, KSTypeAlias> by lazy {
        val result = mutableMapOf<String, KSTypeAlias>()
        val visitor = object : KSVisitorVoid() {
            override fun visitFile(file: KSFile, data: Unit) {
                file.declarations.forEach { it.accept(this, data) }

                // TODO: evaluate with benchmarks: cost of getContainingFile v.s. name collision
                // Import aliases are file-scoped. `aliasingNamesByFile` could be faster
                ((file as? KSFileImpl)?.ktFileSymbol?.psi as? KtFile)?.importDirectives?.forEach {
                    it.aliasName?.let { aliasingNames.add(it) }
                }
            }

            override fun visitTypeAlias(typeAlias: KSTypeAlias, data: Unit) {
                typeAlias.qualifiedName?.asString()?.let { fqn ->
                    result[fqn] = typeAlias
                    aliasingNames.add(fqn.substringAfterLast('.'))
                }
            }
        }
        allKSFiles.forEach { it.accept(visitor, Unit) }
        result
    }
    private val aliasingNames: MutableSet<String> = mutableSetOf()

    // TODO: fix in upstream for builtin types.
    override val builtIns: KSBuiltIns by lazy {
        val builtIns = analyze { analysisSession.builtinTypes }
        object : KSBuiltIns {
            override val anyType: KSType by lazy { KSTypeImpl.getCached(builtIns.ANY) }
            override val nothingType: KSType by lazy { KSTypeImpl.getCached(builtIns.NOTHING) }
            override val unitType: KSType by lazy { KSTypeImpl.getCached(builtIns.UNIT) }
            override val numberType: KSType by lazy {
                getClassDeclarationByName("kotlin.Number")!!.asStarProjectedType()
            }
            override val byteType: KSType by lazy { KSTypeImpl.getCached(builtIns.BYTE) }
            override val shortType: KSType by lazy { KSTypeImpl.getCached(builtIns.SHORT) }
            override val intType: KSType by lazy { KSTypeImpl.getCached(builtIns.INT) }
            override val longType: KSType by lazy { KSTypeImpl.getCached(builtIns.LONG) }
            override val floatType: KSType by lazy { KSTypeImpl.getCached(builtIns.FLOAT) }
            override val doubleType: KSType by lazy { KSTypeImpl.getCached(builtIns.DOUBLE) }
            override val charType: KSType by lazy { KSTypeImpl.getCached(builtIns.CHAR) }
            override val booleanType: KSType by lazy { KSTypeImpl.getCached(builtIns.BOOLEAN) }
            override val stringType: KSType by lazy { KSTypeImpl.getCached(builtIns.STRING) }
            override val iterableType: KSType by lazy {
                getClassDeclarationByName("kotlin.collections.Iterable")!!.asStarProjectedType()
            }
            override val annotationType: KSType by lazy {
                getClassDeclarationByName("kotlin.Annotation")!!.asStarProjectedType()
            }
            override val arrayType: KSType by lazy {
                getClassDeclarationByName("kotlin.Array")!!.asStarProjectedType()
            }
        }
    }

    override fun createKSTypeReferenceFromKSType(type: KSType): KSTypeReference {
        return KSTypeReferenceSyntheticImpl.getCached(type, null)
    }

    override fun effectiveJavaModifiers(declaration: KSDeclaration): Set<Modifier> {
        val modifiers = HashSet<Modifier>(declaration.modifiers.filter { it in javaModifiers })

        // This is only needed by sources.
        // PUBLIC, PRIVATE, PROTECTED are already handled in descriptor based impls.
        fun addVisibilityModifiers() {
            when {
                declaration.isPublic() -> modifiers.add(Modifier.PUBLIC)
                declaration.isPrivate() -> modifiers.add(Modifier.PRIVATE)
                declaration.isProtected() -> modifiers.add(Modifier.PROTECTED)
            }
        }

        when (declaration.origin) {
            Origin.JAVA -> {
                addVisibilityModifiers()
                if (declaration is KSClassDeclaration && declaration.classKind == ClassKind.INTERFACE)
                    modifiers.add(Modifier.ABSTRACT)
            }
            Origin.KOTLIN -> {
                addVisibilityModifiers()
                if (!declaration.isOpen())
                    modifiers.add(Modifier.FINAL)
                (declaration as? KSClassDeclarationImpl)?.let {
                    analyze {
                        if (
                            it.ktClassOrObjectSymbol.getStaticMemberScope()
                                .getAllSymbols().contains(declaration.ktDeclarationSymbol)
                        )
                            modifiers.add(Modifier.JAVA_STATIC)
                    }
                }

                if (declaration.hasAnnotation(JVM_DEFAULT_ANNOTATION_FQN))
                    modifiers.add(Modifier.JAVA_DEFAULT)
                if (declaration.hasAnnotation(JVM_DEFAULT_WITHOUT_COMPATIBILITY_ANNOTATION_FQN))
                    modifiers.add(Modifier.JAVA_DEFAULT)
                if (declaration.hasAnnotation(JVM_STRICTFP_ANNOTATION_FQN))
                    modifiers.add(Modifier.JAVA_STRICT)
                if (declaration.hasAnnotation(JVM_SYNCHRONIZED_ANNOTATION_FQN))
                    modifiers.add(Modifier.JAVA_SYNCHRONIZED)
                if (declaration.hasAnnotation(JVM_TRANSIENT_ANNOTATION_FQN))
                    modifiers.add(Modifier.JAVA_TRANSIENT)
                if (declaration.hasAnnotation(JVM_VOLATILE_ANNOTATION_FQN))
                    modifiers.add(Modifier.JAVA_VOLATILE)
                when (declaration) {
                    is KSClassDeclaration -> {
                        if (declaration.isCompanionObject)
                            modifiers.add(Modifier.JAVA_STATIC)
                        if (declaration.classKind == ClassKind.INTERFACE)
                            modifiers.add(Modifier.ABSTRACT)
                    }
                    is KSPropertyDeclaration -> {
                        if (declaration.isAbstract())
                            modifiers.add(Modifier.ABSTRACT)
                    }
                    is KSFunctionDeclaration -> {
                        if (declaration.isAbstract)
                            modifiers.add(Modifier.ABSTRACT)
                    }
                }
            }
            Origin.KOTLIN_LIB, Origin.JAVA_LIB -> {
                if (declaration.hasAnnotation(JVM_STATIC_ANNOTATION_FQN)) {
                    modifiers.add(Modifier.JAVA_STATIC)
                }
                addVisibilityModifiers()
                when (declaration) {
                    is KSPropertyDeclaration -> {
                        if (declaration.jvmAccessFlag and Opcodes.ACC_TRANSIENT != 0)
                            modifiers.add(Modifier.JAVA_TRANSIENT)
                        if (declaration.jvmAccessFlag and Opcodes.ACC_VOLATILE != 0)
                            modifiers.add(Modifier.JAVA_VOLATILE)
                    }
                    is KSFunctionDeclaration -> {
                        if (declaration.jvmAccessFlag and Opcodes.ACC_STRICT != 0)
                            modifiers.add(Modifier.JAVA_STRICT)
                        if (declaration.jvmAccessFlag and Opcodes.ACC_SYNCHRONIZED != 0)
                            modifiers.add(Modifier.JAVA_SYNCHRONIZED)
                    }
                }
            }
            else -> Unit
        }
        return modifiers
    }

    internal val KSPropertyDeclaration.jvmAccessFlag: Int
        get() = when (origin) {
            Origin.KOTLIN_LIB, Origin.JAVA_LIB -> {
                val fileManager = instance.javaFileManager
                val parentClass = this.findParentOfType<KSClassDeclaration>()
                val classId = (parentClass as KSClassDeclarationImpl).ktClassOrObjectSymbol.classIdIfNonLocal!!
                val virtualFileContent = analyze {
                    (fileManager.findClass(classId, analysisScope) as JavaClassImpl).virtualFile!!.contentsToByteArray()
                }
                BinaryClassInfoCache.getCached(classId, virtualFileContent)
                    .fieldAccFlags[this.simpleName.asString()] ?: 0
            }
            else -> throw IllegalStateException("this function expects only KOTLIN_LIB or JAVA_LIB")
        }

    internal val KSFunctionDeclaration.jvmAccessFlag: Int
        get() = when (origin) {
            Origin.KOTLIN_LIB, Origin.JAVA_LIB -> {
                val jvmDesc = mapToJvmSignatureInternal(this)
                val fileManager = instance.javaFileManager
                val parentClass = this.findParentOfType<KSClassDeclaration>()
                val classId = (parentClass as KSClassDeclarationImpl).ktClassOrObjectSymbol.classIdIfNonLocal!!
                val virtualFileContent = analyze {
                    (fileManager.findClass(classId, analysisScope) as JavaClassImpl).virtualFile!!.contentsToByteArray()
                }
                BinaryClassInfoCache.getCached(classId, virtualFileContent)
                    .methodAccFlags[this.simpleName.asString() + jvmDesc] ?: 0
            }
            else -> throw IllegalStateException("this function expects only KOTLIN_LIB or JAVA_LIB")
        }

    override fun getAllFiles(): Sequence<KSFile> {
        return allKSFiles.asSequence()
    }

    override fun getClassDeclarationByName(name: KSName): KSClassDeclaration? {
        fun findClass(name: KSName): KtSymbol? {
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
            val packageNames = FqName(packageName).pathSegments().map { it.asString() }
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
        if (container.origin != Origin.KOTLIN_LIB) {
            return container.declarations
        }
        require(container is AbstractKSDeclarationImpl)
        val fileManager = instance.javaFileManager
        var parentClass: KSNode = container
        while (parentClass.parent != null &&
            (parentClass !is KSClassDeclarationImpl || parentClass.ktClassOrObjectSymbol.classIdIfNonLocal == null)
        ) {
            parentClass = parentClass.parent!!
        }
        val classId = (parentClass as KSClassDeclarationImpl).ktClassOrObjectSymbol.classIdIfNonLocal
            ?: return container.declarations
        val virtualFile = analyze {
            (fileManager.findClass(classId, analysisScope) as? JavaClassImpl)?.virtualFile
        } ?: return container.declarations
        val kotlinClass = classBinaryCache.getKotlinBinaryClass(virtualFile) ?: return container.declarations
        val declarationOrdering = DeclarationOrdering(kotlinClass)

        return (container.declarations as? Sequence<AbstractKSDeclarationImpl>)
            ?.sortedWith(declarationOrdering.comparator) ?: container.declarations
    }

    override fun getFunctionDeclarationsByName(
        name: KSName,
        includeTopLevel: Boolean
    ): Sequence<KSFunctionDeclaration> {
        val qualifier = name.getQualifier()
        val functionName = name.getShortName()
        val nonTopLevelResult = this.getClassDeclarationByName(qualifier)?.getDeclaredFunctions()
            ?.filter { it.simpleName.asString() == functionName }?.asSequence() ?: emptySequence()
        return if (!includeTopLevel) nonTopLevelResult else {
            nonTopLevelResult.plus(
                analyze {
                    getTopLevelCallableSymbols(FqName(qualifier), Name.identifier(functionName))
                        .filterIsInstance<KtFunctionLikeSymbol>()
                        .map {
                            KSFunctionDeclarationImpl.getCached(it)
                        }
                }
            )
        }
    }

    override fun getJavaWildcard(reference: KSTypeReference): KSTypeReference {
        val (ref, indexes) = reference.findOuterMostRef()
        val type = ref.resolve()
        if (type.isError)
            return reference
        val position = findRefPosition(ref)
        val ktType = (type as KSTypeImpl).type
        // cast to FIR internal needed due to missing support in AA for type mapping mode
        // and corresponding type mapping APIs.
        val coneType = (ktType as KaFirType).coneType
        val mode = analyze {
            val typeContext = analyze { useSiteModule.getFirResolveSession(project).useSiteFirSession.typeContext }
            when (position) {
                RefPosition.RETURN_TYPE -> typeContext.getOptimalModeForReturnType(
                    coneType,
                    reference.isReturnTypeOfAnnotationMethod()
                )
                RefPosition.SUPER_TYPE -> TypeMappingMode.SUPER_TYPE
                RefPosition.PARAMETER_TYPE -> typeContext.getOptimalModeForValueParameter(coneType)
            }.updateFromParents(ref)
        }
        return analyze {
            ktType.toWildcard(mode).let {
                var candidate: KtType = it
                for (i in indexes.reversed()) {
                    candidate = candidate.typeArguments()[i].type!!
                }
                KSTypeReferenceSyntheticImpl.getCached(KSTypeImpl.getCached(candidate), null)
            }
        }
    }

    override fun getJvmCheckedException(accessor: KSPropertyAccessor): Sequence<KSType> {
        return when (accessor.origin) {
            Origin.KOTLIN, Origin.SYNTHETIC -> {
                extractThrowsAnnotation(accessor)
            }
            Origin.KOTLIN_LIB, Origin.JAVA_LIB -> {
                val fileManager = javaFileManager
                val parentClass = accessor.findParentOfType<KSClassDeclaration>()
                val classId = (parentClass as KSClassDeclarationImpl).ktClassOrObjectSymbol.classIdIfNonLocal!!
                val virtualFileContent = analyze {
                    (fileManager.findClass(classId, analysisScope) as JavaClassImpl).virtualFile!!.contentsToByteArray()
                }
                val jvmDesc = this.mapToJvmSignatureInternal(accessor)
                if (virtualFileContent == null) {
                    return emptySequence()
                }
                extractThrowsFromClassFile(
                    virtualFileContent,
                    jvmDesc,
                    (if (accessor is KSPropertyGetter) "get" else "set") +
                        accessor.receiver.simpleName.asString().capitalize()
                )
            }
            else -> emptySequence()
        }
    }

    override fun getJvmCheckedException(function: KSFunctionDeclaration): Sequence<KSType> {
        return when (function.origin) {
            Origin.JAVA -> {
                val psi = (function as KSFunctionDeclarationImpl).ktFunctionSymbol.psi as PsiMethod
                psi.throwsList.referencedTypes.asSequence().mapNotNull {
                    analyze {
                        it.asKtType(psi)?.let { KSTypeImpl.getCached(it) }
                    }
                }
            }
            Origin.KOTLIN -> {
                extractThrowsAnnotation(function)
            }
            Origin.KOTLIN_LIB, Origin.JAVA_LIB -> {
                val fileManager = javaFileManager
                val parentClass = function.findParentOfType<KSClassDeclaration>()
                val classId = (parentClass as KSClassDeclarationImpl).ktClassOrObjectSymbol.classIdIfNonLocal!!
                val virtualFileContent = analyze {
                    (fileManager.findClass(classId, analysisScope) as JavaClassImpl).virtualFile!!.contentsToByteArray()
                }
                val jvmDesc = this.mapToJvmSignature(function)
                if (virtualFileContent == null) {
                    return emptySequence()
                }
                extractThrowsFromClassFile(virtualFileContent, jvmDesc, function.simpleName.asString())
            }
            else -> emptySequence()
        }
    }

    // TODO: handle library symbols
    override fun getJvmName(accessor: KSPropertyAccessor): String? {
        (
            (accessor.receiver.closestClassDeclaration() as? KSClassDeclarationImpl)
                ?.ktClassOrObjectSymbol?.psi as? KtClassOrObject
            )?.toLightClass()?.allMethods
            ?.let {
                // If there are light accessors, information in light accessors are more accurate.
                // check light accessor first, if not found then default to light simple method.
                it.filterIsInstance<SymbolLightAccessorMethod>() + it.filterIsInstance<SymbolLightSimpleMethod>()
            }
            ?.firstOrNull {
                (it.parameters.isNotEmpty() xor (accessor is KSPropertyGetter)) &&
                    it.kotlinOrigin == (accessor.receiver as? KSPropertyDeclarationImpl)?.ktPropertySymbol?.psi
            }?.let {
                return it.name
            }
        if (accessor.receiver.closestClassDeclaration()?.classKind == ClassKind.ANNOTATION_CLASS) {
            return accessor.receiver.simpleName.asString()
        }
        val prefix = if (accessor is KSPropertyGetter) {
            "get"
        } else {
            "set"
        }
        val mangledName = if (accessor.modifiers.contains(Modifier.INTERNAL)) {
            "\$${ktModule.moduleName}"
        } else ""
        return "${prefix}${accessor.receiver.simpleName.asString().capitalize()}$mangledName"
    }

    // TODO: handle library symbols
    override fun getJvmName(declaration: KSFunctionDeclaration): String? {
        (declaration.closestClassDeclaration() as? KSClassDeclarationImpl)?.ktDeclarationSymbol?.psi?.let {
            (it as? KtClassOrObject)?.toLightClass()
        }?.allMethods?.filterIsInstance<SymbolLightSimpleMethod>()?.singleOrNull {
            it.kotlinOrigin == (declaration as KSFunctionDeclarationImpl).ktFunctionSymbol.psi
        }?.let {
            return it.name
        }
        val mangledName = if (declaration.modifiers.contains(Modifier.INTERNAL)) {
            "\$${ktModule.moduleName}"
        } else ""
        return declaration.simpleName.asString() + mangledName
    }

    override fun getKSNameFromString(name: String): KSName {
        return KSNameImpl.getCached(name)
    }

    // FIXME: correct implementation after incremental is ready.
    override fun getNewFiles(): Sequence<KSFile> {
        return newKSFiles.asSequence()
    }

    override fun getOwnerJvmClassName(declaration: KSFunctionDeclaration): String? {
        TODO("Not yet implemented")
    }

    override fun getOwnerJvmClassName(declaration: KSPropertyDeclaration): String? {
        TODO("Not yet implemented")
    }

    override fun getPropertyDeclarationByName(name: KSName, includeTopLevel: Boolean): KSPropertyDeclaration? {
        val qualifier = name.getQualifier()
        val propertyName = name.getShortName()
        val nonTopLevelResult = this.getClassDeclarationByName(qualifier)?.getDeclaredProperties()
            ?.singleOrNull { it.simpleName.asString() == propertyName }
        return if (!includeTopLevel) {
            nonTopLevelResult
        } else {
            val topLevelResult = (
                analyze {
                    getTopLevelCallableSymbols(FqName(qualifier), Name.identifier(propertyName)).singleOrNull {
                        it is KtPropertySymbol
                    }?.toKSDeclaration() as? KSPropertyDeclaration
                }
                )
            if (topLevelResult != null && nonTopLevelResult != null) {
                throw IllegalStateException("Found multiple properties with same qualified name")
            }
            nonTopLevelResult ?: topLevelResult
        }
    }

    internal fun KSTypeReference.resolveToUnderlying(): KSType {
        var candidate = resolve()
        var declaration = candidate.declaration
        while (declaration is KSTypeAlias) {
            candidate = declaration.type.resolve()
            declaration = candidate.declaration
        }
        return candidate
    }
    internal fun checkAnnotation(annotation: KSAnnotation, ksName: KSName, shortName: String): Boolean {
        val annotationType = annotation.annotationType
        val referencedName = (annotationType.element as? KSClassifierReference)?.referencedName()
        val simpleName = referencedName?.substringAfterLast('.')
        return (simpleName == shortName || simpleName in aliasingNames) &&
            annotationType.resolveToUnderlying().declaration.qualifiedName == ksName
    }
    // Currently, all annotation types are imlemented by KSTypeReferenceResolvedImpl.
    // The short-name-check optimization doesn't help.
    override fun getSymbolsWithAnnotation(annotationName: String, inDepth: Boolean): Sequence<KSAnnotated> {
        val realAnnotationName =
            aliasingFqNs[annotationName]?.type?.resolveToUnderlying()?.declaration?.qualifiedName?.asString()
                ?: annotationName

        val ksName = KSNameImpl.getCached(realAnnotationName)
        val shortName = ksName.getShortName()
        fun checkAnnotated(annotated: KSAnnotated): Boolean {
            return annotated.annotations.any {
                checkAnnotation(it, ksName, shortName)
            }
        }
        val newSymbols = if (inDepth) newAnnotatedSymbolsWithLocals else newAnnotatedSymbols
        return (newSymbols + deferredSymbolsRestored).asSequence().filter(::checkAnnotated)
    }

    private fun collectAnnotatedSymbols(inDepth: Boolean): Collection<KSAnnotated> {
        val visitor = CollectAnnotatedSymbolsVisitor(inDepth)

        for (file in newKSFiles) {
            file.accept(visitor, Unit)
        }

        return visitor.symbols
    }

    private val deferredSymbolsRestored: Set<KSAnnotated> by lazy {
        deferredSymbols.values.flatten().mapNotNull { it.restore() }.toSet()
    }

    private val newAnnotatedSymbols: Collection<KSAnnotated> by lazy {
        collectAnnotatedSymbols(false)
    }

    private val newAnnotatedSymbolsWithLocals: Collection<KSAnnotated> by lazy {
        collectAnnotatedSymbols(true)
    }

    override fun getTypeArgument(typeRef: KSTypeReference, variance: Variance): KSTypeArgument {
        return KSTypeArgumentLiteImpl.getCached(typeRef, variance)
    }

    override fun isJavaRawType(type: KSType): Boolean {
        return type is KSTypeImpl && (type.type as KaFirType).coneType.isRaw()
    }

    internal fun KSFile.getPackageAnnotations() = (this as? KSFileJavaImpl)?.psi?.packageStatement?.annotationList
        ?.annotations?.map { KSAnnotationJavaImpl.getCached(it, this) } ?: emptyList<KSAnnotation>()

    @KspExperimental
    override fun getPackageAnnotations(packageName: String): Sequence<KSAnnotation> {
        return packageInfoFiles.singleOrNull { it.packageName.asString() == packageName }
            ?.getPackageAnnotations()?.asSequence() ?: emptySequence()
    }

    @KspExperimental
    override fun getPackagesWithAnnotation(annotationName: String): Sequence<String> {
        return packageInfoFiles.filter {
            it.getPackageAnnotations().any {
                (it.annotationType.element as? KSClassifierReference)?.referencedName()
                    ?.substringAfterLast(".") == annotationName.substringAfterLast(".") &&
                    it.annotationType.resolve().declaration.qualifiedName?.asString() == annotationName
            }
        }.map { it.packageName.asString() }
    }

    @KspExperimental
    override fun getModuleName(): KSName {
        return KSNameImpl.getCached((ktModule.stableModuleName ?: ktModule.moduleName).removeSurrounding("<", ">"))
    }

    @KspExperimental
    override fun mapJavaNameToKotlin(javaName: KSName): KSName? {
        return JavaToKotlinClassMap.mapJavaToKotlin(FqName(javaName.asString()))?.toKSName()
    }

    @KspExperimental
    override fun mapKotlinNameToJava(kotlinName: KSName): KSName? {
        return JavaToKotlinClassMap.mapKotlinToJava(FqNameUnsafe(kotlinName.asString()))?.toKSName()
    }

    @KspExperimental
    override fun mapToJvmSignature(declaration: KSDeclaration): String? {
        return mapToJvmSignatureInternal(declaration)
    }

    internal fun mapToJvmSignature(accessor: KSPropertyAccessor): String {
        return mapToJvmSignatureInternal(accessor) ?: ""
    }

    override fun overrides(overrider: KSDeclaration, overridee: KSDeclaration): Boolean {
        val overriderSymbol = when (overrider) {
            is KSFunctionDeclarationImpl -> if (overrider.ktFunctionSymbol is KtPropertyAccessorSymbol) {
                (overrider.parent as KSPropertyDeclarationImpl).ktPropertySymbol
            } else {
                overrider.ktFunctionSymbol
            }
            is KSPropertyDeclarationImpl -> overrider.ktPropertySymbol
            else -> return false
        }
        val overrideeSymbol = when (overridee) {
            is KSFunctionDeclarationImpl -> overridee.ktFunctionSymbol
            is KSPropertyDeclarationImpl -> overridee.ktPropertySymbol
            else -> return false
        }
        overrider.closestClassDeclaration()?.asStarProjectedType()?.let {
            recordLookupWithSupertypes((it as KSTypeImpl).type)
        }
        recordLookupForPropertyOrMethod(overrider)
        recordLookupForPropertyOrMethod(overridee)
        return analyze {
            overriderSymbol.getAllOverriddenSymbols().contains(overrideeSymbol) ||
                overriderSymbol.getIntersectionOverriddenSymbols().contains(overrideeSymbol)
        }
    }

    override fun overrides(
        overrider: KSDeclaration,
        overridee: KSDeclaration,
        containingClass: KSClassDeclaration
    ): Boolean {
        recordLookupForPropertyOrMethod(overrider)
        recordLookupForPropertyOrMethod(overridee)
        return when (overrider) {
            is KSPropertyDeclaration -> containingClass.getAllProperties().singleOrNull {
                recordLookupForPropertyOrMethod(it)
                it.simpleName.asString() == overrider.simpleName.asString()
            }?.let { overrides(it, overridee) } ?: false
            is KSFunctionDeclaration -> {
                val candidates = containingClass.getAllFunctions().filter {
                    it.simpleName.asString() == overridee.simpleName.asString()
                }
                if (overrider.simpleName.asString().startsWith("get") ||
                    overrider.simpleName.asString().startsWith("set")
                ) {
                    candidates.plus(
                        containingClass.getAllProperties().filter {
                            val overriderName = overrider.simpleName.asString().substring(3)
                                .replaceFirstChar { it.lowercase() }
                            it.simpleName.asString() == overriderName ||
                                it.simpleName.asString().replaceFirstChar { it.lowercase() } == overriderName
                        }
                    ).any { overrides(it, overridee) }
                } else {
                    candidates.any {
                        recordLookupForPropertyOrMethod(it)
                        overrides(it, overridee)
                    }
                }
            }
            else -> false
        }
    }

    internal fun mapToJvmSignatureInternal(ksAnnotated: KSAnnotated): String? {
        fun KtType.toSignature(): String {
            return analyze {
                this@toSignature.mapTypeToJvmType().descriptor.let {
                    when (it) {
                        "Ljava.lang.Void;" -> "Ljava/lang/Void;"
                        "Lkotlin/Unit;" -> "V"
                        else -> it
                    }
                }
            }
        }

        fun KSType.toSignature(): String {
            return if (this is KSTypeImpl) {
                analyze {
                    val decl = (this@toSignature.declaration as? KSClassDeclaration)
                    // special handling for single parameter inline value class
                    // unwrap the underlying for jvm signature.
                    if (
                        decl != null &&
                        (decl.modifiers.contains(Modifier.INLINE) || decl.modifiers.contains(Modifier.VALUE)) &&
                        decl.primaryConstructor?.parameters?.size == 1
                    ) {
                        decl.primaryConstructor!!.parameters.single().type.resolve().toSignature()
                    } else {
                        this@toSignature.type.toSignature()
                    }
                }
            } else {
                "<ERROR>"
            }
        }

        return when (ksAnnotated) {
            is KSClassDeclaration -> analyze {
                (ksAnnotated.asStarProjectedType() as KSTypeImpl).type.mapTypeToJvmType().descriptor
            }
            is KSFunctionDeclarationImpl -> {
                analyze {
                    buildString {
                        append("(")
                        ksAnnotated.parameters.forEach { append(it.type.resolve().toSignature()) }
                        append(")")
                        if (ksAnnotated.isConstructor()) {
                            append("V")
                        } else {
                            append(ksAnnotated.returnType!!.resolve().toSignature())
                        }
                    }
                }
            }
            is KSPropertyDeclaration -> {
                analyze {
                    ksAnnotated.type.resolve().toSignature()
                }
            }
            is KSPropertyAccessorImpl -> {
                analyze {
                    buildString {
                        append("(")
                        ksAnnotated.ktPropertyAccessorSymbol.valueParameters.forEach {
                            append(it.returnType.toSignature())
                        }
                        append(")")
                        append(ksAnnotated.ktPropertyAccessorSymbol.returnType.toSignature())
                    }
                }
            }
            else -> null
        }
    }

    internal fun computeAsMemberOf(property: KSPropertyDeclaration, containing: KSType): KSType {
        val declaredIn = property.closestClassDeclaration()
            ?: throw IllegalArgumentException(
                "Cannot call asMemberOf with a property that is not declared in a class or an interface"
            )
        val key = property to containing
        return propertyAsMemberOfCache.getOrPut(key) {
            val resolved = property.type.resolve()
            if (containing is KSTypeImpl && resolved is KSTypeImpl) {
                recordLookupWithSupertypes(containing.type)
                recordLookupForPropertyOrMethod(property)
                val isSubTypeOf = analyze {
                    (declaredIn.asStarProjectedType() as? KSTypeImpl)?.type?.let {
                        containing.type.isSubTypeOf(it)
                    } ?: false
                }
                if (!isSubTypeOf) {
                    throw IllegalArgumentException(
                        "$containing is not a sub type of the class/interface that contains `$property` ($declaredIn)"
                    )
                }
                analyze {
                    buildSubstitutor {
                        fillInDeepSubstitutor(containing.type, this@buildSubstitutor)
                    }.let {
                        // recursively substitute to ensure transitive substitution works.
                        // should fix in upstream as well.
                        var result = resolved.type
                        var cnt = 0
                        while (it.substitute(result) != result) {
                            if (cnt > 100) {
                                throw IllegalStateException(
                                    "Potential infinite loop in type substitution for computeAsMemberOf"
                                )
                            }
                            result = it.substitute(result)
                            cnt += 1
                        }
                        KSTypeImpl.getCached(result)
                    }
                }
            } else {
                return if (resolved.isError) resolved else KSErrorType(resolved.toString())
            }
        }
    }

    internal fun computeAsMemberOf(function: KSFunctionDeclaration, containing: KSType): KSFunction {
        val propertyDeclaredIn = function.closestClassDeclaration()
            ?: throw IllegalArgumentException(
                "Cannot call asMemberOf with a property that is not declared in a class or an interface"
            )
        val key = function to containing
        return functionAsMemberOfCache.getOrPut(key) {
            if (containing is KSTypeImpl) {
                recordLookupWithSupertypes(containing.type)
                recordLookupForPropertyOrMethod(function)
                val isSubTypeOf = analyze {
                    (propertyDeclaredIn.asStarProjectedType() as? KSTypeImpl)?.type?.let {
                        containing.type.isSubTypeOf(it)
                    } ?: false
                }
                if (!isSubTypeOf) {
                    throw IllegalArgumentException(
                        "$containing is not a sub type of the class/interface that contains `$function` " +
                            "($propertyDeclaredIn)"
                    )
                }
                analyze {
                    buildSubstitutor {
                        fillInDeepSubstitutor(containing.type, this@buildSubstitutor)
                    }.let {
                        // recursively substitute to ensure transitive substitution works.
                        // should fix in upstream as well.
                        // for functions we need to test both returnType and value parameters converges.
                        var funcToSub = (function as KSFunctionDeclarationImpl).ktFunctionSymbol.substitute(it)
                        var next = funcToSub.substitute(it)
                        var cnt = 0
                        while (
                            funcToSub.returnType != next.returnType ||
                            funcToSub.valueParameters.zip(next.valueParameters)
                                .any { it.first.returnType != it.second.returnType } ||
                            funcToSub.receiverType != next.receiverType
                        ) {
                            if (cnt > 100) {
                                throw IllegalStateException(
                                    "Potential infinite loop in type substitution for computeAsMemberOf"
                                )
                            }
                            funcToSub = next
                            next = funcToSub.substitute(it)
                            cnt += 1
                        }
                        funcToSub
                    }.let {
                        KSFunctionImpl(it)
                    }
                }
            } else KSFunctionErrorImpl(function)
        }
    }
}
