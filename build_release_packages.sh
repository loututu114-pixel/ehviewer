#!/bin/bash

# EhViewer 2.0.0.3 正式版构建脚本
# 构建渠道0000（默认版本）和3001的正式版APK包

set -e  # 遇到错误立即退出

echo "🚀 开始构建 EhViewer v2.0.0.3 正式版"
echo "=================================================="

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 版本信息读取自 Gradle（确保与产物一致）
VERSION=$(./gradlew -q :app:properties | awk -F': ' '/versionName/ {print $2; exit}')
VERSION_CODE=$(./gradlew -q :app:properties | awk -F': ' '/versionCode/ {print $2; exit}')
[ -z "$VERSION" ] && { echo -e "${RED}❌ 无法读取 versionName${NC}"; exit 1; }
[ -z "$VERSION_CODE" ] && { echo -e "${RED}❌ 无法读取 versionCode${NC}"; exit 1; }
BUILD_DATE=$(date '+%Y-%m-%d %H:%M:%S')

# 创建输出目录
OUTPUT_DIR="@apk/release-v${VERSION}"
mkdir -p "$OUTPUT_DIR"

echo -e "${BLUE}📦 输出目录: $OUTPUT_DIR${NC}"
echo -e "${BLUE}🔢 版本: ${VERSION} (${VERSION_CODE})${NC}"
echo ""

# 清理之前的构建
echo -e "${YELLOW}🧹 清理之前的构建...${NC}"
./gradlew clean
echo ""

# 函数：构建指定渠道的APK
build_channel() {
    local channel=$1
    local channel_name=$2
    
    echo -e "${BLUE}🔨 开始构建渠道 $channel ($channel_name)...${NC}"
    echo "--------------------------------"
    
    # 构建APK（统一使用 release 签名参数，缺失即失败）
    if ./gradlew assembleAppReleaseRelease -PCHANNEL_CODE="$channel"; then
        echo -e "${GREEN}✅ 渠道 $channel 构建成功${NC}"
        
        # 查找生成的APK文件
        APK_FILE=$(find app/build/outputs/apk/appRelease/release -name "*channel-${channel}*.apk" | head -1)
        
        if [ -f "$APK_FILE" ]; then
            # 复制到输出目录并重命名
            OUTPUT_FILE="$OUTPUT_DIR/EhViewer-v${VERSION}-channel-${channel}-${channel_name}.apk"
            cp "$APK_FILE" "$OUTPUT_FILE"
            
            # 获取文件大小
            FILE_SIZE=$(du -h "$OUTPUT_FILE" | cut -f1)
            
            echo -e "${GREEN}📦 APK已保存: $(basename "$OUTPUT_FILE")${NC}"
            echo -e "${GREEN}📏 文件大小: $FILE_SIZE${NC}"
            
            # 验证APK基本信息：包名与版本
            PKG=$(aapt dump badging "$OUTPUT_FILE" 2>/dev/null | awk -F"'" '/package:/ {print $2; exit}')
            VN=$(aapt dump badging "$OUTPUT_FILE" 2>/dev/null | awk -F"'" '/versionName=/ {print $4; exit}')
            VC=$(aapt dump badging "$OUTPUT_FILE" 2>/dev/null | awk -F"'" '/versionCode=/ {print $4; exit}')
            if [ "$PKG" != "com.hippo.ehviewer" ]; then
                echo -e "${RED}❌ 包名不一致: $PKG（期望 com.hippo.ehviewer）${NC}"; return 1
            fi
            if [ "$VN" != "$VERSION" ] || [ "$VC" != "$VERSION_CODE" ]; then
                echo -e "${RED}❌ 版本不一致: $VN($VC)（期望 $VERSION($VERSION_CODE)）${NC}"; return 1
            fi
            echo -e "${GREEN}✅ 包名与版本校验通过${NC}"
        else
            echo -e "${RED}❌ 未找到生成的APK文件${NC}"
            return 1
        fi
    else
        echo -e "${RED}❌ 渠道 $channel 构建失败${NC}"
        return 1
    fi
    
    echo ""
}

# 构建渠道0000（默认版本）
build_channel "0000" "default"

# 构建渠道3001
build_channel "3001" "partner"

