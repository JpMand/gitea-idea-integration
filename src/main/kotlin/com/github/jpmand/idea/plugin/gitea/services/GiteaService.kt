package com.github.jpmand.idea.plugin.gitea.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class GiteaService(project: Project)
