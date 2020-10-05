package com.google.devtools.ksp.symbol
import com.google.devtools.ksp.processing.Resolver
/**
 * Represents the [KSType]s of a function.
 *
 * @see Resolver.asMemberOf
 */
interface KSFunctionType {
    /**
     * The return type of the function. Note that this might be `null` if an error happened while
     * the the is resolved.
     *
     * @see KSFunctionDeclaration.returnType
     */
    val returnType: KSType?
    /**
     * The types of the value parameters of the function. Note that this list might have `null`
     * values in it if the type of a parameter could not be resolved.
     *
     * @see KSFunctionDeclaration.parameters
     */
    val parametersTypes: List<KSType?>

    /**
     * The type parameters of the function.
     *
     * @see KSFunctionDeclaration.typeParameters
     */
    val typeParameters: List<KSTypeParameter>

    /**
     * The receiver type for the function.
     *
     * @see KSFunctionDeclaration.extensionReceiver
     */
    val extensionReceiverType: KSType?
}