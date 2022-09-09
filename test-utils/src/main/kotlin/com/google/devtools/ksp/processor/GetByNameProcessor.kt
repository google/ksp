package com.google.devtools.ksp.processor

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getFunctionDeclarationsByName
import com.google.devtools.ksp.getPropertyDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated

class GetByNameProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()

    override fun toResult(): List<String> {
        return results
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val classNames = listOf(
            "lib1.Foo",
            "lib1.Foo.FooNested",
            "source.FooInSource",
            "source.FooInSource.FooInSourceNested"
        )
        val funNames = listOf(
            "lib1.Foo.lib1MemberFun",
            "lib1.lib1TopFun",
            "lib1.Bar.lib1JavaMemberFun",
            "lib2.Foo.lib2MemberFun",
            "source.FooInSource.sourceMemberFun"
        )
        val overloadFunctionNames = listOf("lib1.Foo.overload")
        val propNames = listOf(
            "lib1.Foo.lib1MemberProp",
            "lib1.lib1TopProp",
            "lib2.Foo.lib2MemberProp",
            "source.FooInSource.sourceMemberProp",
            "source.propInSource",
        )
        for (className in classNames) {
            if (resolver.getClassDeclarationByName(className) == null) {
                results.add("failed to get $className")
            }
        }
        for (funName in funNames) {
            if (resolver.getFunctionDeclarationsByName(funName, true).none()) {
                results.add("failed to get $funName")
            }
        }
        for (funName in overloadFunctionNames) {
            if (resolver.getFunctionDeclarationsByName(funName, true).toList().size != 2) {
                results.add("failed to get all $funName")
            }
        }
        for (propName in propNames) {
            if (resolver.getPropertyDeclarationByName(propName, true) == null) {
                results.add("failed to get $propName")
            }
        }
        if (results.isEmpty()) {
            results.add("all success")
        }
        return emptyList()
    }
}
