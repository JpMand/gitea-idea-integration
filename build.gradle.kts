import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java") // Java support
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.intelliJPlatform) // IntelliJ Platform Gradle Plugin
    alias(libs.plugins.changelog) // Gradle Changelog Plugin
    alias(libs.plugins.qodana) // Gradle Qodana Plugin
    alias(libs.plugins.kover) // Gradle Kover Plugin
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

// Set the JVM language level used to build the project.
kotlin {
    jvmToolchain(25)
}

// Configure project's dependencies
repositories {
    mavenCentral()

    // IntelliJ Platform Gradle Plugin Repositories Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-repositories-extension.html
    intellijPlatform {
        defaultRepositories()
    }
}

// A separate source set for UI integration tests driven via the IntelliJ Starter/Driver
// framework (https://plugins.jetbrains.com/docs/intellij/integration-tests.html) against a
// sandbox IDE that the `integrationTest` task launches and tears down itself — unlike the old
// Remote Robot setup, there's no separate "leave a sandbox running in another terminal" step.
// Kept apart from the fast unit `test` source set since these need a live IDE instance to run.
sourceSets {
    create("integrationTest") {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

// Dependencies are managed with Gradle version catalog - read more: https://docs.gradle.org/current/userguide/version_catalogs.html
dependencies {
    // Only the jsr310 module classes are needed here — jackson-core/-databind/-annotations
    // are already provided by the IntelliJ Platform itself. Pulling in this artifact's own
    // transitive Jackson jars puts a second, differently-versioned copy on the runtime
    // classpath alongside the platform's, which breaks other bundled plugins that expect
    // their own bundled Jackson classes (observed as a JsonFormat.Shape.POJO NoSuchFieldError
    // when the full plugin set loads, e.g. in MyPluginTest's light IDE fixture).
    implementation(libs.jacksonDatatypeJsr310) {
        exclude(group = "com.fasterxml.jackson.core")
    }
    testImplementation(libs.junit)
    testImplementation(libs.opentest4j)

    // main/test get the Kotlin stdlib transitively from the IntelliJ Platform dependency
    // (see `kotlin.stdlib.default.dependency = false` in gradle.properties); integrationTest
    // doesn't depend on the platform at all (it drives a separate IDE process over JMX, see
    // AGENTS.md), so it needs the stdlib and its own test-runner stack added explicitly.
    "integrationTestImplementation"(kotlin("stdlib"))
    "integrationTestImplementation"(libs.junitJupiter)
    "integrationTestImplementation"(libs.kodeinDi)
    "integrationTestImplementation"(libs.kotlinxCoroutinesCore)

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        intellijIdea(providers.gradleProperty("platformVersion"))

        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugins(providers.gradleProperty("platformBundledPlugins").map { it.split(',') })

        // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file for plugin from JetBrains Marketplace.
        plugins(providers.gradleProperty("platformPlugins").map { it.split(',') })

        // Module Dependencies. Uses `platformBundledModules` property from the gradle.properties file for bundled IntelliJ Platform modules.
        bundledModules(providers.gradleProperty("platformBundledModules").map { it.split(',') })

        testFramework(TestFrameworkType.Platform)
        // Pulls ide-starter-{squashed,junit5,driver} and driver-{client,sdk,model} — the
        // Starter/Driver integration-test stack (see integrationTest task below).
        testFramework(TestFrameworkType.Starter, configurationName = "integrationTestImplementation")
    }
}

// Configure IntelliJ Platform Gradle Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html
intellijPlatform {
    pluginConfiguration {
        name = providers.gradleProperty("pluginName")
        version = providers.gradleProperty("pluginVersion")

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        description = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }

        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        changeNotes = providers.gradleProperty("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = providers.gradleProperty("pluginUntilBuild")
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html#specifying-a-release-channel
        channels = providers.gradleProperty("pluginVersion").map { listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" }) }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.empty()
    repositoryUrl = providers.gradleProperty("pluginRepositoryUrl")
    versionPrefix = ""
}

// Configure Gradle Kover Plugin - read more: https://kotlin.github.io/kotlinx-kover/gradle-plugin/#configuration-details
kover {
    reports {
        total {
            xml {
                onCheck = true
            }
        }
    }
}

tasks {
    wrapper {
        gradleVersion = providers.gradleProperty("gradleVersion").get()
    }

    publishPlugin {
        dependsOn(patchChangelog)
    }
}

// Drives a sandbox IDE via the Starter/Driver framework
// (https://plugins.jetbrains.com/docs/intellij/integration-tests.html) — a single command
// launches, exercises, and tears down the IDE itself; no separate "leave a sandbox running"
// terminal needed (unlike the old Remote Robot setup this replaced).
val integrationTest by intellijPlatformTesting.testIdeUi.registering {
    task {
        outputs.upToDateWhen { false } // always talks to a live external process, never cache
        val integrationTestSourceSet = sourceSets.getByName("integrationTest")
        testClassesDirs = integrationTestSourceSet.output.classesDirs
        classpath = integrationTestSourceSet.runtimeClasspath
        // Lets the tests pin the Starter-downloaded IDE to the same build the plugin actually
        // targets, rather than whatever Starter's own product catalog considers current for the
        // product (which can be well below this plugin's sinceBuild floor).
        systemProperty("gitea.test.platformBuildNumber", providers.gradleProperty("platformTestBuildNumber").get())
        useJUnitPlatform {
            // The ide-starter/driver dependencies transitively pull in junit-vintage-engine,
            // which then fails test discovery outright since this source set has no JUnit 4 on
            // its classpath (nor any JUnit 4-style tests to run) — exclude it rather than add a
            // JUnit 4 dependency purely to satisfy an engine we never use.
            excludeEngines("junit-vintage")
            // GiteaPRListInteractionManualTest needs a local Docker Gitea instance + cloned
            // sample repo (see PR_REVIEW_MIGRATION_PLAN.md) that CI doesn't provision, so it's
            // excluded by default. Run it explicitly with `-PincludeManualTests`.
            if (!project.hasProperty("includeManualTests")) {
                excludeTags("manual")
            }
        }
    }
}
