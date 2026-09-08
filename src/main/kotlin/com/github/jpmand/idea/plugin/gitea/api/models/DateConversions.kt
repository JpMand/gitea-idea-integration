package com.github.jpmand.idea.plugin.gitea.api.models

import java.time.OffsetDateTime
import java.util.Date

internal fun OffsetDateTime.toDate(): Date = Date.from(this.toInstant())
