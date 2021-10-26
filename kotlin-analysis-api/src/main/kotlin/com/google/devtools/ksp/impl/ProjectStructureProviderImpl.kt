package com.google.devtools.ksp.impl

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JdkPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices

class ProjectStructureProviderImpl : ProjectStructureProvider() {
    override fun getKtModuleForKtElement(element: PsiElement): KtModule {
        return object: KtSourceModule {
            override val analyzerServices: PlatformDependentAnalyzerServices
                get() = JvmPlatformAnalyzerServices
            override val contentScope: GlobalSearchScope
                get() = GlobalSearchScope.EMPTY_SCOPE
            override val directFriendDependencies: List<KtModule>
                get() = emptyList()
            override val directRefinementDependencies: List<KtModule>
                get() = emptyList()
            override val directRegularDependencies: List<KtModule>
                get() = emptyList()
            override val languageVersionSettings: LanguageVersionSettings
                get() = LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST)
            override val moduleName: String
                get() = "main"
            override val platform: TargetPlatform
                get() = TargetPlatform(setOf(JdkPlatform(JvmTarget.DEFAULT)))
            override val project: Project
                get() = element.project

        }
    }
}
