# Java annotation processing to KSP reference

## Program elements

| **Java** | **Closest facility in KSP** | **Notes** |
| -------- | --------------------------- | --------- |
| AnnotationMirror | KSAnnotation | |
| AnnotationValue | KSValueArguments | |
| Element | KSDeclaration / KSDeclarationContainer | |
| ExecutableElement | KSFunctionDeclaration | |
| PackageElement | KSFile | KSP doesn't model packages as program elements. |
| Parameterizable | KSDeclaration | |
| QualifiedNameable | KSDeclaration | |
| TypeElement | KSClassDeclaration | |
| TypeParameterElement | KSTypeParameter | |
| VariableElement | KSValueParameter / KSPropertyDeclaration | |

## Types

Because KSP requires explicit type resolution, some functionalities in Java can
only be carried out by KSType and the corresponding elements before resolution.

| **Java** | **Closest facility in KSP** | **Notes** |
| -------- | --------------------------- | --------- |
| ArrayType | KSBuiltIns | |
| DeclaredType | KSType / KSClassifierReference | |
| ErrorType | null | |
| ExecutableType | KSType / KSCallableReference | |
| IntersectionType | KSType / KSTypeParameter | |
| NoType | KSBuiltIns / null | |
| NullType | KSBuiltIns | |
| PrimitiveType | KSBuiltIns | |
| ReferenceType | KSTypeReference | |
| TypeMirror | KSType | |
| TypeVariable | KSTypeParameter | |
| UnionType | N / A | Kotlin has only one type per catch block. UnionType is also not observable by even Java annotation processors. |
| WildcardType | KSType / KSTypeArgument | |

## Misc

| **Java** | **Closest facility in KSP** | **notes** |
| -------- | --------------------------- | --------- |
| Name | KSName | |
| ElementKind | ClassKind / FunctionKind | |
| Modifier | Modifier | |
| NestingKind | ClassKind / FunctionKind | |
| AnnotationValueVisitor |  | |
| ElementVisitor | KSVisitor | |
| AnnotatedConstruct | KSAnnotated | |
| TypeVisitor |  | |
| TypeKind | KSBuiltIns | Some can be found in builtins, otherwise check KSClassDeclaration for DeclaredType |
| ElementFilter | Collection.filterIsInstance | |
| ElementKindVisitor | KSVisitor | |
| ElementScanner | KSTopDownVisitor | |
| SimpleAnnotationValueVisitor |  | No needed in KSP |
| SimpleElementVisitor | KSVisitor | |
| SimpleTypeVisitor |  | |
| TypeKindVisitor |  | |
| Types | Resolver / utils | Some of the utils are also integrated into symbol interfaces |
| Elements | Resolver / utils | |

## Details

How functionalities of Java annotation processing API can be carried out by KSP.

### AnnotationMirror

| **Java** | **KSP equivalent** |
| -------- | ------------------ |
| getAnnotationType | ksAnnotation.annotationType |
| getElementValues | ksAnnotation.arguments |

## AnnotationValue

| **Java** | **KSP equivalent** |
| -------- | ------------------ |
| getValue | ksValueArgument.value |

## Element

| **Java** | **KSP equivalent** |
| -------- | ------------------ |
| asType | ksClassDeclaration.asType(...) // Only available for KSClassDeclaration. Type arguments need to be supplied. |
| getAnnotation | // To be implemented. |
| getAnnotationMirrors | ksDeclaration.annotations |
| getEnclosedElements | ksDeclarationContainer.declarations |
| getEnclosingElements | ksDeclaration.parentDeclaration |
| getKind | `is` operator + ClassKind or FunctionKind |
| getModifiers | ksDeclaration.modifiers |
| getSimpleName | ksDeclaration.simpleName |

## ExecutableElement

| **Java** | **KSP equivalent** |
| -------- | ------------------ |
| getDefaultValue | // To be implemented. |
| getParameters | ksFunctionDeclaration.parameters |
| getReceiverType | ksFunctionDeclaration.parentDeclaration |
| getReturnType | ksFunctionDeclaration.returnType |
| getSimpleName | ksFunctionDeclaration.simpleName |
| getThrownTypes | // Not needed in Kotlin. |
| getTypeParameters | ksFunctionDeclaration.typeParameters |
| isDefault | // Check whether parent declaration is an interface or not. |
| isVarArgs | ksFunctionDeclaration.parameters.any { it.isVarArg } |

## Parameterizable

| **Java** | **KSP equivalent** |
| -------- | ------------------ |
| getTypeParameters | ksFunctionDeclaration.typeParameters |

## QualifiedNameable

| **Java** | **KSP equivalent** |
| -------- | ------------------ |
| getQualifiedName | ksDeclaration.qualifiedName |

## TypeElement

