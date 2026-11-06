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

// TEST PROCESSOR: DeferredBackingFieldsProcessor
// PROCESSOR INPUT: DeferFieldOnly, DeferPropFirst, DeferFieldFirst, DeferBothFromClassDecl, SynthFieldOnly, SynthPropFirst, SynthFieldFirst
// EXPECTED:
// EXPECT ENABLE_BACKING_FIELDS = FALSE: DeferBothFromClassDecl: K.fromClassDecl no matching field. Backing field was: backingField=null
// EXPECT ENABLE_BACKING_FIELDS = FALSE: DeferBothFromClassDecl: KtClass.fromClassDecl no matching field. Backing field was: backingField=null
// EXPECT ENABLE_BACKING_FIELDS = FALSE: DeferBothFromClassDecl: props=2, fields=0, total=2
// EXPECT ENABLE_BACKING_FIELDS = FALSE: DeferFieldFirst: K.fieldFirst no matching field. Backing field was: backingField=null
// EXPECT ENABLE_BACKING_FIELDS = FALSE: DeferFieldFirst: K.unresolvedType2 no matching field. Backing field was: backingField=null
// EXPECT ENABLE_BACKING_FIELDS = FALSE: DeferFieldFirst: KtClass.explicitFieldFirst no matching field. Backing field was: backingField=null
// EXPECT ENABLE_BACKING_FIELDS = FALSE: DeferFieldFirst: KtClass.fieldFirst no matching field. Backing field was: backingField=null
// EXPECT ENABLE_BACKING_FIELDS = FALSE: DeferFieldFirst: KtClass.unresolvedType2 no matching field. Backing field was: backingField=null
// EXPECT ENABLE_BACKING_FIELDS = FALSE: DeferFieldFirst: props=5, fields=0, total=5
// EXPECT ENABLE_BACKING_FIELDS = FALSE: DeferPropFirst: K.propFirst no matching field. Backing field was: backingField=null
// EXPECT ENABLE_BACKING_FIELDS = FALSE: DeferPropFirst: K.unresolvedType no matching field. Backing field was: backingField=null
// EXPECT ENABLE_BACKING_FIELDS = FALSE: DeferPropFirst: KtClass.explicitPropFirst no matching field. Backing field was: backingField=null
// EXPECT ENABLE_BACKING_FIELDS = FALSE: DeferPropFirst: KtClass.propFirst no matching field. Backing field was: backingField=null
// EXPECT ENABLE_BACKING_FIELDS = FALSE: DeferPropFirst: KtClass.unresolvedType no matching field. Backing field was: backingField=null
// EXPECT ENABLE_BACKING_FIELDS = FALSE: DeferPropFirst: props=5, fields=0, total=5
// EXPECT ENABLE_BACKING_FIELDS = FALSE: SynthFieldFirst: SynthAnno.fieldFirst no matching field. Backing field was: backingField=null
// EXPECT ENABLE_BACKING_FIELDS = FALSE: SynthFieldFirst: SynthAnno.unresolvedType2 no matching field. Backing field was: backingField=null
// EXPECT ENABLE_BACKING_FIELDS = FALSE: SynthFieldFirst: props=2, fields=0, total=2
// EXPECT ENABLE_BACKING_FIELDS = FALSE: SynthPropFirst: SynthAnno.propFirst no matching field. Backing field was: backingField=null
// EXPECT ENABLE_BACKING_FIELDS = FALSE: SynthPropFirst: SynthAnno.unresolvedType no matching field. Backing field was: backingField=null
// EXPECT ENABLE_BACKING_FIELDS = FALSE: SynthPropFirst: props=2, fields=0, total=2
// EXPECT ENABLE_BACKING_FIELDS = TRUE: DeferBothFromClassDecl: K.fromClassDecl <-> K.fromClassDecl.field backingFieldMatches=true propertyMatches=true parentMatches=true
// EXPECT ENABLE_BACKING_FIELDS = TRUE: DeferBothFromClassDecl: KtClass.fromClassDecl <-> KtClass.fromClassDecl.field backingFieldMatches=true propertyMatches=true parentMatches=true
// EXPECT ENABLE_BACKING_FIELDS = TRUE: DeferBothFromClassDecl: props=2, fields=2, total=4
// EXPECT ENABLE_BACKING_FIELDS = TRUE: DeferFieldFirst: K.fieldFirst <-> K.fieldFirst.field backingFieldMatches=true propertyMatches=true parentMatches=true
// EXPECT ENABLE_BACKING_FIELDS = TRUE: DeferFieldFirst: K.unresolvedType2 <-> K.unresolvedType2.field backingFieldMatches=true propertyMatches=true parentMatches=true
// EXPECT ENABLE_BACKING_FIELDS = TRUE: DeferFieldFirst: KtClass.explicitFieldFirst <-> KtClass.explicitFieldFirst.field backingFieldMatches=true propertyMatches=true parentMatches=true
// EXPECT ENABLE_BACKING_FIELDS = TRUE: DeferFieldFirst: KtClass.fieldFirst <-> KtClass.fieldFirst.field backingFieldMatches=true propertyMatches=true parentMatches=true
// EXPECT ENABLE_BACKING_FIELDS = TRUE: DeferFieldFirst: KtClass.unresolvedType2 <-> KtClass.unresolvedType2.field backingFieldMatches=true propertyMatches=true parentMatches=true
// EXPECT ENABLE_BACKING_FIELDS = TRUE: DeferFieldFirst: props=5, fields=5, total=10
// EXPECT ENABLE_BACKING_FIELDS = TRUE: DeferFieldOnly: K.onlyField1.field isBackingField=true roundTrip=true
// EXPECT ENABLE_BACKING_FIELDS = TRUE: DeferFieldOnly: K.onlyField2.field isBackingField=true roundTrip=true
// EXPECT ENABLE_BACKING_FIELDS = TRUE: DeferFieldOnly: KtClass.ctorOnly.field isBackingField=true roundTrip=true
// EXPECT ENABLE_BACKING_FIELDS = TRUE: DeferFieldOnly: KtClass.explicitOnly.field isBackingField=true roundTrip=true
// EXPECT ENABLE_BACKING_FIELDS = TRUE: DeferFieldOnly: KtClass.implicitOnly.field isBackingField=true roundTrip=true
// EXPECT ENABLE_BACKING_FIELDS = TRUE: DeferFieldOnly: KtClass.unresolvedOnly.field isBackingField=true roundTrip=true
// EXPECT ENABLE_BACKING_FIELDS = TRUE: DeferPropFirst: K.propFirst <-> K.propFirst.field backingFieldMatches=true propertyMatches=true parentMatches=true
// EXPECT ENABLE_BACKING_FIELDS = TRUE: DeferPropFirst: K.unresolvedType <-> K.unresolvedType.field backingFieldMatches=true propertyMatches=true parentMatches=true
// EXPECT ENABLE_BACKING_FIELDS = TRUE: DeferPropFirst: KtClass.explicitPropFirst <-> KtClass.explicitPropFirst.field backingFieldMatches=true propertyMatches=true parentMatches=true
// EXPECT ENABLE_BACKING_FIELDS = TRUE: DeferPropFirst: KtClass.propFirst <-> KtClass.propFirst.field backingFieldMatches=true propertyMatches=true parentMatches=true
// EXPECT ENABLE_BACKING_FIELDS = TRUE: DeferPropFirst: KtClass.unresolvedType <-> KtClass.unresolvedType.field backingFieldMatches=true propertyMatches=true parentMatches=true
// EXPECT ENABLE_BACKING_FIELDS = TRUE: DeferPropFirst: props=5, fields=5, total=10
// EXPECT ENABLE_BACKING_FIELDS = TRUE: SynthFieldFirst: SynthAnno.fieldFirst <-> SynthAnno.fieldFirst.field backingFieldMatches=true propertyMatches=true parentMatches=true
// EXPECT ENABLE_BACKING_FIELDS = TRUE: SynthFieldFirst: SynthAnno.unresolvedType2 <-> SynthAnno.unresolvedType2.field backingFieldMatches=true propertyMatches=true parentMatches=true
// EXPECT ENABLE_BACKING_FIELDS = TRUE: SynthFieldFirst: props=2, fields=2, total=4
// EXPECT ENABLE_BACKING_FIELDS = TRUE: SynthFieldOnly: SynthAnno.onlyField1.field isBackingField=true roundTrip=true
// EXPECT ENABLE_BACKING_FIELDS = TRUE: SynthFieldOnly: SynthAnno.onlyField2.field isBackingField=true roundTrip=true
// EXPECT ENABLE_BACKING_FIELDS = TRUE: SynthPropFirst: SynthAnno.propFirst <-> SynthAnno.propFirst.field backingFieldMatches=true propertyMatches=true parentMatches=true
// EXPECT ENABLE_BACKING_FIELDS = TRUE: SynthPropFirst: SynthAnno.unresolvedType <-> SynthAnno.unresolvedType.field backingFieldMatches=true propertyMatches=true parentMatches=true
// EXPECT ENABLE_BACKING_FIELDS = TRUE: SynthPropFirst: props=2, fields=2, total=4
// END

