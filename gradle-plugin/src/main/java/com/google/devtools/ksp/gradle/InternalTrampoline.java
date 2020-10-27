package com.google.devtools.ksp.gradle;

import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation;
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileTaskData;

// FIXME: Ask upstream to open these API
public class InternalTrampoline {
    public static void KotlinCompileTaskData_register(String taskName, KotlinCompilation<?> kotlinCompilation) {
        KotlinCompileTaskData kotlinCompileTaskData = KotlinCompileTaskData.Companion.register(taskName, kotlinCompilation);
    }
}
