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

package com.google.devtools.ksp.processor

import com.google.devtools.ksp.ApiFeatures
import com.google.devtools.ksp.impl.symbol.kotlin.Deferrable
import com.google.devtools.ksp.impl.symbol.kotlin.Restorable
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSBackingField
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Origin

class DeferredBackingFieldsProcessor(
    val annotationNames: List<String>,
    override val apiFeatures: ApiFeatures,
) : AbstractTestProcessor() {
    init {
        require(annotationNames.size == 7) {
            "Expected 7 annotation names " +
                "[fieldOnly, propFirst, fieldFirst, bothFromClassDecl, " +
                "synthFieldOnly, synthPropFirst, synthFieldFirst], got: $annotationNames"
        }
    }

    private val deferFieldOnlyAnnotation: String = annotationNames[0]
    private val deferPropFirstAnnotation: String = annotationNames[1]
    private val deferFieldFirstAnnotation: String = annotationNames[2]
    private val deferBothFromClassDeclAnnotation: String = annotationNames[3]
    private val synthFieldOnlyAnnotation: String = annotationNames[4]
    private val synthPropFirstAnnotation: String = annotationNames[5]
    private val synthFieldFirstAnnotation: String = annotationNames[6]

    private val syntheticRestorablesByScenario = mutableMapOf<String, List<Restorable>>()
    private val result = mutableListOf<String>()
    private var round = 0
    private lateinit var env: SymbolProcessorEnvironment

    override fun toResult(): List<String> = result.sorted()

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        env = environment
        return super.create(environment)
    }

    override fun process(resolver: Resolver): List<KSAnnotated> = when (round++) {
        0 -> {
            generateFileToTriggerNextRound()
            buildList {
                // Java (KSBackingFieldJavaImpl) & Kotlin (KSBackingFieldImpl) backing fields
                addAll(deferBackingFieldsOnly(resolver, deferFieldOnlyAnnotation))
                addAll(deferPropertyThenBackingField(resolver, deferPropFirstAnnotation))
                addAll(deferBackingFieldThenProperty(resolver, deferFieldFirstAnnotation))
                addAll(deferPropertyAndBackingFieldFromClassDeclarations(resolver, deferBothFromClassDeclAnnotation))

                // Synthetic Java backing fields (KSSyntheticJavaBackingFieldImpl):
                // Returned in process() to ensure KSP's Origin.SYNTHETIC filter causes no errors,
                // and explicitly deferred via Deferrable.defer() so Restorable.restore() is exercised across rounds.
                addAll(deferSyntheticBackingFieldsOnly(resolver, synthFieldOnlyAnnotation))
                addAll(deferSyntheticPropertyThenBackingField(resolver, synthPropFirstAnnotation))
                addAll(deferSyntheticBackingFieldThenProperty(resolver, synthFieldFirstAnnotation))
            }
        }
        1 -> {
            verifyRestoredBackingFieldsOnly(resolver, deferFieldOnlyAnnotation)
            verifyRestoredPropertyAndBackingFieldPairs(resolver, deferPropFirstAnnotation)
            verifyRestoredPropertyAndBackingFieldPairs(resolver, deferFieldFirstAnnotation)
            verifyRestoredPropertyAndBackingFieldPairs(resolver, deferBothFromClassDeclAnnotation)

            verifyRestoredSyntheticBackingFieldsOnly(resolver, synthFieldOnlyAnnotation)
            verifyRestoredSyntheticPairs(resolver, synthPropFirstAnnotation)
            verifyRestoredSyntheticPairs(resolver, synthFieldFirstAnnotation)
            emptyList()
        }
        else -> emptyList()
    }

    private fun generateFileToTriggerNextRound() {
        env.codeGenerator.createNewFile(Dependencies(false), "", "Unused", "kt").use {
            it.write("class Unused".toByteArray())
        }
    }

    /**
     * Scenario 1: Defer only the [KSBackingField] symbols annotated with [annotationName].
     */
    private fun deferBackingFieldsOnly(resolver: Resolver, annotationName: String): List<KSAnnotated> =
        resolver.getSymbolsWithAnnotation(annotationName)
            .filterIsInstance<KSBackingField>()
            .toList()

    /**
     * Scenario 2a: Defer both the [KSPropertyDeclaration] and its [KSBackingField] in `[prop, field]` order.
     */
    private fun deferPropertyThenBackingField(resolver: Resolver, annotationName: String): List<KSAnnotated> =
        deferAnnotatedPropertiesAndBackingFields(resolver, annotationName, fieldFirst = false)

    /**
     * Scenario 2b: Defer both the [KSPropertyDeclaration] and its [KSBackingField] in `[field, prop]` order.
     */
    private fun deferBackingFieldThenProperty(resolver: Resolver, annotationName: String): List<KSAnnotated> =
        deferAnnotatedPropertiesAndBackingFields(resolver, annotationName, fieldFirst = true)

    private fun deferAnnotatedPropertiesAndBackingFields(
        resolver: Resolver,
        annotationName: String,
        fieldFirst: Boolean,
    ): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(annotationName).toList()
        val props = symbols.filterIsInstance<KSPropertyDeclaration>()
        val fieldsByPropFqn = symbols.filterIsInstance<KSBackingField>()
            .associateBy { it.property.qualifiedName?.asString() }

        return buildList {
            for (prop in props) {
                val propFqn = prop.qualifiedName?.asString()
                val field = fieldsByPropFqn[propFqn]
                if (apiFeatures.enableBackingFields) {
                    check(field != null) { "Expected backing field for $propFqn in round 0" }
                    check(prop.backingField == field) {
                        "Expected prop.backingField == field for $propFqn in round 0"
                    }
                    check(field.property == prop) {
                        "Expected field.property == prop for $propFqn in round 0"
                    }
                }
                if (fieldFirst) {
                    if (field != null) add(field)
                    add(prop)
                } else {
                    add(prop)
                    if (field != null) add(field)
                }
            }
        }
    }

    /**
     * Scenario 3: Defer both a [KSPropertyDeclaration] obtained via [KSClassDeclaration.declarations]
     * (exercising `KSPropertyDeclarationJavaPsiImpl` under experimental PSI resolution) and its [KSBackingField].
     */
    private fun deferPropertyAndBackingFieldFromClassDeclarations(
        resolver: Resolver,
        annotationName: String,
    ): List<KSAnnotated> {
        val props = resolver.getAllFiles()
            .flatMap { it.declarations }
            .filterIsInstance<KSClassDeclaration>()
            .flatMap { it.declarations }
            .filterIsInstance<KSPropertyDeclaration>()
            .filter { it.hasAnnotation(annotationName) }
            .toList()

        check(props.isNotEmpty()) {
            "Expected at least one property in KSClassDeclaration.declarations annotated with $annotationName"
        }

        return buildList {
            for (prop in props) {
                add(prop)
                prop.backingField?.let { add(it) }
            }
        }
    }

    /**
     * Scenario 4a: Defer only synthetic Java [KSBackingField] symbols (`KSSyntheticJavaBackingFieldImpl`).
     */
    private fun deferSyntheticBackingFieldsOnly(resolver: Resolver, annotationName: String): List<KSAnnotated> {
        val fields = findSyntheticPropertiesWithGetterAnnotation(resolver, annotationName)
            .mapNotNull { it.backingField }
        syntheticRestorablesByScenario[annotationName] = fields.map { sym ->
            checkNotNull((sym as? Deferrable)?.defer()) { "Expected Deferrable for $sym" }
        }
        return fields
    }

    /**
     * Scenario 4b: Defer synthetic Java property and its backing field in `[prop, field]` order.
     */
    private fun deferSyntheticPropertyThenBackingField(resolver: Resolver, annotationName: String): List<KSAnnotated> =
        deferSyntheticPropertiesAndBackingFields(resolver, annotationName, fieldFirst = false)

    /**
     * Scenario 4c: Defer synthetic Java property and its backing field in `[field, prop]` order.
     */
    private fun deferSyntheticBackingFieldThenProperty(resolver: Resolver, annotationName: String): List<KSAnnotated> =
        deferSyntheticPropertiesAndBackingFields(resolver, annotationName, fieldFirst = true)

    private fun deferSyntheticPropertiesAndBackingFields(
        resolver: Resolver,
        annotationName: String,
        fieldFirst: Boolean,
    ): List<KSAnnotated> {
        val props = findSyntheticPropertiesWithGetterAnnotation(resolver, annotationName)
        check(props.isNotEmpty()) {
            "Expected at least one synthetic property with getter annotated with $annotationName"
        }

        val orderedSymbols = buildList {
            for (prop in props) {
                val propFqn = prop.qualifiedName?.asString()
                val field = prop.backingField
                if (apiFeatures.enableBackingFields) {
                    check(field != null) { "Expected synthetic backing field for $propFqn in round 0" }
                    check(field.property == prop) {
                        "Expected field.property == prop for $propFqn in round 0"
                    }
                }
                if (fieldFirst) {
                    if (field != null) add(field)
                    add(prop)
                } else {
                    add(prop)
                    if (field != null) add(field)
                }
            }
        }

        syntheticRestorablesByScenario[annotationName] = orderedSymbols.map { sym ->
            checkNotNull((sym as? Deferrable)?.defer()) { "Expected Deferrable for $sym" }
        }
        return orderedSymbols
    }

    private fun findSyntheticPropertiesWithGetterAnnotation(
        resolver: Resolver,
        annotationName: String,
    ): List<KSPropertyDeclaration> =
        resolver.getAllFiles()
            .flatMap { it.declarations }
            .filterIsInstance<KSClassDeclaration>()
            .flatMap { it.declarations }
            .filterIsInstance<KSPropertyDeclaration>()
            .filter { it.origin == Origin.SYNTHETIC && it.getter?.hasAnnotation(annotationName) == true }
            .toList()

    private fun KSAnnotated.hasAnnotation(annotationName: String): Boolean =
        annotations.any { anno ->
            anno.annotationType.resolve().declaration.qualifiedName?.asString() == annotationName
        }

    private fun verifyRestoredBackingFieldsOnly(resolver: Resolver, annotationName: String) {
        verifyRestoredBackingFieldsOnly(
            label = annotationName,
            symbols = resolver.getSymbolsWithAnnotation(annotationName).toList(),
        )
    }

    private fun verifyRestoredSyntheticBackingFieldsOnly(resolver: Resolver, annotationName: String) {
        val restored = syntheticRestorablesByScenario[annotationName].orEmpty().map { restorable ->
            checkNotNull(restorable.restore()) { "Failed to restore synthetic backing field for $annotationName" }
        }
        verifyRestoredBackingFieldsOnly(label = annotationName, symbols = restored)

        val round1FieldsByFqn = findSyntheticPropertiesWithGetterAnnotation(resolver, annotationName)
            .mapNotNull { it.backingField }
            .associateBy { it.qualifiedName?.asString() }
        for (sym in restored) {
            val fqn = (sym as? KSDeclaration)?.qualifiedName?.asString()
            check(sym == round1FieldsByFqn[fqn]) {
                "Expected restored synthetic backing field $fqn to equal round 1 declaration symbol"
            }
        }
    }

    private fun verifyRestoredBackingFieldsOnly(label: String, symbols: List<KSAnnotated>) {
        symbols.forEach { sym ->
            val fqn = (sym as? KSDeclaration)?.qualifiedName?.asString()
            val isBackingField = sym is KSBackingField
            val roundTrip = (sym as? KSBackingField)?.let { it.property.backingField == it } ?: false
            result.add(
                "$label: $fqn " +
                    "isBackingField=$isBackingField roundTrip=$roundTrip"
            )
        }
    }

    private fun verifyRestoredPropertyAndBackingFieldPairs(resolver: Resolver, annotationName: String) {
        verifyRestoredPropertyAndBackingFieldPairs(
            label = annotationName,
            symbols = resolver.getSymbolsWithAnnotation(annotationName).toList(),
        )
    }

    private fun verifyRestoredSyntheticPairs(resolver: Resolver, annotationName: String) {
        val restored = syntheticRestorablesByScenario[annotationName].orEmpty().map { restorable ->
            checkNotNull(restorable.restore()) { "Failed to restore synthetic symbol for $annotationName" }
        }
        verifyRestoredPropertyAndBackingFieldPairs(label = annotationName, symbols = restored)

        val round1PropsByFqn = findSyntheticPropertiesWithGetterAnnotation(resolver, annotationName)
            .associateBy { it.qualifiedName?.asString() }
        for (prop in restored.filterIsInstance<KSPropertyDeclaration>()) {
            val propFqn = prop.qualifiedName?.asString()
            val round1Prop = checkNotNull(round1PropsByFqn[propFqn]) {
                "Expected round 1 synthetic property for $propFqn"
            }
            check(prop == round1Prop) {
                "Expected restored synthetic property $propFqn to equal round 1 declaration symbol"
            }
            check(prop.backingField == round1Prop.backingField) {
                "Expected restored synthetic backing field for $propFqn to equal round 1 declaration symbol"
            }
        }
    }

    private fun verifyRestoredPropertyAndBackingFieldPairs(label: String, symbols: List<KSAnnotated>) {
        val props = symbols.filterIsInstance<KSPropertyDeclaration>()
            .sortedBy { it.qualifiedName?.asString() }
        val fields = symbols.filterIsInstance<KSBackingField>()
            .sortedBy { it.qualifiedName?.asString() }

        result.add("$label: props=${props.size}, fields=${fields.size}, total=${symbols.size}")

        val fieldsByPropFqn = fields.associateBy { it.property.qualifiedName?.asString() }
        for (prop in props) {
            val propFqn = prop.qualifiedName?.asString()
            val matchingField = fieldsByPropFqn[propFqn]
            if (matchingField == null) {
                result.add("$label: $propFqn no matching field. Backing field was: backingField=${prop.backingField}")
            } else {
                val fieldFqn = matchingField.qualifiedName?.asString()
                val backingFieldMatches = prop.backingField == matchingField
                val propertyMatches = matchingField.property == prop
                val parentMatches = matchingField.parent == prop && matchingField.parentDeclaration == prop
                result.add(
                    "$label: $propFqn <-> $fieldFqn " +
                        "backingFieldMatches=$backingFieldMatches " +
                        "propertyMatches=$propertyMatches " +
                        "parentMatches=$parentMatches"
                )
            }
        }
    }
}
