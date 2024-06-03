import android.databinding.tool.ext.capitalizeUS

plugins {
    alias(libs.plugins.com.android.application)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    id("org.jetbrains.kotlin.plugin.parcelize")
    alias(libs.plugins.hiddenapirefine.plugin)
}

android {
    namespace = "io.github.a13e300.intenttracker"
    compileSdk = 33

    defaultConfig {
        applicationId = "io.github.a13e300.intenttracker"
        minSdk = 27
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        aidl = true
        buildConfig = true
    }
}

val adb: String = androidComponents.sdkComponents.adb.get().asFile.absolutePath

androidComponents.onVariants { variant ->
    val variantCapped = variant.name.capitalizeUS()
    val packageName = variant.applicationId.get()
    val pushDeployScript = task<Exec>("pushDeployScript${variantCapped}") {
        group = "IntentTracker"
        dependsOn(":app:install$variantCapped")
        commandLine(adb, "push", project.file("src/scripts/deploy.sh"), "/data/local/tmp/deploy_intent_tracker.sh")
    }
    val deployCLI = task<Exec>("deployCLI${variantCapped}") {
        group = "IntentTracker"
        dependsOn(pushDeployScript)
        commandLine(adb, "shell", "sh /data/local/tmp/deploy_intent_tracker.sh $packageName")
    }
}

dependencies {
    compileOnly(project(":HiddenApi"))
    compileOnly(libs.xposed)
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.commons.cli)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
}