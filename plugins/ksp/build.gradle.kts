description = "Kotlin Symbol Processor"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testCompileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    testRuntime(intellijDep())
    testCompileOnly(intellijDep()) { includeJars("idea", "idea_rt", "openapi") }

    testCompileOnly(intellijDep()) { includeJars("platform-api", "platform-impl") }

    Platform[192].orHigher {
        testRuntime(intellijPluginDep("java"))
    }

    compileOnly(intellijCoreDep()) { includeJars("intellij-core", "asm-all", "guava", rootProject = rootProject) }

    if (hasProperty("kspBaseVersion")) {
        val kspBaseVersion = properties["kspBaseVersion"] as String
        compile(kotlin("compiler", kspBaseVersion))
        compile(project(":kotlin-symbol-processing-api"))
    } else {
        compile(project(":compiler:util"))
        compile(project(":compiler:cli"))
        compile(project(":compiler:backend"))
        compile(project(":compiler:frontend"))
        compile(project(":compiler:frontend.java"))
        compile(project(":compiler:plugin-api"))
        compile(project(":kotlin-symbol-processing-api"))

        testCompile(projectTests(":compiler:tests-common"))
        testCompile(commonDep("junit:junit"))
    }
}

repositories {
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar {}

projectTest(parallel = true) {
    workingDir = rootDir
    dependsOn(":dist")
}

publish()

runtimeJar()

sourcesJar()
javadocJar()