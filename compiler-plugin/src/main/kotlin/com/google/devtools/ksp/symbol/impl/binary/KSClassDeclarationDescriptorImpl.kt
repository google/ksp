/*
 * Copyright 2020 Google LLC
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
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


package com.google.devtools.ksp.symbol.impl.binary

import com.google.devtools.ksp.ExceptionMessage
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.impl.ResolverImpl
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.KSObjectCache
import com.google.devtools.ksp.symbol.impl.kotlin.getKSTypeCached
import com.google.devtools.ksp.symbol.impl.replaceTypeArguments
import com.google.devtools.ksp.symbol.impl.toKSModifiers
import com.jetbrains.rd.util.first
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.types.typeUtil.replaceArgumentsWithStarProjections
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.util.*
import kotlin.Comparator
import org.jetbrains.kotlin.descriptors.ClassKind as KtClassKind

class KSClassDeclarationDescriptorImpl private constructor(val descriptor: ClassDescriptor) : KSClassDeclaration,
    KSDeclarationDescriptorImpl(descriptor),
    KSExpectActual by KSExpectActualDescriptorImpl(descriptor) {
    companion object : KSObjectCache<ClassDescriptor, KSClassDeclarationDescriptorImpl>() {
        fun getCached(descriptor: ClassDescriptor) = cache.getOrPut(descriptor) { KSClassDeclarationDescriptorImpl(descriptor) }
    }

    override val classKind: ClassKind by lazy {
        when (descriptor.kind) {
            KtClassKind.INTERFACE -> ClassKind.INTERFACE
            KtClassKind.CLASS -> ClassKind.CLASS
            KtClassKind.OBJECT -> ClassKind.OBJECT
            KtClassKind.ENUM_CLASS -> ClassKind.ENUM_CLASS
            KtClassKind.ENUM_ENTRY -> ClassKind.ENUM_ENTRY
            KtClassKind.ANNOTATION_CLASS -> ClassKind.ANNOTATION_CLASS
        }
    }

    override val isCompanionObject by lazy {
        descriptor.isCompanionObject
    }

    override fun getAllFunctions(): List<KSFunctionDeclaration> = descriptor.getAllFunctions()

    override fun getAllProperties(): List<KSPropertyDeclaration> = descriptor.getAllProperties()

    override val primaryConstructor: KSFunctionDeclaration? by lazy {
        descriptor.unsubstitutedPrimaryConstructor?.let { KSFunctionDeclarationDescriptorImpl.getCached(it) }
    }

    // Workaround for https://github.com/google/ksp/issues/195
    private val mockSerializableType = ResolverImpl.instance.mockSerializableType
    private val javaSerializableType = ResolverImpl.instance.javaSerializableType

    override val superTypes: List<KSTypeReference> by lazy {
        descriptor.defaultType.constructor.supertypes.map {
            KSTypeReferenceDescriptorImpl.getCached(
                if (it === mockSerializableType) javaSerializableType else it
            )
        }
    }

    override val typeParameters: List<KSTypeParameter> by lazy {
        descriptor.declaredTypeParameters.map { KSTypeParameterDescriptorImpl.getCached(it) }
    }

    override val declarations: List<KSDeclaration> by lazy {
        // taken from: https://github.com/JetBrains/kotlin/blob/master/compiler/frontend.java/src/org/jetbrains/kotlin/load/kotlin/kotlinJvmBinaryClassUtil.kt
        val declarationOrdering = descriptor.safeAs<DeserializedClassDescriptor>()
            ?.source.safeAs<KotlinJvmBinarySourceElement>()?.binaryClass?.let {
                DeclarationOrdering(it)
            }
        listOf(descriptor.unsubstitutedMemberScope.getDescriptorsFiltered(), descriptor.staticScope.getDescriptorsFiltered()).flatten()
            .filter {
                it is MemberDescriptor
                        && it.visibility != DescriptorVisibilities.INHERITED
                        && it.visibility != DescriptorVisibilities.INVISIBLE_FAKE
                        && (it !is CallableMemberDescriptor || it.kind != CallableMemberDescriptor.Kind.FAKE_OVERRIDE)
            }
            .map {
                when (it) {
                    is PropertyDescriptor -> KSPropertyDeclarationDescriptorImpl.getCached(it)
                    is FunctionDescriptor -> KSFunctionDeclarationDescriptorImpl.getCached(it)
                    is ClassDescriptor -> getCached(it)
                    else -> throw IllegalStateException("Unexpected descriptor type ${it.javaClass}, $ExceptionMessage")
                }
            }.let {
                if (declarationOrdering != null) {
                    it.sortedWith(declarationOrdering.comparator)
                } else {
                    it
                }

            }
    }

    override val modifiers: Set<Modifier> by lazy {
        val modifiers = mutableSetOf<Modifier>()
        modifiers.addAll(descriptor.toKSModifiers())
        if (descriptor.isData) {
            modifiers.add(Modifier.DATA)
        }
        if (descriptor.isInline) {
            modifiers.add(Modifier.INLINE)
        }
        if (descriptor.kind == KtClassKind.ANNOTATION_CLASS) {
            modifiers.add(Modifier.ANNOTATION)
        }
        if (descriptor.isInner) {
            modifiers.add(Modifier.INNER)
        }
        modifiers
    }

    override fun asType(typeArguments: List<KSTypeArgument>): KSType =
        getKSTypeCached(descriptor.defaultType.replaceTypeArguments(typeArguments), typeArguments)

    override fun asStarProjectedType(): KSType {
        return getKSTypeCached(descriptor.defaultType.replaceArgumentsWithStarProjections())
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitClassDeclaration(this, data)
    }
}

internal fun ClassDescriptor.getAllFunctions(explicitConstructor: Boolean = false): List<KSFunctionDeclaration> {
    ResolverImpl.instance.incrementalContext.recordLookupForGetAllFunctions(this)
    val functionDescriptors = unsubstitutedMemberScope.getDescriptorsFiltered(DescriptorKindFilter.FUNCTIONS).toList()
            .filter { (it as FunctionDescriptor).visibility != DescriptorVisibilities.INVISIBLE_FAKE }.toMutableList()
    if (explicitConstructor)
        functionDescriptors += constructors
    return functionDescriptors.map { KSFunctionDeclarationDescriptorImpl.getCached(it as FunctionDescriptor) }
}

internal fun ClassDescriptor.getAllProperties(): List<KSPropertyDeclaration> {
    ResolverImpl.instance.incrementalContext.recordLookupForGetAllProperties(this)
    return unsubstitutedMemberScope.getDescriptorsFiltered(DescriptorKindFilter.VARIABLES).toList()
            .filter { (it as PropertyDescriptor).visibility != DescriptorVisibilities.INVISIBLE_FAKE }
            .map { KSPropertyDeclarationDescriptorImpl.getCached(it as PropertyDescriptor) }
}


/**
 * Helper class to read the order of fields/methods in a .class file compiled from Kotlin.
 *
 * When a compiled Kotlin class is read from descriptors, the order of fields / methods do not match
 * the order in the original source file (or the .class file).
 * This helper class reads the order from the binary class (using the visitor API) and allows
 * [KSClassDeclarationDescriptorImpl] to sort its declarations based on the .class file.
 *
 * Note that the ordering is relevant only for fields and methods. For any other declaration, the
 * order that was returned from the descriptor API is kept.
 *
 * see: https://github.com/google/ksp/issues/250
 */
