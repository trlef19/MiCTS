plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.lsplugin.jgit)
    alias(libs.plugins.lsplugin.apksign)
}

apksign {
    storeFileProperty = "androidStoreFile"
    storePasswordProperty = "androidStorePassword"
    keyAliasProperty = "androidKeyAlias"
    keyPasswordProperty = "androidKeyPassword"
}

val repo = jgit.repo()
val commitCount = (repo?.commitCount("refs/remotes/origin/main") ?: 1)
val latestTag = repo?.latestTag?.removePrefix("v") ?: "1.0"

android {
    namespace = "com.parallelc.micts"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.parallelc.micts"
        minSdk = 28
        targetSdk = 35
        versionCode = commitCount
        versionName = latestTag

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    applicationVariants.all {
        val variant = this
        variant.outputs
            .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
            .forEach { output ->
                output.outputFileName = "MiCTS_${variant.versionName}_${variant.versionCode}_${variant.baseName}.apk"
            }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
}

dependencies {
    compileOnly(project(":libxposed-compat"))
    compileOnly(libs.libxposed.api)
    implementation(libs.hiddenapibypass)
}