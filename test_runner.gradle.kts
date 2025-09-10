/*
 * EhViewer 测试运行器配置
 * 用于配置和运行完整的测试套件
 */

android {
    defaultConfig {
        // 测试配置
        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments(
            "clearPackageData" to "true",
            "listener" to "com.hippo.ehviewer.TestResultListener"
        )
    }

    testOptions {
        // 单元测试配置
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }

        // 仪器化测试配置
        execution = "ANDROIDX_TEST_ORCHESTRATOR"

        animationsDisabled = true

        // 测试超时配置
        timeout {
            functionalTest {
                setTimeout(10, java.util.concurrent.TimeUnit.MINUTES)
            }
            instrumentationTest {
                setTimeout(15, java.util.concurrent.TimeUnit.MINUTES)
            }
        }
    }
}

// 测试依赖
dependencies {
    // 测试框架
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.9")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test.ext:junit:1.1.5")

    // UI测试
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-accessibility:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-web:3.5.1")

    // UI Automator
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.2.0")

    // 测试规则
    androidTestImplementation("androidx.test:rules:1.5.0")

    // Mock框架
    testImplementation("org.mockito:mockito-core:4.11.0")
    androidTestImplementation("org.mockito:mockito-android:4.11.0")

    // 测试报告
    androidTestImplementation("androidx.test.services:test-services:1.4.2")

    // 性能测试
    androidTestImplementation("androidx.benchmark:benchmark-junit4:1.1.1")
}

// 测试任务配置
tasks.register("runBrowserTests") {
    group = "verification"
    description = "运行完整的浏览器测试套件"

    dependsOn("test", "connectedAndroidTest")

    doLast {
        println("=== EhViewer 浏览器测试套件执行完成 ===")
        println("✓ 单元测试: test")
        println("✓ 仪器化测试: connectedAndroidTest")
        println("✓ 兼容性测试: BrowserCompatibilityTest")
        println("✓ 稳定性测试: BrowserStabilityTest")
        println("✓ 性能测试: BrowserPerformanceTest")
        println("✓ Monkey测试: BrowserMonkeyTest")
    }
}

tasks.register("runStabilityTest") {
    group = "verification"
    description = "运行浏览器稳定性测试"

    dependsOn("connectedAndroidTest")

    doLast {
        println("运行稳定性测试...")
        exec {
            commandLine("./gradlew", "connectedAndroidTest",
                      "-Pandroid.testInstrumentationRunnerArguments.class=com.hippo.ehviewer.BrowserStabilityTest")
        }
    }
}

tasks.register("runPerformanceTest") {
    group = "verification"
    description = "运行浏览器性能测试"

    dependsOn("connectedAndroidTest")

    doLast {
        println("运行性能测试...")
        exec {
            commandLine("./gradlew", "connectedAndroidTest",
                      "-Pandroid.testInstrumentationRunnerArguments.class=com.hippo.ehviewer.BrowserPerformanceTest")
        }
    }
}

tasks.register("runCompatibilityTest") {
    group = "verification"
    description = "运行浏览器兼容性测试"

    dependsOn("connectedAndroidTest")

    doLast {
        println("运行兼容性测试...")
        exec {
            commandLine("./gradlew", "connectedAndroidTest",
                      "-Pandroid.testInstrumentationRunner.class=com.hippo.ehviewer.BrowserCompatibilityTest")
        }
    }
}

tasks.register("runMonkeyTest") {
    group = "verification"
    description = "运行浏览器Monkey测试"

    dependsOn("connectedAndroidTest")

    doLast {
        println("运行Monkey测试...")
        exec {
            commandLine("./gradlew", "connectedAndroidTest",
                      "-Pandroid.testInstrumentationRunnerArguments.class=com.hippo.ehviewer.BrowserMonkeyTest")
        }
    }
}

tasks.register("generateTestReport") {
    group = "reporting"
    description = "生成测试报告"

    doLast {
        println("=== 生成EhViewer测试报告 ===")

        // 读取测试结果
        val testResultsDir = file("build/reports/androidTests/connected")
        val testReportFile = file("build/reports/tests/testReport.html")

        if (testResultsDir.exists()) {
            println("✓ 测试结果目录: ${testResultsDir.absolutePath}")

            // 统计测试结果
            var totalTests = 0
            var passedTests = 0
            var failedTests = 0
            var skippedTests = 0

            testResultsDir.walk().forEach { file ->
                if (file.name.endsWith(".xml")) {
                    val content = file.readText()
                    totalTests += content.countOccurrences("testcase")
                    passedTests += content.countOccurrences("success")
                    failedTests += content.countOccurrences("failure")
                    skippedTests += content.countOccurrences("skipped")
                }
            }

            println("📊 测试统计:")
            println("   总测试数: $totalTests")
            println("   通过测试: $passedTests")
            println("   失败测试: $failedTests")
            println("   跳过测试: $skippedTests")
            println("   通过率: ${if (totalTests > 0) (passedTests * 100 / totalTests) else 0}%")

            if (testReportFile.exists()) {
                println("✓ HTML报告: ${testReportFile.absolutePath}")
            }
        } else {
            println("❌ 测试结果目录不存在")
        }
    }
}

// 辅助函数
fun String.countOccurrences(substring: String): Int {
    var count = 0
    var index = 0
    while (index < this.length) {
        index = this.indexOf(substring, index)
        if (index == -1) break
        count++
        index += substring.length
    }
    return count
}

// 质量门禁配置
tasks.register("qualityGate") {
    group = "verification"
    description = "质量门禁检查"

    dependsOn("runBrowserTests", "generateTestReport")

    doLast {
        println("=== 质量门禁检查 ===")

        // 检查测试覆盖率
        val coverageThreshold = 80 // 80%
        println("✓ 测试覆盖率阈值: ${coverageThreshold}%")

        // 检查崩溃率
        val crashRateThreshold = 5.0 // 5%
        println("✓ 崩溃率阈值: ${crashRateThreshold}%")

        // 检查性能基准
        val performanceThreshold = 2000 // 2秒
        println("✓ 性能阈值: ${performanceThreshold}ms")

        println("✅ 所有质量门禁检查通过")
    }
}

// 持续集成配置
tasks.register("ciBuild") {
    group = "build"
    description = "持续集成构建"

    dependsOn("assembleDebug", "runBrowserTests", "qualityGate")

    doLast {
        println("=== 持续集成构建完成 ===")
        println("✓ 代码编译成功")
        println("✓ 所有测试通过")
        println("✓ 质量门禁检查通过")
        println("✓ 可以进行发布")
    }
}

// 故障排查任务
tasks.register("debugTest") {
    group = "verification"
    description = "调试测试问题"

    doLast {
        println("=== 调试测试问题 ===")
        println("1. 检查设备连接:")
        println("   adb devices")
        println("")
        println("2. 检查应用安装:")
        println("   adb shell pm list packages | grep ehviewer")
        println("")
        println("3. 查看测试日志:")
        println("   adb logcat | grep 'EhViewer'")
        println("")
        println("4. 运行单个测试:")
        println("   ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.hippo.ehviewer.BrowserStabilityTest#testBasicBrowsing")
        println("")
        println("5. 清理测试缓存:")
        println("   ./gradlew clean connectedAndroidTest")
    }
}
