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

import kotlin.reflect.KClass

annotation class ExampleDefaultReference(
    val value: KClass<*> = UpstreamChanges::class
)

annotation class ExampleArrayDefaultReference(
    val value: Array<KClass<*>> = [UpstreamChanges::class]
)

annotation class ExampleNestedExplicitDefaultReference(
    val value: ExampleReference = ExampleReference(UpstreamChanges::class)
)

annotation class ExampleNestedDefaultReference(
    val value: ExampleDefaultReference = ExampleDefaultReference()
)

annotation class ExampleArrayNestedExplicitDefaultReference(
    val value: Array<ExampleReference> = [ExampleReference(UpstreamChanges::class)]
)

annotation class ExampleArrayNestedDefaultReference(
    val value: Array<ExampleDefaultReference> = [ExampleDefaultReference()]
)

annotation class ExampleArrayNestedArrayExplicitDefaultReference(
    val value: Array<ExampleArrayDefaultReference> = [ExampleArrayDefaultReference([UpstreamChanges::class])]
)

annotation class ExampleArrayNestedArrayDefaultReference(
    val value: Array<ExampleArrayDefaultReference> = [ExampleArrayDefaultReference()]
)
