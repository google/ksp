package com.google.devtools.ksp.common.visitor

import com.google.devtools.ksp.InternalKSPException
import com.google.devtools.ksp.impl.symbol.kotlin.toLocation
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiCodeBlock
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiImportList
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiPackageStatement
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeElement
import com.intellij.psi.PsiTypeParameter
import com.intellij.psi.javadoc.PsiDocComment
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.psi.KtAnnotatedExpression
import org.jetbrains.kotlin.psi.KtAnnotation
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtDeclarationModifierList
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFileAnnotationList
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtImportList
import org.jetbrains.kotlin.psi.KtObjectLiteralExpression
import org.jetbrains.kotlin.psi.KtPackageDirective

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
            is KtAnnotationEntry -> when (val parent = element.parent) {
                // Annotations on declarations. Examples:
                // @MyAnnotation class MyClass
                // @MyAnnotation val myVal = "";
                // @MyAnnotation fun myFun() {}
                // fun myMethodWithParameter(@MyAnnotation myParam: Int) {}
                is KtDeclarationModifierList -> {
                    result.add(parent.parent)
                    return
                }

                // Annotations at the file level. Example:
                // @file:MyAnnotation
                is KtFileAnnotationList -> {
                    result.add(parent.parent)
                    return
                }

                // Annotations on expressions. Example:
                // val x = @Suppress("UNCHECKED_CAST") list as List<String>
                is KtAnnotatedExpression ->
                    // Do nothing - KSP does not consider expressions / values
                    return

                // A group of annotations. Example:
                // @[MyAnnotation1, MyAnnotation2] class MyClass
                is KtAnnotation -> {
                    result.add(parent.parent.parent)
                    return
                }

                else ->
                    // Explicit crash
                    throw InternalKSPException(
                        "Unexpected Kotlin Psi element",
                        parent.toLocation(),
                        parent.javaClass
                    )
            }

            is PsiAnnotation -> when (val owner = element.owner) {
                // Annotations on declarations. Examples:
                // @MyAnnotation class MyClass {}
                // @MyAnnotation String myField = "";
                // @MyAnnotation void myMethod() {}
                // void myMethodWithParameter(@MyAnnotation int myParam) {}
                is PsiModifierList -> {
                    result.add(owner.parent)
                    return
                }

                // Type parameter annotations. Examples:
                // class MyClass<@MyAnnotation A>
                // fun <@MyAnnotation A> myFun() {}
                is PsiTypeParameter -> {
                    result.add(owner)
                    return
                }

                is PsiTypeElement -> {
                    result.add(owner)
                    return
                }

                // Type argument annotation / type application. Example:
                // interface <A> Foo<A> {}
                // abstract class Abs {
                //     Foo<String> bar(Foo2<@MyAnnotation Boolean> baz)
                //     Foo<@MyAnnotation A> baz(Foo2<A> baz)
                // }
                is PsiType ->
                    // Do nothing - Analysis API implementation does not collect these
                    return

                // Annotations used as values. Example with other annotation used as default value
                // @interface MyAnnotation {
                //     MyOtherAnnotation value() default @MyOtherAnnotation("default value here");
                // }
                null ->
                    // Do nothing - Analysis API implementation does not collect these
                    return

                else ->
                    // Explicit crash
                    throw InternalKSPException(
                        "Unexpected Java Psi element",
                        (owner as? PsiElement).toLocation(),
                        owner.javaClass
                    )
            }
        }
        super.visitElement(element)
    }

    /**
     * Returns `true` if the [PsiElement] is skippable.
     *
     * An element is skippable if one of the following holds:
     * - It is a package directive
     * - It is an import list
     * - It is a doc comment
     * - It is a Kotlin function or Java method body
     * - It is a Kotlin object expression or Java anonymous class
     */
    private fun PsiElement.isSkippable(): Boolean =
        isPackageDirective() ||
            isImport() ||
            isDocComment() ||
            isFunctionOrMethodBody() ||
            isObjectOrAnonymousClass()

    private fun PsiElement.isPackageDirective(): Boolean =
        isKotlinPackageDirective() || isJavaPackageDirective()

    /**
     * Returns `true` if the [PsiElement] is either a Kotlin or Java import list.
     */
    private fun PsiElement.isImport(): Boolean =
        isKotlinImportList() || isJavaImportList()

    /**
     * Return `true` if the [PsiElement] is either a Kotlin or Java doc-comment.
     */
    private fun PsiElement.isDocComment(): Boolean =
        isKotlinDocComment() || isJavaDocComment()

    /**
     * Return `true` if the [PsiElement] is either a Kotlin function body or a Java method body.
     */
    private fun PsiElement.isFunctionOrMethodBody(): Boolean =
        isKotlinFunctionBody() || isJavaMethodBody()

    /**
     * Return `true` if the [PsiElement] is either a Kotlin object expression or a Java anonymous class.
     */
    private fun PsiElement.isObjectOrAnonymousClass(): Boolean =
        isKotlinObjectExpression() || isJavaAnonymousClass()

    /**
     * Returns `true` is the [PsiElement] is a Kotlin package directive, e.g., `package com.example`
     */
    private fun PsiElement.isKotlinPackageDirective(): Boolean =
        this is KtPackageDirective

    /**
     * Returns `true` is the [PsiElement] is a Java package directive, e.g., `package com.example`
     */
    private fun PsiElement.isJavaPackageDirective(): Boolean =
        this is PsiPackageStatement

    /**
     * Returns `true` if the [PsiElement] is a Kotlin import list.
     */
    private fun PsiElement.isKotlinImportList(): Boolean =
        this is KtImportList

    /**
     * Returns `true` if the [PsiElement] is a Java import list.
     */
    private fun PsiElement.isJavaImportList(): Boolean =
        this is PsiImportList

    /**
     * Returns `true` if the [PsiElement] is a Kotlin doc comment.
     */
    private fun PsiElement.isKotlinDocComment(): Boolean =
        this is KDoc

    /**
     * Returns `true` if the [PsiElement] is a Java doc-comment.
     */
    private fun PsiElement.isJavaDocComment(): Boolean =
        this is PsiDocComment

    /**
     * Returns `true` if the [PsiElement] is a Kotlin function body.
     */
    private fun PsiElement.isKotlinFunctionBody(): Boolean = when (this) {
        is KtExpression -> when (val parent = this.parent) {
            is KtFunction ->
                parent.bodyExpression == this || parent.bodyBlockExpression == this

            else -> false
        }

        else -> false
    }

    /**
     * Returns `true` if the [PsiElement] is a Java method body.
     */
    private fun PsiElement.isJavaMethodBody(): Boolean =
        this is PsiExpression ||
            (
                this is PsiCodeBlock &&
                    this.parent is PsiMethod
                )

    /**
     * Returns `true` if the [PsiElement] is a Kotlin object expression.
     */
    private fun PsiElement.isKotlinObjectExpression(): Boolean =
        this is KtObjectLiteralExpression

    /**
     * Returns `true` if the [PsiElement] is a Java anonymous class.
     */
    private fun PsiElement.isJavaAnonymousClass(): Boolean =
        this is PsiAnonymousClass
}
