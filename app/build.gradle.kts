plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.license)
}

if (file("google-services.json").exists()) {
    plugins.apply("com.google.gms.google-services")
    plugins.apply("com.google.firebase.crashlytics")
}

android {
    namespace = "com.hippo.ehviewer"
    testNamespace = "com.hippo.ehviewer.debug"
    compileSdk = 35

    flavorDimensions += "distribute"
    productFlavors {
        create("appRelease") {
            dimension = "distribute"
        }
    }

    defaultConfig {
        applicationId = "com.hippo.ehviewer"
        minSdk = 23
        targetSdk = 34
        versionCode = 200005
        versionName = "2.0.0.5"
        vectorDrawables.useSupportLibrary = true
        androidResources.localeFilters += listOf(
            "zh", "zh-rCN", "zh-rHK", "zh-rTW",
            "es", "ja", "ko", "fr", "de", "th"
        )
        testOptions.unitTests.isIncludeAndroidResources = true
        multiDexEnabled = true
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
        externalNativeBuild {
            cmake {
                cppFlags += ""
            }
        }
    }

    // 优化构建时内存使用
    dexOptions {
        javaMaxHeapSize = "4g"
        preDexLibraries = true
        maxProcessCount = 8
    }

    lint {
        disable += "MissingTranslation"
        abortOnError = false
        baseline = file("lint-baseline.xml")
        checkReleaseBuilds = true
    }

    signingConfigs {
        create("release") {
            storeFile = file("../debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            buildConfigField("String", "FILE_PROVIDER_AUTHORITY", "\"com.hippo.ehviewer.fileprovider\"")
            
            // 支持通过命令行参数设置渠道号，默认为3001
            val channelCode = project.findProperty("CHANNEL_CODE")?.toString() ?: "3001"
            buildConfigField("String", "CHANNEL_CODE", "\"$channelCode\"")
        }
        debug {
            applicationIdSuffix = ".debug"
            buildConfigField("String", "FILE_PROVIDER_AUTHORITY", "\"com.hippo.ehviewer.debug.fileprovider\"")
            buildConfigField("String", "CHANNEL_CODE", "\"0000\"")
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

//tasks.withType(JavaCompile) {
//    task -> task.dependsOn copyNotice
//}

dependencies {
    implementation(fileTree(mapOf("include" to listOf("*.jar"), "dir" to "libs")))

    // Core AndroidX dependencies
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity:1.2.4")
    implementation("androidx.fragment:fragment:1.8.6")
    implementation("androidx.recyclerview:recyclerview:1.2.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.browser:browser:1.3.0")
    implementation("androidx.biometric:biometric:1.2.0-alpha01")
    implementation("com.google.android.material:material:1.12.0")

    // EhViewer specific dependencies
    implementation("com.github.seven332.a7zip:extract-lite:1b21783")
    implementation("com.github.seven332:android-recaptcha:2bbb3459a8")
    implementation("com.github.seven332:android-resource:0.1.0")
    implementation("com.github.seven332:animator:0.1.0")
    implementation("com.github.seven332:drawerlayout:ea2bb388f0")
    implementation("com.github.seven332:easyrecyclerview:0.1.1")
    implementation("com.github.xiaojieonly:Pagination_Sxj:8d2c34d8f53927aeaa1d405d8653969379cb37ec")
    implementation("com.github.seven332:hotspot:0.1.0")
    implementation("com.github.seven332:refreshlayout:0.1.0")
    // implementation("com.github.seven332:ripple:0.1.2") // 已包含在项目中，避免重复
    implementation("com.github.seven332:streampipe:0.1.0")

    implementation("com.h6ah4i.android.widget.advrecyclerview:advrecyclerview:1.0.0")

    // Network and parsing
    implementation("com.squareup.okhttp3:okhttp:3.14.7")
    implementation("com.squareup.okhttp3:okhttp-dnsoverhttps:3.14.7")
    implementation("org.jsoup:jsoup:1.15.3")
    implementation("org.ccil.cowan.tagsoup:tagsoup:1.2.1")
    implementation("org.greenrobot:eventbus:3.3.1")
    implementation("com.alibaba:fastjson:1.2.83")
    implementation("com.google.code.gson:gson:2.10.1")

    // UI components
    implementation("com.github.ybq:Android-SpinKit:1.4.0")
    implementation("com.github.amlcurran.showcaseview:library:5.4.3")
    implementation("com.github.xiaojieonly:Android-Request-Inspector-WebView:70403bb")

    // Database and security
    implementation("org.greenrobot:greendao:3.0.0")
    implementation("org.conscrypt:conscrypt-android:2.5.1")

    // Native libraries
    implementation("com.fpliu.ndk.pkg.prefab.android.21:libpng:1.6.37")
    implementation("com.getkeepsafe.relinker:relinker:1.4.4")

    // MultiDex (AndroidX version)
    implementation("androidx.multidex:multidex:2.0.1")

    // Firebase (王子公主 app analytics)
    implementation("com.google.firebase:firebase-crashlytics:19.4.2")
    implementation("com.google.firebase:firebase-analytics:22.4.0")
    implementation("com.google.firebase:firebase-messaging:24.0.0")

    // Test dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.2.1")
    testImplementation("org.jooq:joor:0.9.6")
    
    // Mockito for unit testing
    testImplementation("org.mockito:mockito-core:4.11.0")
    testImplementation("org.mockito:mockito-android:4.11.0")
    testImplementation("org.mockito:mockito-inline:4.11.0")
    
    // AndroidX Test dependencies
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test:runner:1.5.2")
    testImplementation("androidx.test:rules:1.5.0")
    testImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("androidx.test.ext:truth:1.5.0")
    
    // Android instrumentation testing
    androidTestImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-web:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")
    
    // Mockito for instrumentation testing
    androidTestImplementation("org.mockito:mockito-android:4.11.0")

    // WebView
    runtimeOnly("androidx.webkit:webkit:1.13.0")

    // Tencent X5 Browser SDK
    implementation("com.tencent.tbs:tbssdk:44286")
}

configurations.configureEach {
    resolutionStrategy {
        force("com.github.seven332:glgallery:25893283ca")
        force("com.github.seven332:glview:ba6aee61d7")
        force("com.github.seven332:glview-image:68d94b0fc2")
        force("com.github.seven332:image:09b43c0c68")

        exclude(group = "com.github.seven332", module = "okhttp")
        

        // Force all Support Library dependencies to use AndroidX equivalents
        force("androidx.multidex:multidex:2.0.1")
        force("androidx.appcompat:appcompat:1.7.0")
        force("androidx.recyclerview:recyclerview:1.2.0")
        force("androidx.cardview:cardview:1.0.0")
        force("androidx.fragment:fragment:1.8.6")
        force("androidx.preference:preference-ktx:1.2.1")
        force("androidx.browser:browser:1.3.0")
        force("androidx.biometric:biometric:1.2.0-alpha01")
        force("com.google.android.material:material:1.12.0")
    }
}

