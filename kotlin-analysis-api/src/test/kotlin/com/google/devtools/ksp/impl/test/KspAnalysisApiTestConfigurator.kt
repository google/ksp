package com.google.devtools.ksp.impl.test

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.KtModuleProjectStructure
import org.jetbrains.kotlin.analysis.api.standalone.fir.test.StandaloneModeConfigurator
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.FrontendKind
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.services.EnvironmentBasedStandardLibrariesPathProvider
import org.jetbrains.kotlin.test.services.KotlinStandardLibrariesPathProvider
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices

object KspAnalysisApiTestConfigurator : AnalysisApiTestConfigurator() {
    override val analyseInDependentSession: Boolean get() = false
    override val frontendKind: FrontendKind get() = FrontendKind.Fir

    override val serviceRegistrars: List<AnalysisApiTestServiceRegistrar> = buildList {
        addAll(StandaloneModeConfigurator.serviceRegistrars)
    }

    override fun configureTest(builder: TestConfigurationBuilder, disposable: Disposable) {
        StandaloneModeConfigurator.configureTest(builder, disposable)

        with(builder) {
            useAdditionalService<KotlinStandardLibrariesPathProvider> { EnvironmentBasedStandardLibrariesPathProvider }
        }
    }

    override fun createModules(
        moduleStructure: TestModuleStructure,
        testServices: TestServices,
        project: Project
    ): KtModuleProjectStructure {
        return StandaloneModeConfigurator.createModules(moduleStructure, testServices, project)
    }

    override fun doOutOfBlockModification(file: KtFile) {
        StandaloneModeConfigurator.doOutOfBlockModification(file)
    }
}
