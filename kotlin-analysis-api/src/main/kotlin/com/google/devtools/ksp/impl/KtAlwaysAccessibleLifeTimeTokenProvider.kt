package com.google.devtools.ksp.impl

import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeTokenFactory
import org.jetbrains.kotlin.analysis.api.platform.lifetime.KotlinAlwaysAccessibleLifetimeTokenFactory
import org.jetbrains.kotlin.analysis.api.platform.lifetime.KotlinLifetimeTokenProvider

public class KtAlwaysAccessibleLifeTimeTokenProvider : KotlinLifetimeTokenProvider() {
    override fun getLifetimeTokenFactory(): KtLifetimeTokenFactory {
        return KotlinAlwaysAccessibleLifetimeTokenFactory
    }
}
