package com.google.devtools.ksp.impl;

import com.intellij.mock.MockProject;
import org.jetbrains.kotlin.analysis.low.level.api.fir.FirIdeResolveStateService;

public class RegisterComponentService {
        public static void registerFirIdeResolveStateService(MockProject project) {
        project.getPicoContainer().registerComponentInstance(FirIdeResolveStateService.class.getName(), new FirIdeResolveStateService(project));
        }
}
