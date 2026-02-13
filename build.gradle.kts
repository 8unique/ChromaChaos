// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.6.1" apply false
    id("com.android.library") version "8.6.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.10" apply false
    id("com.google.dagger.hilt.android") version "2.51" apply false
    id("com.google.devtools.ksp") version "1.9.10-1.0.13" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

/**
 * Force Kotlin stdlib and related artifacts to the Kotlin plugin version (1.9.10).
 * This prevents transitive dependencies that pull kotlin-stdlib:2.x from breaking the compiler
 * when the Kotlin plugin/compiler expects 1.9.x metadata.
 */
subprojects {
    configurations.all {
        resolutionStrategy.force(
            "org.jetbrains.kotlin:kotlin-stdlib:1.9.10",
            "org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.10",
            "org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.10",
            "org.jetbrains.kotlin:kotlin-reflect:1.9.10",
            "org.jetbrains.kotlin:kotlin-compiler-embeddable:1.9.10"
        )
    }
}
