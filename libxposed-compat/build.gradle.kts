plugins {
    alias(libs.plugins.androidLibrary)
}

android {
    namespace = "io.github.libxposed"
    compileSdk = 35
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
