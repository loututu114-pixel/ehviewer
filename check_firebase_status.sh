#!/bin/bash

# Firebase Analytics 配置状态检查工具
# 用于诊断 Measurement ID 获取问题

echo "============================================"
echo "   Firebase Analytics 配置状态检查"
echo "============================================"

# 检查 google-services.json 文件
echo ""
echo "📁 检查 google-services.json 文件..."
if [ -f "google-services.json" ]; then
    echo "✅ google-services.json 文件存在"

    # 检查是否包含 analytics_service
    if grep -q "analytics_service" google-services.json; then
        echo "✅ 包含 Google Analytics 配置"

        # 提取项目信息
        PROJECT_ID=$(grep -o '"project_id": "[^"]*' google-services.json | cut -d'"' -f4)
        PACKAGE_NAME=$(grep -o '"package_name": "[^"]*' google-services.json | cut -d'"' -f4)

        echo "📋 项目信息:"
        echo "   项目ID: $PROJECT_ID"
        echo "   包名: $PACKAGE_NAME"

        # 检查是否有 measurement_id
        if grep -q "measurement_id" google-services.json; then
            MEASUREMENT_ID=$(grep -o '"measurement_id": "[^"]*' google-services.json | cut -d'"' -f4)
            echo "✅ Measurement ID 存在: $MEASUREMENT_ID"
        else
            echo "⚠️  Measurement ID 不存在（需要启用 Google Analytics）"
        fi

    else
        echo "❌ 未找到 Google Analytics 配置"
        echo "   您需要在 Firebase 控制台中启用 Google Analytics"
    fi
else
    echo "❌ google-services.json 文件不存在"
    echo "   请从 Firebase 控制台下载配置文件"
fi

# 检查应用配置
echo ""
echo "📱 检查应用配置..."
if [ -f "app/src/main/res/values/analytics.xml" ]; then
    echo "✅ analytics.xml 配置文件存在"

    CURRENT_ID=$(grep -o 'G-[A-Z0-9]*' app/src/main/res/values/analytics.xml)
    if [ -n "$CURRENT_ID" ]; then
        echo "📋 当前配置的 Measurement ID: $CURRENT_ID"
    else
        echo "⚠️  未找到 Measurement ID 配置"
    fi
else
    echo "❌ analytics.xml 配置文件不存在"
fi

echo ""
echo "🔧 推荐操作步骤:"
echo ""

if ! grep -q "analytics_service" google-services.json 2>/dev/null; then
    echo "1. 访问 Firebase 控制台: https://console.firebase.google.com/"
    echo "2. 选择项目: ehviewer-1f7d7"
    echo "3. 点击 'Analytics' 启用 Google Analytics"
    echo "4. 创建 Analytics 账户"
    echo "5. 重新下载 google-services.json"
    echo ""
fi

echo "📊 获取 Measurement ID 的位置:"
echo "• Firebase 控制台 → Analytics → 管理 → 数据流"
echo "• 项目设置 → 集成 → Google Analytics → 管理"
echo "• Google Analytics 控制台 → 管理 → 数据流"
echo ""

echo "🛠️  更新配置的工具:"
echo "• 运行: ./update_measurement_id.sh G-YOUR-ID"
echo "• 然后重新构建: ./gradlew assembleAppReleaseDebug"
echo ""

echo "============================================"
