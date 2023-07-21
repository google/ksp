package com.google.devtools.ksp.impl

import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.lifetime.KtAlwaysAccessibleLifetimeTokenFactory
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeTokenFactory
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeTokenProvider

@OptIn(KtAnalysisApiInternals::class)
public class KtAlwaysAccessibleLifeTimeTokenProvider : KtLifetimeTokenProvider() {
    override fun getLifetimeTokenFactory(): KtLifetimeTokenFactory {
        return KtAlwaysAccessibleLifetimeTokenFactory
    }
}
