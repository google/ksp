package com.google.devtools.ksp.impl

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.services.FirSealedClassInheritorsProcessorFactory
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.SealedClassInheritorsProvider
import org.jetbrains.kotlin.name.ClassId

class FirSealedClassInheritorsProcessorFactoryImpl: FirSealedClassInheritorsProcessorFactory() {
    override fun createSealedClassInheritorsProvider(): SealedClassInheritorsProvider {
        return SealedClassInheritorProviderImpl()
    }
}

class SealedClassInheritorProviderImpl: SealedClassInheritorsProvider() {
    override fun getSealedClassInheritors(firClass: FirRegularClass): List<ClassId> {
        return emptyList()
    }

}
