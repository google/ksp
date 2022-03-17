package com.google.devtools.ksp.impl

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.analysis.api.analyseWithReadAction
import org.jetbrains.kotlin.analysis.api.standalone.configureApplicationEnvironment
import org.jetbrains.kotlin.analysis.api.standalone.configureProjectEnvironment
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoots
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.KtFile
import java.io.File
import java.nio.file.Files

internal fun convertFilesToKtFiles(project: Project, files: List<File>): List<KtFile> {
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

fun main(args: Array<String>) {
    val kotlinSourceRoots = args.toList().map { File(it) }
    val compilerConfiguration = CompilerConfiguration()
    compilerConfiguration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
    val env = KotlinCoreEnvironment.createForProduction(
        Disposer.newDisposable(), compilerConfiguration,
        EnvironmentConfigFiles.JVM_CONFIG_FILES
    )

    val application = ApplicationManager.getApplication() as MockApplication
    configureApplicationEnvironment(application)

    val files = kotlinSourceRoots
        .sortedBy { Files.isSymbolicLink(it.toPath()) } // Get non-symbolic paths first
        .flatMap { root -> root.walk().filter { it.isFile && it.extension == "kt" }.toList() }
        .sortedBy { Files.isSymbolicLink(it.toPath()) } // This time is for .java files
        .distinctBy { it.canonicalPath }
    compilerConfiguration.addKotlinSourceRoots(files.map { it.absolutePath })

    val project = env.project as MockProject
    val ktFiles = convertFilesToKtFiles(project, files)
    configureProjectEnvironment(
        project,
        compilerConfiguration,
        ktFiles,
        env::createPackagePartProvider,
        env.projectEnvironment.environment.jarFileSystem as CoreJarFileSystem
    )
    val kspCoreEnvironment = KSPCoreEnvironment(project)

    for (ktFile in ktFiles) {
        analyseWithReadAction(ktFile) {
            val fileSymbol = ktFile.getFileSymbol()
            val members = fileSymbol.getFileScope().getAllSymbols()
            members.filterIsInstance<KtFunctionSymbol>()
        }
    }
}
