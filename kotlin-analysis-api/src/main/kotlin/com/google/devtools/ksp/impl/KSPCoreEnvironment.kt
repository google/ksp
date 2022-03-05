package com.google.devtools.ksp.impl

import com.intellij.mock.MockProject
import com.intellij.psi.PsiDocumentManager

class KSPCoreEnvironment(private val project: MockProject) {
    companion object {
        lateinit var instance: KSPCoreEnvironment
    }
    init {
        instance = this
    }
    val psiDocumentManager = PsiDocumentManager.getInstance(project)
}
