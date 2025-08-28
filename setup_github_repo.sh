#!/bin/bash

# ===========================================
# EhViewer 个人GitHub仓库自动配置脚本
# ===========================================

echo "🎯 EhViewer 个人GitHub仓库配置脚本"
echo "======================================"

# 配置信息
REPO_NAME="ehviewer"
GITHUB_USER="loututu114-pixel"
EMAIL="loututu114@gmail.com"

echo "📋 配置信息:"
echo "  用户名: $GITHUB_USER"
echo "  邮箱: $EMAIL"
echo "  仓库名: $REPO_NAME"
echo ""

# 检查Git是否安装
if ! command -v git &> /dev/null; then
    echo "❌ Git未安装，请先安装Git"
    exit 1
fi

# 配置Git用户信息
echo "🔧 配置Git用户信息..."
git config --global user.name "$GITHUB_USER"
git config --global user.email "$EMAIL"

echo "✅ Git配置完成"

# 创建临时目录
TEMP_DIR="/tmp/ehviewer_repo"
echo "📁 创建临时目录: $TEMP_DIR"
rm -rf "$TEMP_DIR"
mkdir -p "$TEMP_DIR"
cd "$TEMP_DIR"

# 初始化Git仓库
echo "📝 初始化Git仓库..."
git init
echo "# EhViewer Updates" > README.md
echo "" >> README.md
echo "EhViewer 个人更新配置文件仓库" >> README.md
echo "" >> README.md
echo "## 配置信息" >> README.md
echo "- 用户: $GITHUB_USER" >> README.md
echo "- 邮箱: $EMAIL" >> README.md
echo "- 更新URL: https://raw.githubusercontent.com/$GITHUB_USER/$REPO_NAME/main/update.json" >> README.md

# 复制update.json文件
if [ -f "/Users/lu/AndroidStudioProjects/EhViewerh/update.json" ]; then
    cp "/Users/lu/AndroidStudioProjects/EhViewerh/update.json" .
    echo "📋 复制update.json配置文件"
else
    echo "⚠️  未找到update.json文件，创建默认配置..."
    cat > update.json << 'EOF'
{
  "version": "1.9.9.18",
  "versionCode": 189918,
  "mustUpdate": false,
  "updateContent": {
    "title": "EhViewer卡通风格新版本",
    "content": [
      "🎨 全新的卡通风格UI设计",
      "📱 完美适配手机、平板等多屏幕设备",
      "🔧 优化详情页样式和图标显示",
      "⚡ 提升应用性能和响应速度",
      "🎯 改进标签显示效果和交互体验"
    ],
    "fileDownloadUrl": "https://github.com/loututu114-pixel/ehviewer/releases/download/v1.9.9.18/EhViewer_CN_SXJ_v1.9.9.18.apk"
  }
}
EOF
fi

# 添加并提交文件
git add .
git commit -m "Initial commit - EhViewer update configuration"

echo ""
echo "🎉 本地仓库初始化完成！"
echo ""
echo "📋 下一步操作："
echo "1. 访问 https://github.com 创建新仓库"
echo "2. 仓库名称: $REPO_NAME"
echo "3. 不要初始化README（因为我们已经创建了）"
echo "4. 创建仓库后，运行以下命令："
echo ""
echo "   cd $TEMP_DIR"
echo "   git remote add origin https://github.com/$GITHUB_USER/$REPO_NAME.git"
echo "   git push -u origin main"
echo ""
echo "5. 验证配置："
echo "   curl https://raw.githubusercontent.com/$GITHUB_USER/$REPO_NAME/main/update.json"
echo ""
echo "📁 临时目录: $TEMP_DIR"
echo "📄 配置文件: $TEMP_DIR/update.json"
echo "📖 说明文档: $TEMP_DIR/README.md"

echo ""
echo "💡 提示："
echo "- 确保GitHub仓库设置为公开（Public）"
echo "- 配置文件会自动处理更新检测"
echo "- 可以通过修改update.json来发布新版本"

echo ""
echo "🎊 准备就绪！请按上述步骤完成GitHub仓库设置。"
