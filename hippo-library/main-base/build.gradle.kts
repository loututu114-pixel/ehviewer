// Hippo Library - Android通用组件库构建配置
// 版本: 1.0.0
// 更新时间: 2024年12月

plugins {
    // Android插件
    id("com.android.application") version "8.2.0" apply false
    id("com.android.library") version "8.2.0" apply false

    // Kotlin插件
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false

    // 依赖注入
    id("com.google.dagger.hilt.android.plugin") version "2.48" apply false

    // 代码质量
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.4" apply false
}

// 全局任务
task<Delete>("clean") {
    delete(rootProject.buildDir)
}

// 全局仓库配置
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
}

// 子项目统一配置
subprojects {
    afterEvaluate {
        if (this.plugins.hasPlugin("com.android.application") ||
            this.plugins.hasPlugin("com.android.library")) {

            configure<com.android.build.gradle.BaseExtension> {
                compileSdkVersion(34)

                defaultConfig {
                    minSdk = 21
                    targetSdk = 34
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

                    // 统一版本配置
                    buildConfigField("String", "LIBRARY_VERSION", "\"1.0.0\"")
                    buildConfigField("String", "BUILD_TIME", "\"${System.currentTimeMillis()}\"")
                }

                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_11
                    targetCompatibility = JavaVersion.VERSION_11
                }

                // Kotlin配置
                if (this is com.android.build.gradle.LibraryExtension ||
                    this is com.android.build.gradle.AppExtension) {
                    kotlinOptions {
                        jvmTarget = "11"
                        freeCompilerArgs = listOf(
                            "-Xjvm-default=all",
                            "-opt-in=kotlin.RequiresOptIn"
                        )
                    }
                }

                // 代码质量检查
                lintOptions {
                    isCheckReleaseBuilds = true
                    isAbortOnError = false
                    xmlReport = true
                    htmlReport = true
                    xmlOutput = file("$buildDir/reports/lint-results.xml")
                    htmlOutput = file("$buildDir/reports/lint-results.html")
                }
            }
        }
    }
}

// 代码质量检查任务
task("checkAll") {
    group = "verification"
    description = "运行所有代码质量检查"

    dependsOn("ktlintCheck", "detekt")
}

task("ktlintCheck") {
    group = "verification"
    description = "Kotlin代码风格检查"
}

task("detekt") {
    group = "verification"
    description = "静态代码分析"
}

// 测试任务
task("testAll") {
    group = "verification"
    description = "运行所有单元测试"

    dependsOn(subprojects.map { "${it.name}:test" })
}

task("testDebug") {
    group = "verification"
    description = "运行Debug版本单元测试"

    dependsOn(subprojects.map { "${it.name}:testDebugUnitTest" })
}

task("testRelease") {
    group = "verification"
    description = "运行Release版本单元测试"

    dependsOn(subprojects.map { "${it.name}:testReleaseUnitTest" })
}

// 构建任务
task("buildAll") {
    group = "build"
    description = "构建所有模块和应用"

    dependsOn(subprojects.map { "${it.name}:build" })
}

task("buildDebug") {
    group = "build"
    description = "构建Debug版本"

    dependsOn(subprojects.map { "${it.name}:assembleDebug" })
}

task("buildRelease") {
    group = "build"
    description = "构建Release版本"

    dependsOn(subprojects.map { "${it.name}:assembleRelease" })
}

// 文档任务
task("dokkaAll") {
    group = "documentation"
    description = "生成所有模块的API文档"

    dependsOn(subprojects.map { "${it.name}:dokkaHtml" })
}

task("dokkaMerge") {
    group = "documentation"
    description = "合并所有模块的API文档"
}

// 发布任务
task("publishAll") {
    group = "publishing"
    description = "发布所有模块到仓库"

    dependsOn(subprojects.map { "${it.name}:publish" })
}

task("publishToMavenLocal") {
    group = "publishing"
    description = "发布到本地Maven仓库"

    dependsOn(subprojects.map { "${it.name}:publishToMavenLocal" })
}

// 依赖分析任务
task("dependencyTree") {
    group = "help"
    description = "显示项目依赖树"

    doLast {
        subprojects.forEach { project ->
            println("\n=== ${project.name} 依赖树 ===")
            project.configurations.forEach { config ->
                if (config.isCanBeResolved) {
                    println("${config.name}:")
                    config.resolvedConfiguration.firstLevelModuleDependencies.forEach { dep ->
                        println("  - ${dep.moduleName}:${dep.moduleVersion}")
                    }
                }
            }
        }
    }
}

// 模块信息任务
task("moduleInfo") {
    group = "help"
    description = "显示所有模块信息"

    doLast {
        println("=== Hippo Library 模块信息 ===")
        println("总模块数: ${subprojects.size}")

        val coreModules = listOf("network", "database", "ui", "utils", "settings", "notification", "image", "filesystem")
        val featureModules = subprojects.map { it.name }.filter { !coreModules.contains(it) }

        println("\n核心模块 (${coreModules.size}个):")
        coreModules.forEach { println("  - $it") }

        println("\n功能模块 (${featureModules.size}个):")
        featureModules.forEach { println("  - $it") }

        println("\n构建信息:")
        println("  - Gradle版本: ${gradle.gradleVersion}")
        println("  - Android Gradle插件版本: 8.2.0")
        println("  - Kotlin版本: 1.9.22")
    }
}

// 清理任务
task("deepClean") {
    group = "build"
    description = "深度清理所有构建文件"

    doLast {
        delete(rootProject.buildDir)
        subprojects.forEach { project ->
            delete(project.buildDir)
            delete(file("${project.projectDir}/.gradle"))
            delete(file("${project.projectDir}/build"))
        }
        println("深度清理完成")
    }
}

// 默认任务
defaultTasks("buildAll")