internal class DeclarationOrdering(
    binaryClass: KotlinJvmBinaryClass
) : KotlinJvmBinaryClass.MemberVisitor {
    // Map of fieldName -> Order
    private val fieldOrdering = mutableMapOf<String, Int>()
    // Map of method name to (jvm desc -> Order) map
    // multiple methods might have the same name, hence we need to use signature matching for
    // methods. That being said, we only do it when we find multiple methods with the same name
    // otherwise, there is no reason to compute the jvm signature.
    private val methodOrdering = mutableMapOf<String, MutableMap<String, Int>>()
    // This map is built while we are sorting to ensure for the same declaration, we return the same
    // order, in case it is not found in fields / methods.
    private val declOrdering = IdentityHashMap<KSDeclaration, Int>()
    // Helper class to generate ids that can be used for comparison.
    private val orderProvider = OrderProvider()

    init {
        binaryClass.visitMembers(this, null)
        orderProvider.seal()
    }

    val comparator = Comparator<KSDeclarationDescriptorImpl> { first, second ->
        getOrder(first).compareTo(getOrder(second))
    }

    @OptIn(KspExperimental::class)
    private fun getOrder(decl: KSDeclarationDescriptorImpl): Int {
        return declOrdering.getOrPut(decl) {
            when(decl) {
                is KSPropertyDeclarationDescriptorImpl -> {
                    fieldOrdering[decl.simpleName.asString()]?.let {
                        return@getOrPut it
                    }
                    // might be a property without backing field. Use method ordering instead
                    decl.getter?.let { getter ->
                        return@getOrPut findMethodOrder(
                            ResolverImpl.instance.getJvmName(getter)
                        ) {
                            ResolverImpl.instance.mapToJvmSignature(getter)
                        }
                    }
                    decl.setter?.let { setter ->
                        return@getOrPut findMethodOrder(
                            ResolverImpl.instance.getJvmName(setter)
                        ) {
                            ResolverImpl.instance.mapToJvmSignature(setter)
                        }
                    }
                    orderProvider.next(decl)
                }
                is KSFunctionDeclarationDescriptorImpl -> {
                    findMethodOrder(
                        ResolverImpl.instance.getJvmName(decl)
                    ) {
                        ResolverImpl.instance.mapToJvmSignature(decl)
                    }
                }
                else -> orderProvider.nextIgnoreSealed()
            }
        }
    }

    private inline fun findMethodOrder(
        jvmName:String,
        crossinline getJvmDesc: ()-> String
    ): Int {
        val methods = methodOrdering[jvmName]
        // if there is 1 method w/ that name, just return.
        // otherwise, we need signature matching
        return when {
            methods == null -> {
                orderProvider.next(jvmName)
            }
            methods.size == 1 -> {
                // only 1 method with this name, return it, no reason to resolve jvm
                // signature
                methods.first().value
            }
            else -> {
                // need to match using the jvm signature
                val jvmDescriptor = getJvmDesc()
                methods.getOrPut(jvmDescriptor) {
                    orderProvider.next(jvmName)
                }
            }
        }
    }
    override fun visitField(
        name: Name,
        desc: String,
        initializer: Any?
    ): KotlinJvmBinaryClass.AnnotationVisitor? {
        fieldOrdering.getOrPut(name.asString()) {
            orderProvider.next(name)
        }
        return null
    }

    override fun visitMethod(
        name: Name,
        desc: String
    ): KotlinJvmBinaryClass.MethodAnnotationVisitor? {
        methodOrdering.getOrPut(name.asString()) {
            mutableMapOf()
        }.put(desc, orderProvider.next(name))
        return null
    }

    /**
     * Helper class to generate order values for items.
     * Each time we see a new declaration, we give it an increasing order.
     *
     * This provider can also run in STRICT MODE to ensure that if we don't find an expected value
     * during sorting, we can crash instead of picking the next ID. For now, it is only used for
     * testing.
     */
    private class OrderProvider {
        private var nextId = 0
        private var sealed = false

        /**
         * Seals the provider, preventing it from generating new IDs if [STRICT_MODE] is enabled.
         */
        fun seal() {
            sealed = true
        }

        /**
         * Returns the next available order value.
         *
         * @param ref Used for logging if the data is sealed and we shouldn't provide a new order.
         */
        fun next(ref: Any) : Int {
            check(!sealed || !STRICT_MODE) {
                "couldn't find item $ref"
            }
            return nextId ++
        }

        /**
         * Returns the next ID without checking whether the model is sealed or not. This is useful
         * for declarations where we don't care about the order (e.g. inner class declarations).
         */
        fun nextIgnoreSealed() : Int {
            return nextId ++
        }

    }
    companion object {
        /**
         * Used in tests to prevent fallback behavior of creating a new ID when we cannot find the
         * order.
         */
        var STRICT_MODE = false
    }
}