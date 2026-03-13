package com.google.devtools.ksp.impl.visitor

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiCodeBlock
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunction

class CollectAnnotatedSymbolsPsiVisitor : PsiRecursiveElementWalkingVisitor() {

    val result: MutableSet<PsiElement> = mutableSetOf()

    override fun visitElement(element: PsiElement) {
        if (
            element is KtExpression &&
            element.parent is KtFunction &&
            (element.parent as KtFunction).bodyExpression == element
        ) {
            return
        }
        if (element is PsiCodeBlock && element.parent is PsiMethod) {
            return
        }
        // TODO: Skip elements we are not interested in visiting, e.g., imports.
        when (element) {
            // TODO: Add comment explaining why we are interested in parent's parent and fall back to parent
            //  Answer: structure of Psi and fallback on type parameters.
            is KtAnnotationEntry -> { // Handle Kotlin sources
                // TODO: Revert this
                val parentsParent = element.parent?.parent as? KtAnnotated
                val immediateParent = element.parent
                result.add(parentsParent ?: immediateParent)
                return
            }

            is PsiAnnotation -> { // Handle Java sources
                // TODO: Revert this
                val immediateParent = element.parent
                val parentsParent = (immediateParent as? PsiModifierList)?.parent
                result.add(parentsParent ?: immediateParent)
                return
            }
        }
        super.visitElement(element)
    }
}
