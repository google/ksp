val testRepo: String by project

plugins {
    // DO NOT CHANGE THE ORDER.
    id("com.google.devtools.ksp")
    id("com.android.library")
    kotlin("android")
}

version = "1.0-SNAPSHOT"

repositories {
    maven(testRepo)
    mavenCentral()
    maven("https://redirector.kotlinlang.org/maven/bootstrap/")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":test-processor"))
    ksp(project(":test-processor"))
}

android {
    namespace = "com.example.mylibrary"
    compileSdk = 34
    defaultConfig {
        minSdk = 34
    }
    lint {
        targetSdk = 34
    }

    testOptions {
        targetSdk = 34
    }
}

ksp {
    arg("option1", "value1")
    arg("option2", "value2")
}

androidComponents.onVariants { variant ->
    val kotlinGenTaskProvider = project.tasks.register(
        "generate${variant.name}KotlinSources",
        KotlinSourceGeneratingTask::class.java
    )

    kotlinGenTaskProvider.configure {
        this.packageName.set("com.kotlingen")
        this.sourceFiles.set(
            variant.sources.kotlin!!.static
        )
    }
    variant.sources.java!!.addGeneratedSourceDirectory(
        kotlinGenTaskProvider, KotlinSourceGeneratingTask::outputDir
    )


    val javaGenTaskProvider = project.tasks.register(
        "generate${variant.name}JavaSources",
        JavaSourceGeneratingTask::class.java
    )
    javaGenTaskProvider.configure {
        this.packageName.set("com.javagen")
        this.sourceFiles.set(
            variant.sources.java!!.static
        )
    }
    variant.sources.java!!.addGeneratedSourceDirectory(
        javaGenTaskProvider, JavaSourceGeneratingTask::outputDir
    )
}

abstract class KotlinSourceGeneratingTask: DefaultTask() {
    @get:Input
    abstract val packageName: Property<String>
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    @get:InputFiles
    abstract val sourceFiles: ListProperty<Directory>
    @TaskAction
    fun generate() {
        val outputFolder = File(outputDir.get().asFile, packageName.get().replace('.', File.separatorChar))
        outputFolder.mkdirs()
        File(outputFolder, "MyKotlinClass.kt").writeText(
            """
                package ${packageName.get()}
                import com.example.annotation.Builder
                
                @Builder
                class MyKotlinClass {
                    fun someFunctionUsingGeneratedAPIs() {
                        System.err.println("Hello world !")
                    }
                }
            """.trimIndent()
        )
    }
}

abstract class JavaSourceGeneratingTask: DefaultTask() {
    @get:Input
    abstract val packageName: Property<String>
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    @get:InputFiles
    abstract val sourceFiles: ListProperty<Directory>
    @TaskAction
    fun generate() {
        val outputFolder = File(outputDir.get().asFile, packageName.get().replace('.', File.separatorChar))
        outputFolder.mkdirs()
        File(outputFolder, "MyJavaClass.java").writeText(
            """
                package ${packageName.get()};
                import com.example.annotation.Builder;
                
                @Builder
                public class MyJavaClass {
                    public static void someFunctionUsingGeneratedAPIs() {
                        System.err.println("Hello world !");
                    }
                }
            """.trimIndent()
        )
    }
}

