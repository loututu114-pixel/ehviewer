# 渠道打包指南

## 概述

渠道统计SDK已集成到EhViewer中，支持向 `qudao.eh-viewer.com/api` 发送统计数据。

## 默认配置

- **默认渠道号**: 0000
- **统计API**: https://qudao.eh-viewer.com/api
- **软件ID**: com.hippo.ehviewer

## 修改渠道号打包

### 方法1：修改 build.gradle.kts

在 `app/build.gradle.kts` 中修改 `buildConfigField` 的渠道号：

```kotlin
buildConfigField("String", "CHANNEL_CODE", "\"3001\"")  // 改为目标渠道号
```

### 方法2：通过环境变量

```bash
# 设置环境变量
export CHANNEL_CODE=3001

# 修改 build.gradle.kts 使用环境变量
buildConfigField("String", "CHANNEL_CODE", "\"${System.getenv('CHANNEL_CODE') ?: '0000'}\"")
```

### 方法3：使用Gradle参数

```bash
# 打包时传递参数
./gradlew assembleRelease -PchannelCode=3001
```

对应的build.gradle.kts配置：
```kotlin
val channelCode = project.findProperty("channelCode")?.toString() ?: "0000"
buildConfigField("String", "CHANNEL_CODE", "\"$channelCode\"")
```

## 统计事件

SDK自动跟踪以下事件：

1. **安装统计** - 应用启动时自动发送
2. **下载统计** - 手动调用 `ChannelTracker.getInstance().trackDownload()`
3. **激活统计** - 手动调用 `ChannelTracker.getInstance().trackActivate()`

## 容错机制

- 所有网络请求都有超时设置（5秒）
- 异常不会导致应用崩溃，只记录日志
- 自动重试机制（内置在OkHttp中）
- 网络不可用时静默失败

## 使用示例

```java
// 获取当前渠道号
String channel = ChannelTracker.getInstance().getChannelCode();

// 手动发送激活统计
ChannelTracker.getInstance().trackActivate("license-key-here", new ChannelTracker.TrackCallback() {
    @Override
    public void onResult(boolean success, String message) {
        Log.d("Channel", "Activate result: " + success + " - " + message);
    }
});
```

## 常用渠道号

- 0000 - 默认渠道
- 3001 - 特殊推广渠道  
- 其他 - 根据需要配置