// FILE: Annotations.kt
annotation class DeferFieldOnly
annotation class DeferPropFirst
annotation class DeferFieldFirst
annotation class DeferBothFromClassDecl

annotation class SynthFieldOnly
annotation class SynthPropFirst
annotation class SynthFieldFirst

// FILE: Kt.kt
class KtClass(
    @field:DeferFieldOnly val ctorOnly: Int,
) {
    @field:DeferFieldOnly var implicitOnly: Int = 0

    val explicitOnly: Any
        @DeferFieldOnly field = ""

    @field:DeferFieldOnly lateinit var unresolvedOnly: UnresolvedKtType

    @property:DeferPropFirst
    @field:DeferPropFirst
    var propFirst: Int = 0

    @DeferPropFirst
    val explicitPropFirst: Any
        @DeferPropFirst field = ""

    @property:DeferPropFirst
    @field:DeferPropFirst
    lateinit var unresolvedType: UnresolvedKtType

    @property:DeferFieldFirst
    @field:DeferFieldFirst
    var fieldFirst: Int = 0

    @DeferFieldFirst
    val explicitFieldFirst: Any
        @DeferFieldFirst field = ""

    @property:DeferFieldFirst
    @field:DeferFieldFirst
    lateinit var unresolvedType2: UnresolvedKtType

    @property:DeferBothFromClassDecl
    @field:DeferBothFromClassDecl
    var fromClassDecl: Int = 0
}

// FILE: J.java
public @interface SynthAnno {
    @SynthFieldOnly int onlyField1() default 0;
    @SynthFieldOnly List<Double> onlyField2();

    @SynthPropFirst int propFirst() default 0;
    @SynthPropFirst List<Double> unresolvedType();

    @SynthFieldFirst int fieldFirst() default 0;
    @SynthFieldFirst List<Double> unresolvedType2();
}

class K {
    @DeferFieldOnly int onlyField1 = 0;

    @DeferFieldOnly
    List<Double> onlyField2 = List<double>();

    @DeferPropFirst int propFirst = 0;
    @DeferFieldFirst int fieldFirst = 0;

    @DeferBothFromClassDecl int fromClassDecl = 0;

    @DeferPropFirst
    List<Double> unresolvedType = List<double>()

    @DeferFieldFirst
    List<Double> unresolvedType2 = List<double>()
}
