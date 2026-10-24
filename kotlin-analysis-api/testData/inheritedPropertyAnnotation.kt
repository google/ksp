/*
 * Copyright 2026 Google LLC
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

// TEST PROCESSOR: InheritedPropertyAnnotationProcessor
// EXPECTED:
// EXPECT CURRENT: Control.name: name
// EXPECT CURRENT: WithInterface.name: name
// EXPECT NEXT: Control.name: null
// EXPECT NEXT: WithInterface.name: null
// END

// FILE: Column.java
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {
    String name();
}

// FILE: main.kt
interface HasName {
    val name: String
}

abstract class BaseEntity {
    @Column(name = "name") lateinit var name: String
}

class Control : BaseEntity()
class WithInterface : BaseEntity(), HasName
