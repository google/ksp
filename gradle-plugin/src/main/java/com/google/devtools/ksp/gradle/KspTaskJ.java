package com.google.devtools.ksp.gradle;

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments;
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile;
import org.jetbrains.kotlin.gradle.tasks.SourceRoots;
import org.jetbrains.kotlin.incremental.ChangedFiles;

// FIXME: Ask upstream to open these API
public class KspTaskJ extends KotlinCompile {
    @Override
    public void callCompilerAsync$kotlin_gradle_plugin(K2JVMCompilerArguments args, SourceRoots sourceRoots, ChangedFiles changedFiles) {
        KspSubpluginKt.addChangedFiles(args, changedFiles);
        super.callCompilerAsync$kotlin_gradle_plugin(args, sourceRoots, changedFiles);
    }
}