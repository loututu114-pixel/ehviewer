#!/bin/bash
# EhViewerh 项目安全检查脚本
# 版本: v1.0 | 创建时间: 2025-09-10

set -e  # 遇到错误立即退出

echo "🔍 EhViewerh 安全检查开始..."
echo "===================================="

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 计数器
ERRORS=0
WARNINGS=0

# 检查函数
check_error() {
    echo -e "${RED}❌ $1${NC}"
    ERRORS=$((ERRORS + 1))
}

check_warning() {
    echo -e "${YELLOW}⚠️ $1${NC}"
    WARNINGS=$((WARNINGS + 1))
}

check_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

echo "1. 检查签名文件是否被 Git 跟踪..."
if git ls-files | grep -qE "\.(keystore|jks)$"; then
    check_error "发现签名文件被跟踪！这是严重的安全风险！"
    echo "   被跟踪的签名文件："
    git ls-files | grep -E "\.(keystore|jks)$" | sed 's/^/   - /'
else
    check_success "签名文件未被跟踪"
fi

echo ""
echo "2. 检查配置文件是否包含明文密码..."
if git ls-files | xargs grep -l "EhViewer2025!" 2>/dev/null; then
    check_error "发现明文密码！"
    echo "   包含明文密码的文件："
    git ls-files | xargs grep -l "EhViewer2025!" 2>/dev/null | sed 's/^/   - /'
else
    check_success "未发现明文密码"
fi

echo ""
echo "3. 检查 Firebase 配置文件..."
if git ls-files | grep -q "google-services.json"; then
    check_error "发现 Firebase 配置文件被跟踪！"
    echo "   被跟踪的文件："
    git ls-files | grep "google-services.json" | sed 's/^/   - /'
else
    check_success "Firebase 配置文件未被跟踪"
fi

echo ""
echo "4. 检查 gradle.properties 文件..."
if git ls-files | grep -qE "gradle\.properties$"; then
    check_error "发现 gradle.properties 被跟踪！可能包含敏感信息"
    echo "   被跟踪的文件："
    git ls-files | grep -E "gradle\.properties$" | sed 's/^/   - /'
else
    check_success "gradle.properties 未被跟踪"
fi

echo ""
echo "5. 检查 APK/AAB 文件..."
APK_FILES=$(git ls-files | grep -E "\.(apk|aab)$" | wc -l)
if [ "$APK_FILES" -gt 0 ]; then
    check_warning "发现 $APK_FILES 个编译产物被跟踪（占用存储空间）"
    echo "   被跟踪的文件："
    git ls-files | grep -E "\.(apk|aab)$" | sed 's/^/   - /'
else
    check_success "无编译产物被跟踪"
fi

echo ""
echo "6. 检查日志和临时文件..."
TEMP_FILES=$(git ls-files | grep -E "\.(log|tmp|temp|bak)$" | wc -l)
if [ "$TEMP_FILES" -gt 0 ]; then
    check_warning "发现 $TEMP_FILES 个临时文件被跟踪"
    echo "   被跟踪的文件："
    git ls-files | grep -E "\.(log|tmp|temp|bak)$" | sed 's/^/   - /'
else
    check_success "无临时文件被跟踪"
fi

echo ""
echo "7. 检查 .gitignore 配置..."
if [ -f ".gitignore" ]; then
    if grep -q "*.keystore" .gitignore && grep -q "google-services.json" .gitignore; then
        check_success ".gitignore 配置正确"
    else
        check_error ".gitignore 配置不完整，缺少关键安全规则"
    fi
else
    check_error ".gitignore 文件不存在"
fi

echo ""
echo "8. 检查商业渠道信息..."
# 排除核心功能代码，只检查配置和文档文件
CHANNEL_SENSITIVE=$(git ls-files | grep -iE "(channel|partner|distribution)" | grep -vE "\.java$|\.kt$" | wc -l)
if [ "$CHANNEL_SENSITIVE" -gt 0 ]; then
    check_error "发现 $CHANNEL_SENSITIVE 个渠道配置/文档被跟踪！商业信息可能泄露"
    echo "   被跟踪的商业敏感文件："
    git ls-files | grep -iE "(channel|partner|distribution)" | grep -vE "\.java$|\.kt$" | head -5 | sed 's/^/   - /'
    if [ "$CHANNEL_SENSITIVE" -gt 5 ]; then
        echo "   ... 还有 $((CHANNEL_SENSITIVE - 5)) 个文件"
    fi
else
    check_success "无商业渠道配置被跟踪"
fi

# 检查源码中是否包含具体渠道号
echo ""
echo "8.1 检查源码中的具体渠道号..."
if git ls-files | xargs grep -l "3001\|2001\|1001\|9999" 2>/dev/null | grep -qE "\.(java|kt)$"; then
    check_warning "发现源码中包含具体渠道号"
    echo "   包含渠道号的源码文件："
    git ls-files | xargs grep -l "3001\|2001\|1001\|9999" 2>/dev/null | grep -E "\.(java|kt)$" | sed 's/^/   - /'
else
    check_success "源码中未发现具体渠道号"
fi

echo ""
echo "9. 检查渠道构建脚本..."
if git ls-files | grep -qE "build.*channel.*\.sh$"; then
    check_error "发现渠道构建脚本被跟踪！"
    echo "   被跟踪的脚本："
    git ls-files | grep -E "build.*channel.*\.sh$" | sed 's/^/   - /'
else
    check_success "渠道构建脚本未被跟踪"
fi

echo ""
echo "10. 检查模板文件..."
if [ -f "gradle.properties.template" ]; then
    check_success "gradle.properties.template 存在"
else
    check_warning "缺少 gradle.properties.template 文件"
fi

echo ""
echo "===================================="
echo "🔍 安全检查完成"
echo ""

# 输出结果
if [ "$ERRORS" -eq 0 ] && [ "$WARNINGS" -eq 0 ]; then
    echo -e "${GREEN}🎉 恭喜！未发现任何安全问题！${NC}"
    exit 0
elif [ "$ERRORS" -eq 0 ]; then
    echo -e "${YELLOW}⚠️ 发现 $WARNINGS 个警告，建议处理${NC}"
    exit 0
else
    echo -e "${RED}💥 发现 $ERRORS 个严重安全问题，必须立即处理！${NC}"
    if [ "$WARNINGS" -gt 0 ]; then
        echo -e "${YELLOW}   同时还有 $WARNINGS 个警告${NC}"
    fi
    echo ""
    echo "🔧 修复建议："
    echo "   1. 运行 'git rm --cached <敏感文件>' 从跟踪中移除"
    echo "   2. 确认 .gitignore 配置正确"
    echo "   3. 使用模板文件重新配置敏感信息"
    echo "   4. 参考 GIT_VERSION_CONTROL_GUIDE.md"
    exit 1
fi