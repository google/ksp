package com.google.devtools.ksp.gradle;

import org.gradle.api.provider.Provider;
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation;
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileTaskData;

import java.io.File;

// FIXME: Ask upstream to open these API
public class InternalTrampoline {
    public static void KotlinCompileTaskData_register(String taskName, KotlinCompilation<?> kotlinCompilation, Provider<File> destinationDirProvider)  {
        KotlinCompileTaskData kotlinCompileTaskData = KotlinCompileTaskData.Companion.register(taskName, kotlinCompilation);
        kotlinCompileTaskData.getDestinationDir().set(destinationDirProvider);
    }
}
