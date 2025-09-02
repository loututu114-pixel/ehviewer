#!/bin/bash

# EhViewer 远程配置测试脚本
# 用于验证 sou.json 配置文件是否能正常从 GitHub 下载

echo "=== EhViewer 远程配置测试 ==="
echo ""

# 配置 URL
CONFIG_URL="https://raw.githubusercontent.com/loututu114-pixel/ehviewer/main/sou.json"
EXAMPLE_CONFIG_URL="https://raw.githubusercontent.com/loututu114-pixel/ehviewer/main/sou_example.json"

echo "1. 测试主配置文件连接..."
echo "URL: $CONFIG_URL"
echo ""

# 测试连接
echo "测试 HTTP 响应..."
curl -s -I "$CONFIG_URL" | head -3
echo ""

# 下载并验证 JSON 格式
echo "2. 下载并验证配置文件..."
echo "正在下载配置..."
CONFIG_CONTENT=$(curl -s "$CONFIG_URL")

if [ $? -eq 0 ] && [ ! -z "$CONFIG_CONTENT" ]; then
    echo "✅ 配置下载成功"
    echo "📏 配置大小: $(echo "$CONFIG_CONTENT" | wc -c) 字节"

    # 验证 JSON 格式
    if echo "$CONFIG_CONTENT" | python3 -m json.tool > /dev/null 2>&1; then
        echo "✅ JSON 格式验证通过"
    else
        echo "❌ JSON 格式验证失败"
        exit 1
    fi

    # 提取关键配置信息
    echo ""
    echo "3. 配置内容摘要:"
    echo "$CONFIG_CONTENT" | python3 -c "
import sys, json
try:
    config = json.load(sys.stdin)
    print('📋 版本:', config.get('version', '未知'))
    print('📅 更新时间:', config.get('lastUpdate', '未知'))

    search_config = config.get('searchConfig', {})
    homepage = search_config.get('homepage', {})
    if homepage.get('enabled', False):
        print('🏠 默认首页启用')
        print('   🌐 国际:', homepage.get('url', '未设置'))
        print('   🇨🇳 中国:', homepage.get('china', '未设置'))

        custom_options = homepage.get('customOptions', [])
        if custom_options:
            print('   📋 自定义选项数量:', len(custom_options))
            for i, option in enumerate(custom_options[:3]):  # 只显示前3个
                print(f'      {i+1}. {option.get(\"name\", \"未命名\")}')

    engines = search_config.get('engines', {})
    print('🔍 搜索引擎数量:', len(engines))
    for engine_id, engine in list(engines.items())[:3]:  # 只显示前3个
        print(f'   • {engine.get(\"name\", engine_id)}')

    print('')
    print('4. 国家映射:')
    country_mapping = search_config.get('countryMapping', {})
    for country, region in list(country_mapping.items())[:5]:  # 只显示前5个
        print(f'   {country} -> {region}')

except Exception as e:
    print('❌ 解析配置失败:', str(e))
"
else
    echo "❌ 配置下载失败"
    echo "请检查网络连接和 GitHub 访问权限"
    exit 1
fi

echo ""
echo "5. 测试示例配置文件..."
EXAMPLE_CONTENT=$(curl -s "$EXAMPLE_CONFIG_URL")

if [ $? -eq 0 ] && [ ! -z "$EXAMPLE_CONTENT" ]; then
    echo "✅ 示例配置下载成功"
    echo "📏 示例配置大小: $(echo "$EXAMPLE_CONTENT" | wc -c) 字节"

    if echo "$EXAMPLE_CONTENT" | python3 -m json.tool > /dev/null 2>&1; then
        echo "✅ 示例配置 JSON 格式验证通过"
    else
        echo "❌ 示例配置 JSON 格式验证失败"
    fi
else
    echo "❌ 示例配置下载失败"
fi

echo ""
echo "=== 测试完成 ==="
echo ""
echo "📝 使用说明:"
echo "1. 配置文件已上传到 GitHub"
echo "2. 应用会自动下载并缓存配置"
echo "3. 网络不可用时使用本地缓存"
echo "4. 修改配置后提交到 GitHub 即可实时生效"
echo ""
echo "🔧 故障排除:"
echo "- 如果下载失败，检查网络连接"
echo "- 如果 JSON 格式错误，验证配置文件语法"
echo "- 如果配置未生效，等待24小时或强制重启应用"