# 生成构建报告
REPORT_FILE="$OUTPUT_DIR/build-report.md"
cat > "$REPORT_FILE" << EOF
# EhViewer v${VERSION} 正式版构建报告

## 构建信息
- **版本号**: ${VERSION}
- **构建时间**: ${BUILD_DATE}
- **构建环境**: $(uname -s) $(uname -m)
- **Gradle版本**: $(./gradlew --version | grep "Gradle" | head -1)

## 渠道包列表

| 渠道号 | 渠道名称 | APK文件名 | 文件大小 | 状态 |
|--------|----------|-----------|----------|------|
EOF

# 添加APK信息到报告
for apk in "$OUTPUT_DIR"/*.apk; do
    if [ -f "$apk" ]; then
        filename=$(basename "$apk")
        size=$(du -h "$apk" | cut -f1)
        
        # 提取渠道信息
        if [[ "$filename" =~ channel-([0-9]+) ]]; then
            channel="${BASH_REMATCH[1]}"
            if [ "$channel" = "0000" ]; then
                channel_name="默认版本"
            elif [ "$channel" = "3001" ]; then
                channel_name="合作渠道"
            else
                channel_name="未知"
            fi
            
            echo "| $channel | $channel_name | $filename | $size | ✅ 成功 |" >> "$REPORT_FILE"
        fi
    fi
done

cat >> "$REPORT_FILE" << EOF

## 重要改进 (v2.0.0.3)

### 🔄 渠道统计API重试机制
- **智能重试策略**: 指数退避算法（1秒 → 2秒 → 4秒）
- **本地缓存机制**: 失败请求自动缓存，网络恢复后重试
- **超时时间优化**: 从5秒增加到8秒，适应弱网环境
- **频率控制**: 1分钟内最多发送1次统计，避免重复请求

### 📊 统计功能增强
- **安装统计**: 点击画廊任意按钮时触发
- **下载统计**: 浏览器成功访问网页时触发  
- **激活统计**: 每天软件打开超过10次时触发
- **防重复机制**: 使用设备指纹和频率控制

### 🛡️ 稳定性提升
- **异常安全**: 所有统计操作都有完善的异常处理
- **内存优化**: 缓存最多50个请求，24小时自动过期
- **网络检测**: 智能检测网络状态，避免无效重试

## 安装说明

### 渠道选择
- **渠道0000**: 默认版本，适用于直接下载用户
- **渠道3001**: 合作渠道版本，用于渠道推广统计

### 系统要求
- **Android版本**: 6.0 (API 23) 及以上
- **RAM**: 建议2GB以上
- **存储空间**: 100MB以上可用空间
- **权限**: 需要存储权限用于文件管理功能

### 安装步骤
1. 下载对应渠道的APK文件
2. 开启"未知来源应用安装"权限
3. 点击APK文件进行安装
4. 首次启动会下载腾讯X5内核（约30MB）

## 技术规格

- **编译SDK**: Android 35
- **目标SDK**: Android 34  
- **最小SDK**: Android 23
- **签名**: Release签名
- **架构支持**: armeabi-v7a, arm64-v8a, x86, x86_64
- **混淆**: 已关闭（便于调试）

---

*构建时间: ${BUILD_DATE}*
*版本: ${VERSION}*
EOF

echo -e "${GREEN}📋 构建报告已生成: $REPORT_FILE${NC}"
echo ""

# 最终总结
echo -e "${GREEN}🎉 EhViewer v${VERSION} 正式版构建完成!${NC}"
echo "=================================================="
echo -e "${BLUE}📂 输出目录: $OUTPUT_DIR${NC}"
echo -e "${BLUE}📊 包含文件:${NC}"
ls -la "$OUTPUT_DIR"
echo ""
echo -e "${YELLOW}💡 重要提醒:${NC}"
echo -e "  • APK文件已包含重试机制和频率控制功能"
echo -e "  • 渠道统计将自动发送到 qudao.eh-viewer.com"
echo -e "  • 建议在不同网络环境下测试重试功能"
echo -e "  • 生产环境部署前请确认渠道号配置正确"
echo ""
echo -e "${GREEN}🚀 发布就绪! 🚀${NC}"