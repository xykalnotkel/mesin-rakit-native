plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "id.mesinrakit"
    compileSdk = 34

    defaultConfig {
        applicationId = "id.mesinrakit"
        minSdk = 23
        targetSdk = 34
        versionCode = 10
        versionName = "1.3.0"
        resourceConfigurations += setOf("in", "en")
    }

    /* Kunci tanda tangan dibaca dari environment. Isinya hanya ada di GitHub
       Secrets, jadi berkas keystore tidak pernah ikut ke repo. Kalau env-nya
       kosong (misalnya build di komputer sendiri), APK tetap kebangun
       memakai kunci debug bawaan. */
    signingConfigs {
        create("rilis") {
            val bekas = System.getenv("KEYSTORE_FILE")
            if (!bekas.isNullOrBlank()) {
                storeFile = file(bekas)
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
                keyAlias = System.getenv("KEY_ALIAS") ?: "mesinrakit"
                keyPassword = System.getenv("KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        debug { isMinifyEnabled = false }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("rilis")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    androidResources { noCompress += setOf("ttf") }
    packaging { resources { excludes += setOf("META-INF/*.kotlin_module") } }
}

dependencies {
}
