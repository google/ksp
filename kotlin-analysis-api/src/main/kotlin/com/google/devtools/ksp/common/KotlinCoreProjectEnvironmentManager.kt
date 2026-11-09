/*
 * Copyright 2026 Google LLC
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devtools.ksp.common

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.impl.ZipHandler
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.StandaloneProjectFactory
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironmentMode
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreProjectEnvironment

internal object KotlinCoreProjectEnvironmentManager {

    /**
     * Executes [action] with a [KotlinCoreProjectEnvironment] instance.
     *
     * [withKotlinCoreProjectEnvironment] manages resources related to [CoreJarFileSystem] which is available
     * on the [KotlinCoreProjectEnvironment]. For example, during concurrent executions of [com.google.devtools.ksp.impl.KotlinSymbolProcessing]
     * the instance that finishes first drops the roots in the [CoreJarFileSystem] thereby invalidating the results
     * for other instances. See https://github.com/google/ksp/issues/3286.
     */
    internal fun <T> withKotlinCoreProjectEnvironment(action: (KotlinCoreProjectEnvironment) -> T): T {
        val projectDisposable = Disposer.newDisposable("StandaloneAnalysisAPISession.project")
        val kotlinCoreProjectEnvironment = createKotlinCoreProjectEnvironment(projectDisposable)

        return withExecutionLease(
            startAction = {
                trackJarFileSystem(
                    kotlinCoreProjectEnvironment,
                    projectDisposable
                )
            },
            action = { runActionWithCleanup(action, kotlinCoreProjectEnvironment, projectDisposable) },
            finalAction = { cleanUpJarFileSystems() }
        )
    }

    /**
     * Manages resources for concurrent executions, ensuring that only the last concurrent execution of
     * [com.google.devtools.ksp.impl.KotlinSymbolProcessing] releases resources.
     */
    private fun <T> withExecutionLease(
        startAction: () -> Unit,
        action: () -> T,
        finalAction: () -> Unit
    ): T {
        synchronized(activeExecutionsLock) {
            activeExecutions++
            try {
                startAction()
            } catch (e: Throwable) {
                // Decrement if an exception occurred and rethrow to abort
                activeExecutions--
                throw e
            }
        }
        // Store the exception from `action` to let it pop through to the caller.
        var exception: Throwable? = null
        try {
            return action()
        } catch (e: Throwable) {
            exception = e
            throw e
        } finally {
            synchronized(activeExecutionsLock) {
                if (--activeExecutions == 0) {
                    try {
                        finalAction()
                    } catch (e: Throwable) {
                        if (exception != null) {
                            // If `action` threw and `finalAction` also threw,
                            // we suppress the exception from `finalAction`.
                            exception.addSuppressed(e)
                        } else {
                            throw e
                        }
                    }
                }
            }
        }
    }

    private fun cleanUpJarFileSystems() {
        var exception: Throwable? = null
        try {
            jarFileSystemsToClear.forEach { jarFileSystem ->
                try {
                    jarFileSystem.clearHandlersCache()
                } catch (e: Throwable) {
                    // Catch exceptions to clear all elements in `jarFileSystemsToClear`, i.e., we want
                    // to avoid stopping iterating after the first exception.
                    if (exception == null) {
                        exception = e
                    } else {
                        exception.addSuppressed(e)
                    }
                }
            }

            // If there was an exception, throw it.
            if (exception != null) {
                throw exception
            }
        } finally {
            jarFileSystemsToClear.clear()
            try {
                // CoreJarFileSystem.myHandlers uses ConcurrentFactoryMap, which can construct
                // duplicate CoreJarHandler instances on concurrent lookups of an uncached JAR
                // and discard the loser via putIfAbsent. Because each CoreJarHandler opens a
                // ZipFile in ZipHandler's static cache (keyed by handler identity),
                // clearHandlersCache() only closes the winning handler. Also clear
                // ZipHandler's cache to close any orphaned ZipFile handles when the
                // application environment stays alive across runs.
                ZipHandler.clearFileAccessorCache()
            } catch (e: Throwable) {
                // N.B: We suppress the exception `e` from `ZipHandler.clearFileAccessorCache` if
                // previously `jarFileSystemsToClear.forEach(CoreJarFileSystem::clearHandlersCache)` threw.
                // This is done to prevent masking the real exception.
                if (exception == null) {
                    throw e
                } else {
                    exception.addSuppressed(e)
                }
            }
        }
    }

    private fun <T> runActionWithCleanup(
        action: (KotlinCoreProjectEnvironment) -> T,
        kotlinCoreProjectEnvironment: KotlinCoreProjectEnvironment,
        projectDisposable: Disposable
    ): T {
        var exception: Throwable? = null
        try {
            return action(kotlinCoreProjectEnvironment)
        } catch (e: Throwable) {
            exception = e
            throw e
        } finally {
            if (exception == null) {
                dispose(projectDisposable)
            } else {
                disposeSuppressed(exception, projectDisposable)
            }
        }
    }

    private fun trackJarFileSystem(
        kotlinCoreProjectEnvironment: KotlinCoreProjectEnvironment,
        projectDisposable: Disposable
    ) = runActionAndDisposeInCaseOfError(projectDisposable) {
        (kotlinCoreProjectEnvironment.environment.jarFileSystem as? CoreJarFileSystem)?.let {
            jarFileSystemsToClear.add(it)
        }
    }

    private fun createKotlinCoreProjectEnvironment(projectDisposable: Disposable): KotlinCoreProjectEnvironment =
        runActionAndDisposeInCaseOfError(projectDisposable) {
            StandaloneProjectFactory.createProjectEnvironment(
                projectDisposable,
                KotlinCoreApplicationEnvironmentMode.Production
            )
        }

    private inline fun <T> runActionAndDisposeInCaseOfError(projectDisposable: Disposable, action: () -> T): T = try {
        action()
    } catch (e: Throwable) {
        // Dispose projectDisposable in case of exception to prevent resource leak
        disposeSuppressed(e, projectDisposable)
        throw e
    }

    private fun disposeSuppressed(e: Throwable, projectDisposable: Disposable) {
        suppressBecauseOf(e) {
            dispose(projectDisposable)
        }
    }

    private fun dispose(projectDisposable: Disposable) {
        IntelliJManager.maybeRunInWriteAction {
            Disposer.dispose(projectDisposable)
        }
    }

    private inline fun suppressBecauseOf(e: Throwable, actionToSuppress: () -> Unit): Unit =
        try {
            actionToSuppress()
        } catch (suppressedThrowable: Throwable) {
            e.addSuppressed(suppressedThrowable)
        }

    private val activeExecutionsLock = Any()
    private var activeExecutions = 0

    private val jarFileSystemsToClear = mutableSetOf<CoreJarFileSystem>()
}
