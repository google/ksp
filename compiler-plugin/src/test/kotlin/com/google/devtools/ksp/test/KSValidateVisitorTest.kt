package com.google.devtools.ksp.test

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.FunctionKind
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunction
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSPropertyGetter
import com.google.devtools.ksp.symbol.KSPropertySetter
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Nullability
import com.google.devtools.ksp.symbol.Origin
import com.google.devtools.ksp.symbol.Variance
import com.google.devtools.ksp.symbol.impl.kotlin.KSErrorType
import com.google.devtools.ksp.symbol.impl.synthetic.KSTypeReferenceSyntheticImpl
import com.google.devtools.ksp.validate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KSValidateVisitorTest {

    @Test
    fun validate_success_on_skipping_function() {
        val result = FakeKSClassDeclaration(
            declarations = sequenceOf(KSFakeFunctionDeclaration(KSTypeReferenceSyntheticImpl(KSErrorType, null)))
        ).validate { node, _ ->
            node !is KSFunctionDeclaration
        }

        assertTrue(result)
    }

    @Test
    fun validates_property_declaration_fail_on_type_fail() {
        val result = FakeKSClassDeclaration(
            declarations = sequenceOf(
                FakeKSPropertyDeclaration(
                    KSTypeReferenceSyntheticImpl(
                        FakeKSType(
                            arguments = listOf(
                                FakeKSTypeArgument(KSTypeReferenceSyntheticImpl(KSErrorType, null))
                            )
                        ),
                        null
                    )
                )
            )
        ).validate()

        assertFalse(result)
    }
}

private class FakeKSClassDeclaration(
    private val type: KSType = FakeKSType(),
    override val declarations: Sequence<KSDeclaration> = emptySequence()
) : KSClassDeclaration {
    override val classKind: ClassKind
        get() = TODO("Not yet implemented")
    override val primaryConstructor: KSFunctionDeclaration? = null
    override val superTypes: Sequence<KSTypeReference> = emptySequence()
    override val isCompanionObject: Boolean = false

    override fun getSealedSubclasses(): Sequence<KSClassDeclaration> {
        TODO("Not yet implemented")
    }

    override fun getAllFunctions(): Sequence<KSFunctionDeclaration> =
        declarations.filterIsInstance<KSFunctionDeclaration>()

    override fun getAllProperties(): Sequence<KSPropertyDeclaration> {
        TODO("Not yet implemented")
    }

    override fun asType(typeArguments: List<KSTypeArgument>): KSType {
        TODO("Not yet implemented")
    }

    override fun asStarProjectedType(): KSType = type

    override val simpleName: KSName
        get() = TODO("Not yet implemented")
    override val qualifiedName: KSName?
        get() = TODO("Not yet implemented")
    override val typeParameters: List<KSTypeParameter> = emptyList()
    override val packageName: KSName
        get() = TODO("Not yet implemented")
    override val parentDeclaration: KSDeclaration?
        get() = TODO("Not yet implemented")
    override val containingFile: KSFile?
        get() = TODO("Not yet implemented")
    override val docString: String?
        get() = TODO("Not yet implemented")
    override val modifiers: Set<Modifier>
        get() = TODO("Not yet implemented")
    override val origin: Origin
        get() = TODO("Not yet implemented")
    override val location: Location
        get() = TODO("Not yet implemented")
    override val parent: KSNode?
        get() = TODO("Not yet implemented")

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitClassDeclaration(this, data)
    }

    override val annotations: Sequence<KSAnnotation> = emptySequence()
    override val isActual: Boolean
        get() = TODO("Not yet implemented")
    override val isExpect: Boolean
        get() = TODO("Not yet implemented")

    override fun findActuals(): Sequence<KSDeclaration> {
        TODO("Not yet implemented")
    }

    override fun findExpects(): Sequence<KSDeclaration> {
        TODO("Not yet implemented")
    }
}

private class KSFakeFunctionDeclaration(
    override val returnType: KSTypeReference? = null
) : KSFunctionDeclaration {
    override val functionKind: FunctionKind
        get() = TODO("Not yet implemented")
    override val isAbstract: Boolean
        get() = TODO("Not yet implemented")
    override val extensionReceiver: KSTypeReference?
        get() = TODO("Not yet implemented")
    override val parameters: List<KSValueParameter> = emptyList()

    override fun findOverridee(): KSDeclaration? {
        TODO("Not yet implemented")
    }

    override fun asMemberOf(containing: KSType): KSFunction {
        TODO("Not yet implemented")
    }

    override val simpleName: KSName
        get() = TODO("Not yet implemented")
    override val qualifiedName: KSName?
        get() = TODO("Not yet implemented")
    override val typeParameters: List<KSTypeParameter> = emptyList()
    override val packageName: KSName
        get() = TODO("Not yet implemented")
    override val parentDeclaration: KSDeclaration?
        get() = TODO("Not yet implemented")
    override val containingFile: KSFile?
        get() = TODO("Not yet implemented")
    override val docString: String?
        get() = TODO("Not yet implemented")
    override val modifiers: Set<Modifier>
        get() = TODO("Not yet implemented")
    override val origin: Origin
        get() = TODO("Not yet implemented")
    override val location: Location
        get() = TODO("Not yet implemented")
    override val parent: KSNode?
        get() = TODO("Not yet implemented")

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitFunctionDeclaration(this, data)
    }

    override val annotations: Sequence<KSAnnotation> = emptySequence()
    override val isActual: Boolean
        get() = TODO("Not yet implemented")
    override val isExpect: Boolean
        get() = TODO("Not yet implemented")

    override fun findActuals(): Sequence<KSDeclaration> {
        TODO("Not yet implemented")
    }

    override fun findExpects(): Sequence<KSDeclaration> {
        TODO("Not yet implemented")
    }

    override val declarations: Sequence<KSDeclaration>
        get() = TODO("Not yet implemented")
}

