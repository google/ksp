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

// WITH_RUNTIME
// TEST PROCESSOR: AnnotationTypeArgumentsProcessor
// EXPECTED:
// ContainerTarget.nested.annotationType: MapConvert<SourceObject, DestinationObject, DestinationConverter>
// ContainerTarget.nested.typeArgCount: 3
// ContainerTarget.nested.typeArgs: SourceObject, DestinationObject, DestinationConverter
// NestedTarget.annotationType: MapConvert<INVARIANT List<INVARIANT SourceObject>, INVARIANT Map<INVARIANT String, INVARIANT DestinationObject>, INVARIANT NestedDestinationConverter>
// NestedTarget.typeArgCount: 3
// NestedTarget.typeArgs: kotlin.collections.List, kotlin.collections.Map, NestedDestinationConverter
// Target.annotationType: MapConvert<INVARIANT SourceObject, INVARIANT DestinationObject, INVARIANT DestinationConverter>
// Target.typeArgCount: 3
// Target.typeArgs: SourceObject, DestinationObject, DestinationConverter
// END

interface Converter<S : Any, D : Any>

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class MapConvert<S : Any, D : Any, C : Converter<S, D>>

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Container(
    val nested: MapConvert<out Any, out Any, out Converter<out Any, out Any>>,
)

class SourceObject
class DestinationObject
class DestinationConverter : Converter<SourceObject, DestinationObject>
class NestedDestinationConverter : Converter<List<SourceObject>, Map<String, DestinationObject>>

@MapConvert<SourceObject, DestinationObject, DestinationConverter>
class Target

@MapConvert<List<SourceObject>, Map<String, DestinationObject>, NestedDestinationConverter>
class NestedTarget

@Container(
    nested = MapConvert<SourceObject, DestinationObject, DestinationConverter>()
)
class ContainerTarget
