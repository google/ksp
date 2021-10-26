package com.google.devtools.ksp.impl

import com.intellij.openapi.project.Project
import com.intellij.pom.PomModelAspect
import com.intellij.pom.core.impl.PomModelImpl
import com.intellij.pom.event.PomModelEvent
import com.intellij.psi.impl.source.PostprocessReformattingAspect

class HackPomModel(project: Project) : PomModelImpl(project) {

    var myAspect = PostprocessReformattingAspect(project)

    override fun <T : PomModelAspect> getModelAspect(aClass: Class<T>): T {
        return if (myAspect.javaClass == aClass) myAspect as T else super.getModelAspect<T>(aClass)
    }

    override fun updateDependentAspects(event: PomModelEvent?) {
        super.updateDependentAspects(event)
        myAspect.update(event!!)
    }
}
