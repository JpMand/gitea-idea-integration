package com.github.jpmand.idea.plugin.gitea.authentication

import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector
import org.jetbrains.annotations.ApiStatus

@Suppress("UnstableApiUsage")
@ApiStatus.Internal
class GiteaLoginCollector : CounterUsagesCollector() {
    private val GROUP = EventLogGroup("gitea.login", 1)

    private val IS_GITEA_COM = EventFields.Boolean("is_gitea_com")
    private val LOGIN_SOURCE = EventFields.Enum<GiteaLoginSource>("source")
    private val IS_RE_LOGIN = EventFields.Boolean("re_login")
    private val LOGIN_EVENT = GROUP.registerEvent("login", LOGIN_SOURCE, IS_RE_LOGIN, IS_GITEA_COM)

    override fun getGroup(): EventLogGroup = GROUP

    fun login(loginData: GiteaLoginData) {
        LOGIN_EVENT.log(
            loginData.source,
            loginData.isReLogin,
            loginData.isGiteaCom
        )
    }
}

data class GiteaLoginData(
    val source: GiteaLoginSource,
    val isReLogin: Boolean,
    val isGiteaCom: Boolean
)

enum class GiteaLoginSource {
    GIT,
    SETTINGS,
    CLONE,
    SHARE,
    PR_TW,
    PR_LIST,
    PR_DETAILS,
    PR_TIMELINE,
    SNIPPET,
    WELCOME_SCREEN,
    UNKNOWN
}