/*
 * Copyright 2022 Google LLC
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
package com.google.devtools.ksp

import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import kotlin.system.measureNanoTime

fun main(args: Array<String>) {
    val runs = 3
    val warmup = 3
    val benchName = args[0]
    val compilerArgs = args.drop(1).toTypedArray()
    val cold = measureNanoTime{
        K2JVMCompiler.main(compilerArgs)
    } / 1000000L
    for (i in 1..warmup) {
        K2JVMCompiler.main(compilerArgs)
    }
    val hot = (1..runs).map { measureNanoTime{K2JVMCompiler.main(compilerArgs)} / 1000000L }
    println("$benchName.Cold(RunTimeRaw): $cold ms")
    println("$benchName.Hot(RunTimeRaw): ${hot.minOrNull()!!} ms")
}
