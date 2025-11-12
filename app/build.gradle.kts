plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.voltmart"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.voltmart"
        minSdk = 27
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/NOTICE.md",
                "META-INF/LICENSE.md",
                "META-INF/NOTICE",
                "META-INF/LICENSE",
                "META-INF/DEPENDENCIES",
                "META-INF/ASL2.0"
            )
            // 或者用 pickFirsts 也行：
            // pickFirsts += setOf("META-INF/NOTICE.md", "META-INF/LICENSE.md")
        }
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    // UI 基础
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.activity:activity:1.9.3")

    // 其它第三方
    implementation("com.airbnb.android:lottie:6.0.0")
    implementation("com.google.android.gms:play-services-auth:21.1.0")
    implementation("com.github.mancj:MaterialSearchBar:0.8.5")
    implementation("org.imaginativeworld.whynotimagecarousel:whynotimagecarousel:2.1.0")
    implementation("com.facebook.shimmer:shimmer:0.5.0")
    implementation("com.squareup.picasso:picasso:2.71828")
    implementation("com.makeramen:roundedimageview:2.3.0")
    implementation("com.github.f0ris.sweetalert:library:1.6.2")

    // Firebase 用 BOM 管版本，避免混搭
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-dynamic-links")
    implementation("com.firebaseui:firebase-ui-firestore:8.0.2")

    // JavaMail（导致 NOTICE.md 冲突的两个库，保留即可，冲突已通过 packaging 解决）
    implementation("com.sun.mail:android-mail:1.6.7")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

}

