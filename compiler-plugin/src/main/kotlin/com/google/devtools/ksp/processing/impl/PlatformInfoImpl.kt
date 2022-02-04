package com.google.devtools.ksp.processing.impl

import com.google.devtools.ksp.processing.JsPlatformInfo
import com.google.devtools.ksp.processing.JvmPlatformInfo
import com.google.devtools.ksp.processing.NativePlatformInfo
import com.google.devtools.ksp.processing.UnknownPlatformInfo

internal class JvmPlatformInfoImpl(
    override val platformName: String,
    override val jvmTarget: String
) : JvmPlatformInfo {
    override fun toString() = "$platformName ($jvmTarget)"
}

internal class JsPlatformInfoImpl(
    override val platformName: String
) : JsPlatformInfo {
    override fun toString() = platformName
}

internal class NativePlatformInfoImpl(
    override val platformName: String,
    override val targetName: String
) : NativePlatformInfo {
    override fun toString() = "$platformName ($targetName)"
}

internal class UnknownPlatformInfoImpl(
    override val platformName: String
) : UnknownPlatformInfo {
    override fun toString() = platformName
}
