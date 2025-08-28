#!/bin/bash

# 更新 Measurement ID 的脚本
# 用法: ./update_measurement_id.sh G-YOUR-MEASUREMENT-ID

if [ $# -eq 0 ]; then
    echo "用法: $0 <Measurement ID>"
    echo "例如: $0 G-ABCDEFGHIJ"
    exit 1
fi

MEASUREMENT_ID=$1

# 验证格式
if [[ ! $MEASUREMENT_ID =~ ^G-[A-Z0-9]{9,}$ ]]; then
    echo "❌ Measurement ID 格式不正确"
    echo "正确的格式应该是: G-XXXXXXXXXX (G-后面跟着9位以上的字母数字)"
    exit 1
fi

# 更新配置文件
echo "🔄 正在更新 Measurement ID..."
sed -i.bak "s/G-TEST123456/$MEASUREMENT_ID/g" app/src/main/res/values/analytics.xml

if [ $? -eq 0 ]; then
    echo "✅ Measurement ID 已更新为: $MEASUREMENT_ID"
    echo ""
    echo "📱 下一步:"
    echo "   ./gradlew assembleAppReleaseDebug"
    echo ""
    echo "🎉 然后您的应用就可以收集真实的统计数据了！"
else
    echo "❌ 更新失败"
    exit 1
fi
