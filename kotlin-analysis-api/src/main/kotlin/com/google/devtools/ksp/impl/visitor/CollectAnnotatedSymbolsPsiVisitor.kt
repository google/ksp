package com.google.devtools.ksp.impl.visitor

import com.google.devtools.ksp.impl.symbol.kotlin.toLocation
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiCodeBlock
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import com.intellij.psi.PsiTypeElement
import com.intellij.psi.PsiTypeParameter
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtDeclarationModifierList
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFileAnnotationList
import org.jetbrains.kotlin.psi.KtFunction

/**
 * A [PsiRecursiveElementWalkingVisitor] that collects [PsiElement]s which
 * are annotated at least once. It does **not** traverse function or method bodies, i.e.,
 * always assumes `inDepth = false`.
 */
class CollectAnnotatedSymbolsPsiVisitor : PsiRecursiveElementWalkingVisitor() {

    val result: MutableSet<PsiElement> = mutableSetOf()

    override fun visitElement(element: PsiElement) {
        if (element.isSkippable()) {
            return
        }
        when (element) {
            // For all annotated elements in Kotlin sources, we expect the structure:
            //   KtAnnotationEntry -> KtDeclarationModifierList / KtFileAnnotationList -> Annotated Element
            is KtAnnotationEntry -> when (val parent = element.parent) {
                is KtDeclarationModifierList -> {
                    result.add(parent.parent)
                    return
                }

                is KtFileAnnotationList -> {
                    result.add(parent.parent)
                    return
                }

                else ->
                    // Explicit crash
                    error("Unexpected Kotlin Psi element at ${parent.toLocation()}: ${parent.javaClass}")
            }

            // For Java sources, we expect annotated type parameters to have the structure:
            //   PsiAnnotation -> Annotated Element
            // For all other annotated elements, we expect the structure:
            //   PsiAnnotation -> PsiModifierList -> Annotated Element
            is PsiAnnotation -> when (val parent = element.parent) {
                is PsiTypeParameter ->
                    result.add(parent)

                is PsiModifierList ->
                    result.add(parent.parent)

                is PsiTypeElement -> // Type argument (type application)
                    result.add(parent)

                else ->
                    // Explicit crash
                    error("Unexpected Java Psi element at ${parent.toLocation()}: ${parent.javaClass}")
            }
        }
        super.visitElement(element)
    }

    /**
     * Returns `true` if the [PsiElement] is skippable.
     *
     * An element is skippable if one of the following holds:
     * - It is a Kotlin function body
     * - It is a Java method body
     * - It is a Java anonymous class
     */
    private fun PsiElement.isSkippable(): Boolean =
        // TODO: Skip elements we are not interested in visiting, e.g., imports.
        isKotlinFunctionBody() ||
            isJavaMethodBody() ||
            isJavaAnonymousClass()

    /**
     * Returns `true` if the [PsiElement] is a Kotlin function body.
     */
    private fun PsiElement.isKotlinFunctionBody(): Boolean =
        this is KtExpression &&
            this.parent is KtFunction &&
            (this.parent as KtFunction).bodyExpression == this

    /**
     * Returns `true` if the [PsiElement] is a Java method body.
     */
    private fun PsiElement.isJavaMethodBody(): Boolean =
        this is PsiCodeBlock &&
            this.parent is PsiMethod

    /**
     * Returns `true` if the [PsiElement] is a Java anonymous class.
     */
    private fun PsiElement.isJavaAnonymousClass(): Boolean =
        this is PsiAnonymousClass
}
