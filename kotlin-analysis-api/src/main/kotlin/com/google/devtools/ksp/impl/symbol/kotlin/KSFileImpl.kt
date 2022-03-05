package com.google.devtools.ksp.impl.symbol.kotlin

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.Origin
import org.jetbrains.kotlin.analysis.api.analyseWithReadAction
import org.jetbrains.kotlin.analysis.api.annotations.annotations
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.psi.KtFile

class KSFileImpl(private val ktFile: KtFile) : KSFile {
    override val packageName: KSName by lazy {
        KSNameImpl(ktFile.packageFqName.asString())
    }
    override val fileName: String by lazy {
        ktFile.name
    }
    override val filePath: String by lazy {
        ktFile.virtualFilePath
    }
    override val declarations: Sequence<KSDeclaration> by lazy {
        analyseWithReadAction(ktFile) {
            ktFile.getFileSymbol().getFileScope().getAllSymbols().map {
                when (it) {
                    is KtNamedClassOrObjectSymbol -> KSClassDeclarationImpl(it)
                    is KtFunctionSymbol -> KSFunctionDeclarationImpl(it)
                    is KtPropertySymbol -> KSPropertyDeclarationImpl(it)
                    else -> throw IllegalStateException("Unhandled ")
                }
            }
        }
    }
    override val origin: Origin = Origin.KOTLIN

    override val location: Location by lazy {
        ktFile.toLocation()
    }

    override val parent: KSNode? = null

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitFile(this, data)
    }

    override val annotations: Sequence<KSAnnotation> by lazy {
        analyseWithReadAction(ktFile) {
            ktFile.getFileSymbol().annotations.map { KSAnnotationImpl(it) }.asSequence()
        }
    }
}
