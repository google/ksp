package com.google.devtools.ksp.processing.impl

import com.google.devtools.ksp.symbol.Location
import com.intellij.psi.PsiFile

// PsiElement.toLocation() isn't available before ResolveImpl is initialized.
class KSPCompilationError(val file: PsiFile, val offset: Int, override val message: String) : Exception()
