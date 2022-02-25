package com.google.devtools.ksp.impl

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.analysis.api.standalone.configureApplicationEnvironment
import org.jetbrains.kotlin.analysis.api.standalone.configureProjectEnvironment
import org.jetbrains.kotlin.analysis.api.analyseWithReadAction
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.references.*
import org.jetbrains.kotlin.psi.KtFile
import java.io.File
import org.jetbrains.kotlin.cli.jvm.config.addJavaSourceRoot

fun findSomeReference(ktFile: KtFile): KtReference? {
    for (i in 1..300) {
        val reference = ktFile.findReferenceAt(i)
        if (reference != null && reference is KtReference)
            return reference
    }

    return null
}

private fun convertFilesToKtFiles(project: Project, files: List<File>): List<KtFile> {
    val fs = StandardFileSystems.local()
    val psiManager = PsiManager.getInstance(project)
    val ktFiles = mutableListOf<KtFile>()
    for (file in files) {
        val vFile = fs.findFileByPath(file.absolutePath) ?: continue
        val ktFile = psiManager.findFile(vFile) as? KtFile ?: continue
        ktFiles.add(ktFile)
    }
    return ktFiles
}

fun main() {
    val compilerConfiguration = CompilerConfiguration()
    compilerConfiguration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
    val env = KotlinCoreEnvironment.createForProduction(
        Disposer.newDisposable(), compilerConfiguration,
        EnvironmentConfigFiles.JVM_CONFIG_FILES
    )

    val application = ApplicationManager.getApplication() as MockApplication
    configureApplicationEnvironment(application)

    val file = File("testData/api/hello.kt").absoluteFile
    compilerConfiguration.addJavaSourceRoot(file)

    val project = env.project as MockProject
    val ktFiles = convertFilesToKtFiles(project, listOf(file))
    configureProjectEnvironment(
        project,
        compilerConfiguration,
        ktFiles,
        env::createPackagePartProvider,
        env.projectEnvironment.environment.jarFileSystem as CoreJarFileSystem
    )

    for (ktFile in ktFiles) {
        analyseWithReadAction(ktFile) {
            val fileSymbol = ktFile.getFileSymbol()
            val members = fileSymbol.getFileScope().getAllSymbols()
            members.filterIsInstance<KtFunctionSymbol>()
        }
    }
}
