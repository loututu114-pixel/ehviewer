#!/bin/bash

# Google Analytics 配置更新脚本
# 用于更新 王子的公主 应用的 Measurement ID

echo "==============================================="
echo "   王子的公主 - Google Analytics 配置工具"
echo "==============================================="

# 提示用户输入 Measurement ID
echo ""
echo "请从 Firebase 控制台获取您的 Google Analytics Measurement ID"
echo "格式类似: G-XXXXXXXXXX"
echo ""
read -p "请输入您的 Measurement ID: " MEASUREMENT_ID

# 验证输入格式
if [[ $MEASUREMENT_ID =~ ^G-[A-Z0-9]{9,}$ ]]; then
    echo "✓ Measurement ID 格式正确"
else
    echo "✗ Measurement ID 格式不正确，请检查后重新输入"
    echo "正确的格式应该是: G-XXXXXXXXXX"
    exit 1
fi

# 更新配置文件
sed -i.bak "s/G-XXXXXXXXXX/$MEASUREMENT_ID/g" app/src/main/res/values/analytics.xml

echo ""
echo "✅ 配置已更新！"
echo "Measurement ID: $MEASUREMENT_ID"
echo ""
echo "接下来请运行以下命令重新构建应用:"
echo "  ./gradlew assembleAppReleaseDebug"
echo ""
echo "构建完成后，您可以测试统计功能了！"
echo ""
echo "==============================================="
