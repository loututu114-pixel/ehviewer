#!/bin/bash

# EhViewer v2.0.0.3 渠道3001 ADB安装脚本
# 安装EhViewer渠道3001版本到Android设备

set -e

echo "🚀 EhViewer v2.0.0.3 渠道3001 安装程序"
echo "=========================================="

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# APK文件路径
APK_FILE="@apk/release-v2.0.0.3/EhViewer-v2.0.0.3-channel-3001-partner.apk"
EXPECTED_MD5="babaaabda9c0a98d92210f98adda92c6"

# 检查APK文件是否存在
echo -e "${BLUE}📦 检查APK文件...${NC}"
if [ ! -f "$APK_FILE" ]; then
    echo -e "${RED}❌ 错误: 找不到APK文件: $APK_FILE${NC}"
    exit 1
fi

# 验证APK文件完整性
echo -e "${BLUE}🔍 验证APK文件完整性...${NC}"
ACTUAL_MD5=$(md5 -q "$APK_FILE")
if [ "$ACTUAL_MD5" != "$EXPECTED_MD5" ]; then
    echo -e "${RED}❌ 错误: APK文件MD5校验失败${NC}"
    echo -e "   期望: $EXPECTED_MD5"
    echo -e "   实际: $ACTUAL_MD5"
    exit 1
fi
echo -e "${GREEN}✅ APK文件完整性验证通过${NC}"

# 获取APK文件信息
APK_SIZE=$(du -h "$APK_FILE" | cut -f1)
echo -e "${BLUE}📏 APK文件大小: $APK_SIZE${NC}"

# 检查设备连接
echo -e "${BLUE}📱 检查设备连接...${NC}"
DEVICES=$(adb devices | grep -v "List of devices attached" | grep -v "^$" | wc -l | tr -d ' ')

if [ "$DEVICES" -eq "0" ]; then
    echo -e "${RED}❌ 错误: 未检测到连接的Android设备${NC}"
    echo ""
    echo -e "${YELLOW}📋 请确保:${NC}"
    echo -e "  1. 手机已开启USB调试"
    echo -e "  2. USB数据线连接正常"
    echo -e "  3. 已授权计算机进行USB调试"
    echo -e "  4. 手机处于文件传输(MTP)模式"
    echo ""
    echo -e "${BLUE}💡 排查步骤:${NC}"
    echo -e "  • 设置 → 关于手机 → 连续点击版本号7次"
    echo -e "  • 设置 → 开发者选项 → 启用USB调试"
    echo -e "  • 重新连接USB线缆"
    echo -e "  • 在手机上点击'始终允许此计算机'"
    exit 1
fi

if [ "$DEVICES" -gt "1" ]; then
    echo -e "${YELLOW}⚠️  检测到多个设备，将安装到第一个设备${NC}"
fi

# 显示连接的设备信息
echo -e "${GREEN}✅ 设备连接正常${NC}"
adb devices -l | grep -v "List of devices attached" | while read line; do
    echo -e "${BLUE}📱 设备: $line${NC}"
done

# 检查设备是否已安装旧版本
echo -e "${BLUE}🔍 检查是否存在旧版本...${NC}"
PACKAGE_NAME="com.hippo.ehviewer"
if adb shell pm list packages | grep -q "$PACKAGE_NAME"; then
    echo -e "${YELLOW}⚠️  检测到已安装的EhViewer应用${NC}"
    
    # 获取当前版本信息
    CURRENT_VERSION=$(adb shell dumpsys package $PACKAGE_NAME | grep versionName | head -1 | cut -d'=' -f2)
    echo -e "${BLUE}📋 当前版本: $CURRENT_VERSION${NC}"
    echo -e "${BLUE}📋 新版本: 2.0.0.3${NC}"
    
    echo -e "${YELLOW}🗑️  卸载旧版本...${NC}"
    if adb uninstall "$PACKAGE_NAME" > /dev/null 2>&1; then
        echo -e "${GREEN}✅ 旧版本卸载成功${NC}"
    else
        echo -e "${YELLOW}⚠️  旧版本卸载失败，继续安装新版本${NC}"
    fi
else
    echo -e "${GREEN}✅ 未检测到旧版本，可以直接安装${NC}"
fi

# 开始安装APK
echo -e "${BLUE}🚀 开始安装EhViewer v2.0.0.3 渠道3001...${NC}"
echo -e "${BLUE}   文件: $(basename "$APK_FILE")${NC}"

# 执行安装
if adb install -r "$APK_FILE"; then
    echo ""
    echo -e "${GREEN}🎉 安装成功！${NC}"
    echo -e "${GREEN}=========================================${NC}"
    echo -e "${BLUE}📱 应用信息:${NC}"
    echo -e "   名称: EhViewer (王子公主)"
    echo -e "   版本: v2.0.0.3"
    echo -e "   渠道: 3001 (合作渠道)"
    echo -e "   包名: com.hippo.ehviewer"
    echo ""
    echo -e "${BLUE}🚀 新功能亮点:${NC}"
    echo -e "   ✅ 渠道统计API重试机制"
    echo -e "   ✅ 智能网络异常处理"  
    echo -e "   ✅ 本地请求缓存功能"
    echo -e "   ✅ 频率控制防重复请求"
    echo ""
    echo -e "${BLUE}📋 下一步操作:${NC}"
    echo -e "   1. 在手机上找到并打开EhViewer应用"
    echo -e "   2. 授予存储权限(文件管理功能需要)"
    echo -e "   3. 等待X5内核下载完成(首次启动约30MB)"
    echo -e "   4. 完成初始设置向导"
    echo ""
    echo -e "${GREEN}🎊 安装完成！享受使用EhViewer v2.0.0.3！${NC}"
    
    # 尝试启动应用
    echo -e "${BLUE}🚀 正在启动应用...${NC}"
    if adb shell am start -n "$PACKAGE_NAME/.ui.MainActivity" > /dev/null 2>&1; then
        echo -e "${GREEN}✅ 应用已启动${NC}"
    else
        echo -e "${YELLOW}⚠️  请手动在手机上打开EhViewer应用${NC}"
    fi
    
else
    echo ""
    echo -e "${RED}❌ 安装失败${NC}"
    echo -e "${RED}=========================================${NC}"
    echo -e "${YELLOW}💡 常见解决方案:${NC}"
    echo -e "   • 检查手机存储空间是否充足(需要至少100MB)"
    echo -e "   • 确认已开启'允许安装未知来源应用'"
    echo -e "   • 尝试重启手机后重新安装"
    echo -e "   • 检查APK文件是否损坏"
    echo ""
    echo -e "${BLUE}🔧 手动安装方案:${NC}"
    echo -e "   1. 将APK文件传输到手机"
    echo -e "   2. 在手机文件管理器中找到APK文件"
    echo -e "   3. 点击APK文件进行安装"
    exit 1
fi