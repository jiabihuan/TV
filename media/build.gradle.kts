// Copyright (C) 2016 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
import androidx.media3.buildlogic.Media3Modules
import java.io.File

// NOTE: Removed the buildscript {} block that previously applied
// `com.google.android.gms:strict-version-matcher-plugin:1.2.4`.
// Reason: 1.2.4 transitively pulls in `org.jetbrains.kotlin:kotlin-stdlib:2.3.20`
// (which requires `org.jetbrains:annotations:{strictly 13.0}`), but the media
// build-logic uses `kotlin-gradle-plugin:2.2.10` which requires
// `org.jetbrains:annotations:23.0.0`. The two constraints cannot both be
// satisfied, so Gradle aborts with:
//   "Cannot find a version of 'org.jetbrains:annotations' that satisfies
//    the version constraints ... Pinned to the embedded Kotlin".
// The strict-version-matcher-plugin is only needed when publishing to Google
// Play; this fork is consumed via composite build and does not need it.
// If the demos (cast/main) still need the plugin, they should declare it
// locally via their own `buildscript` block.

plugins {
  id("media3.android-application") apply false
  id("media3.android-library") apply false
  alias(libs.plugins.kotlin.compose.compiler) apply false
  id("gradlebuild.media3-build-logic")
}

allprojects {
  repositories {
    google()
    mavenCentral()
    maven {
      url = uri("https://jitpack.io")
      content { includeGroup("com.github.philburk") }
    }
  }
  if (project.hasProperty("externalBuildDir")) {
    val externalBuildDirProp = project.property("externalBuildDir") as String
    val externalBuildDirFile =
      if (File(externalBuildDirProp).isAbsolute) {
        File(externalBuildDirProp)
      } else {
        File(rootDir, externalBuildDirProp)
      }
    layout.buildDirectory.set(File(externalBuildDirFile, project.name))
  }
  group = "androidx.media3"
}

tasks.register("printReleaseArtifactIds") {
  description = "Prints the releaseArtifactId of modules configured for publishing."
  doLast {
    subprojects {
      // Check if the project is intended to be published by looking for a task
      // added by the maven-publish plugin.
      if (!tasks.names.contains("generatePomFileForReleasePublication")) {
        return@subprojects
      }
      Media3Modules.EXTERNAL_MODULES[project.name]?.artifactId?.let { println(it) }
        ?: logger.warn("WARN: Project $path has publish task but no releaseArtifactId.")
    }
  }
}