| **Java** | **KSP equivalent** |
| -------- | ------------------ |
| getEnclosedElements | ksClassDeclaration.declarations |
| getEnclosingElement | ksClassDeclaration.parentDeclaration |
| getInterfaces | ```ksClassDeclaration.superTypes.map { it.resolve() }.filter {(it?.declaration as? KSClassDeclaration)?.classKind == ClassKind.INTERFACE} // Should be able to do without resolution.``` |
| getNestingKind | // check KSClassDeclaration.parentDeclaration and inner modifier. |
| getQualifiedName | ksClassDeclaration.qualifiedName |
| getSimpleName | ksClassDeclaration.simpleName |
| getSuperclass | ```ksClassDeclaration.superTypes.map { it.resolve() }.filter { (it?.declaration as? KSClassDeclaration)?.classKind == ClassKind.CLASS } // Should be able to do without resolution.``` |
| getTypeParameters | ksClassDeclaration.typeParameters |

## TypeParameterElement

| **Java** | **KSP equivalent** |
| -------- | ------------------ |
| getBounds | ksTypeParameter.bounds |
| getEnclosingElement | ksTypeParameter.parentDeclaration |
| getGenericElement | ksTypeParameter.parentDeclaration |

## VariableElement

| **Java** | **KSP equivalent** |
| -------- | ------------------ |
| getConstantValue | // To be implemented. |
| getEnclosingElement | ksValueParameter.parentDeclaration |
| getSimpleName | ksValueParameter.simpleName |

## ArrayType

| **Java** | **KSP equivalent** |
| -------- | ------------------ |
| getComponentType | ksType.arguments.first() |

## DeclaredType

| **Java** | **KSP equivalent** |
| -------- | ------------------ |
| asElement | ksType.declaration |
| getEnclosingType | ksType.declaration.parentDeclaration |
| getTypeArguments | ksType.arguments |

## ExecutableType

**Note:** A `KSType` for a function is just a signature represented by the
`FunctionN<R, T1, T2, ..., TN>` family.

| **Java** | **KSP equivalent** |
| -------- | ------------------ |
| getParameterTypes | ksType.declaration.typeParameters, ksFunctionDeclaration.parameters.map { it.type } |
| getReceiverType | ksFunctionDeclaration.parentDeclaration.asType(...) |
| getReturnType | ksType.declaration.typeParameters.last() |
| getThrownTypes | // Not needed in Kotlin. |
| getTypeVariables | ksFunctionDeclaration.typeParameters |

## IntersectionType

| **Java** | **KSP equivalent** |
| -------- | ------------------ |
| getBounds | ksTypeParameter.bounds |

## TypeMirror

| **Java** | **KSP equivalent** |
| -------- | ------------------ |
| getKind | // Compare with types in KSBuiltIns. |

## TypeVariable

| **Java** | **KSP equivalent** |
| -------- | ------------------ |
| asElement | ksType.declaration |
| getLowerBound | // To be decided. Only needed if capture is provided and explicit bound checking is needed. |
| getUpperBound | ksTypeParameter.bounds |

## WildcardType

| **Java** | **KSP equivalent** |
| -------- | ------------------ |
| getExtendsBound | ```if (ksTypeArgument.variance == Variance.COVARIANT) { ksTypeArgument.type } else { null }``` |
| getSuperBound | ```if (ksTypeArgument.variance == Variance.CONTRAVARIANT) { ksTypeArgument.type } else { null }``` |

## Elements

| **Java** | **KSP equivalent** |
| -------- | ------------------ |
| getAllAnnotationMirrors | KSDeclarations.annotations |
| getAllMembers | getAllFunctions and getAllProperties, the latter is not there yet |
| getBinaryName | // To be decided, see [Java Spec](https://docs.oracle.com/javase/specs/jls/se13/html/jls-13.html#jls-13.1) |
| getConstantExpression | we have constant value, not expression |
| getDocComment | // To be implemented |
| getElementValuesWithDefaults | // To be implemented. |
| getName | resolver.getKSNameFromString |
| getPackageElement | Package not supported, while package information can be retrieved, operation on package is not possible for KSP |
| getPackageOf | Package not supported |
| getTypeElement | Resolver.getClassDeclarationByName |
| hides | // To be implemented |
| isDeprecated | KsDeclaration.annotations.any { it.annotationType.resolve()!!.declaration.quailifiedName!!.asString() == Deprecated::class.quailifiedName } |
| overrides | KSFunctionDeclaration.overrides // extension function defined in util.kt. |
| printElements | // Users can implement them freely. |

## Types

| **Java** | **KSP equivalent** |
| -------- | ------------------ |
| asElement | ksType.declaration |
| asMemberOf | resolver.asMemberOf |
| boxedClass | // Not needed |
| capture | // To be decided. |
| contains | KSType.isAssignableFrom |
| directSuperTypes | (ksType.declaration as KSClassDeclaration).superTypes |
| erasure | ksType.starProjection() |
| getArrayType | ksBuiltIns.arrayType.replace(...) |
| getDeclaredType | ksClassDeclaration.asType |
| getNoType | ksBuiltIns.nothingType / null |
| getNullType | // depends on the context, KSType.markNullable maybe useful. |
| getPrimitiveType | // Not needed |
| getWildcardType | // Use Variance in places expecting KSTypeArgument |
| isAssignable | ksType.isAssignableFrom |
| isSameType | ksType.equals |
| isSubsignature | functionTypeA == functionTypeB || functionTypeA == functionTypeB.starProjection() |
| isSubtype | ksType.isAssignableFrom |
| unboxedType | // Not needed. |