private class FakeKSPropertyDeclaration(
    override val type: KSTypeReference
) : KSPropertyDeclaration {
    override val getter: KSPropertyGetter?
        get() = TODO("Not yet implemented")
    override val setter: KSPropertySetter?
        get() = TODO("Not yet implemented")
    override val extensionReceiver: KSTypeReference?
        get() = TODO("Not yet implemented")
    override val isMutable: Boolean
        get() = TODO("Not yet implemented")
    override val hasBackingField: Boolean
        get() = TODO("Not yet implemented")

    override fun isDelegated(): Boolean {
        TODO("Not yet implemented")
    }

    override fun findOverridee(): KSPropertyDeclaration? {
        TODO("Not yet implemented")
    }

    override fun asMemberOf(containing: KSType): KSType {
        TODO("Not yet implemented")
    }

    override val simpleName: KSName
        get() = TODO("Not yet implemented")
    override val qualifiedName: KSName?
        get() = TODO("Not yet implemented")
    override val typeParameters: List<KSTypeParameter> = emptyList()
    override val packageName: KSName
        get() = TODO("Not yet implemented")
    override val parentDeclaration: KSDeclaration?
        get() = TODO("Not yet implemented")
    override val containingFile: KSFile?
        get() = TODO("Not yet implemented")
    override val docString: String?
        get() = TODO("Not yet implemented")
    override val modifiers: Set<Modifier>
        get() = TODO("Not yet implemented")
    override val origin: Origin
        get() = TODO("Not yet implemented")
    override val location: Location
        get() = TODO("Not yet implemented")
    override val parent: KSNode?
        get() = TODO("Not yet implemented")

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitPropertyDeclaration(this, data)
    }

    override val annotations: Sequence<KSAnnotation> = emptySequence()
    override val isActual: Boolean
        get() = TODO("Not yet implemented")
    override val isExpect: Boolean
        get() = TODO("Not yet implemented")

    override fun findActuals(): Sequence<KSDeclaration> {
        TODO("Not yet implemented")
    }

    override fun findExpects(): Sequence<KSDeclaration> {
        TODO("Not yet implemented")
    }
}

private class FakeKSType(
    override val arguments: List<KSTypeArgument> = emptyList()
) : KSType {
    override val declaration: KSDeclaration
        get() = TODO("Not yet implemented")
    override val nullability: Nullability
        get() = TODO("Not yet implemented")
    override val annotations: Sequence<KSAnnotation>
        get() = TODO("Not yet implemented")

    override fun isAssignableFrom(that: KSType): Boolean {
        TODO("Not yet implemented")
    }

    override fun isMutabilityFlexible(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isCovarianceFlexible(): Boolean {
        TODO("Not yet implemented")
    }

    override fun replace(arguments: List<KSTypeArgument>): KSType {
        TODO("Not yet implemented")
    }

    override fun starProjection(): KSType {
        TODO("Not yet implemented")
    }

    override fun makeNullable(): KSType {
        TODO("Not yet implemented")
    }

    override fun makeNotNullable(): KSType {
        TODO("Not yet implemented")
    }

    override val isMarkedNullable: Boolean
        get() = TODO("Not yet implemented")
    override val isError: Boolean = false
    override val isFunctionType: Boolean
        get() = TODO("Not yet implemented")
    override val isSuspendFunctionType: Boolean
        get() = TODO("Not yet implemented")
}

private class FakeKSTypeArgument(
    override val type: KSTypeReference? = null
) : KSTypeArgument {

    override val annotations: Sequence<KSAnnotation>
        get() = TODO("Not yet implemented")
    override val origin: Origin
        get() = TODO("Not yet implemented")
    override val location: Location
        get() = TODO("Not yet implemented")
    override val parent: KSNode?
        get() = TODO("Not yet implemented")

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitTypeArgument(this, data)
    }

    override val variance: Variance
        get() = TODO("Not yet implemented")
}
