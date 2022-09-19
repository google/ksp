package com.google.devtools.ksp.impl.test

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KtDeclarationSymbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiSingleFileTest
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.junit.jupiter.api.Test

class DummyAnalysisApiBasedTest: AbstractAnalysisApiSingleFileTest() {
    override val configurator: AnalysisApiTestConfigurator = KspAnalysisApiTestConfigurator

    override fun doTestByFileStructure(ktFile: KtFile, module: TestModule, testServices: TestServices) {
        val actual = analyze(ktFile) {
            ktFile.declarations
                .map { (it.getSymbol() as KtDeclarationSymbol).render() }
                .joinToString(separator = "\n")
        }
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }


    @TestMetadata("annotationValue_kt.kt")
    @Test
    fun testAnnotationValue_kt() {
        runTest("../test-utils/testData/api/annotationValue_kt.kt")
    }
}
