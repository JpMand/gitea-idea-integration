package com.github.jpmand.idea.plugin.gitea.authentication.account

import com.github.jpmand.idea.plugin.gitea.api.GiteaServerPath
import com.intellij.collaboration.auth.ServerAccount
import com.intellij.openapi.util.NlsSafe
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Property
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.Transient
import org.jetbrains.annotations.VisibleForTesting

@Tag("account")
class GiteaAccount(
  @set:Transient
  @param:NlsSafe
  @Attribute("name")
  override var name: String = "",
  @Property(style = Property.Style.ATTRIBUTE, surroundWithTag = false)
  override val server: GiteaServerPath = GiteaServerPath(),
  @Attribute("id")
  @VisibleForTesting
  override val id: String = generateId()
) : ServerAccount() {
  override fun toString(): String = "$server/$name"
}