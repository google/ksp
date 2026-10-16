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

// TEST PROCESSOR: OverridePropertyAnnotationArgumentProcessor
// EXPECTED:
// Plain.syncMapStyle: name=map_style, marker=false
// OverrideOnly.syncMapStyle: name=map_style, marker=false
// MarkerOnly.syncMapStyle: name=map_style, marker=true
// OverrideWithMarker.syncMapStyle: name=map_style, marker=true
// END

// MODULE: annotations
// FILE: ColumnInfo.java
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.CLASS)
public @interface ColumnInfo {
    String name() default "[field-name]";
}

// MODULE: main(annotations)
// FILE: main.kt
interface HasMapStyle {
    var syncMapStyle: Int?
}

@Retention(AnnotationRetention.SOURCE)
annotation class MyMarker

class Plain(
    @ColumnInfo(name = "map_style")
    var syncMapStyle: Int? = null,
)

class OverrideOnly(
    @ColumnInfo(name = "map_style")
    override var syncMapStyle: Int? = null,
) : HasMapStyle

class MarkerOnly(
    @ColumnInfo(name = "map_style")
    @property:MyMarker
    var syncMapStyle: Int? = null,
)

class OverrideWithMarker(
    @ColumnInfo(name = "map_style")
    @property:MyMarker
    override var syncMapStyle: Int? = null,
) : HasMapStyle
