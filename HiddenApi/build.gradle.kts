plugins {
    alias(libs.plugins.com.android.library)
}

android {
    namespace = "io.github.a13e300.hiddenapi"
    compileSdk = 33

    defaultConfig {
        minSdk = 27

        consumerProguardFiles("consumer-rules.pro")
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
}

dependencies {
    implementation(libs.androidx.annotation)
    annotationProcessor(libs.hiddenapirefine.annotationprocessor)
    compileOnly(libs.hiddenapirefine.annotation)
}