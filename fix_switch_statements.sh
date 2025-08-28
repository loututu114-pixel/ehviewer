#!/bin/bash

# 修复Java switch语句中R.id.xxx常量表达式问题
# 将switch语句转换为if-else语句

echo "开始修复Java switch语句问题..."

# 需要修复的文件列表
files=(
    "app/src/main/java/com/hippo/ehviewer/ui/BlackListActivity.java"
    "app/src/main/java/com/hippo/ehviewer/ui/UConfigActivity.java"
    "app/src/main/java/com/hippo/ehviewer/ui/MainActivity.java"
    "app/src/main/java/com/hippo/ehviewer/ui/FilterActivity.java"
    "app/src/main/java/com/hippo/ehviewer/ui/scene/SelectSiteScene.java"
    "app/src/main/java/com/hippo/ehviewer/ui/scene/gallery/detail/GalleryDetailScene.java"
    "app/src/main/java/com/hippo/ehviewer/ui/scene/gallery/list/BookmarksDraw.java"
    "app/src/main/java/com/hippo/ehviewer/ui/scene/gallery/list/SubscriptionDraw.java"
    "app/src/main/java/com/hippo/ehviewer/ui/scene/GalleryCommentsScene.java"
    "app/src/main/java/com/hippo/ehviewer/ui/scene/GalleryPreviewsScene.java"
    "app/src/main/java/com/hippo/ehviewer/ui/scene/download/DownloadLabelDraw.java"
    "app/src/main/java/com/hippo/ehviewer/ui/scene/download/DownloadsScene.java"
    "app/src/main/java/com/hippo/ehviewer/ui/scene/download/DownloadLabelsScene.java"
    "app/src/main/java/com/hippo/ehviewer/ui/scene/history/HistoryScene.java"
    "app/src/main/java/com/hippo/ehviewer/widget/JumpDateSelector.java"
    "app/src/main/java/com/hippo/ehviewer/widget/SearchLayout.java"
    "app/src/main/java/com/hippo/ehviewer/sync/DownloadListInfosExecutor.java"
)

for file in "${files[@]}"; do
    if [ -f "$file" ]; then
        echo "正在处理: $file"

        # 创建备份
        cp "$file" "${file}.backup"

        # 使用sed命令将switch语句转换为if-else语句
        # 这是一个复杂的转换，需要处理多行内容
        # 这里我们使用一个简单的Python脚本来处理
        python3 -c "
import re

def convert_switch_to_if_else(content):
    # 简单的switch转换逻辑
    # 这是一个简化版本，实际使用时可能需要更复杂的逻辑

    # 查找switch语句
    switch_pattern = r'switch\s*\(\s*(\w+)\s*\)\s*\{([^}]*)\}'
    def replace_switch(match):
        var_name = match.group(1)
        cases = match.group(2)

        # 解析case语句
        case_pattern = r'case\s+([^:]+):\s*([^;]+);'
        cases_list = re.findall(case_pattern, cases)

        if_else_chain = []
        for case_value, case_body in cases_list:
            if case_value.strip() == 'default':
                if_else_chain.append(f'else {case_body.strip()}')
            else:
                condition = f'if ({var_name} == {case_value.strip()})'
                if_else_chain.append(f'{condition} {case_body.strip()}')

        # 将case链转换为if-else链
        result = []
        for i, item in enumerate(if_else_chain):
            if i == 0:
                result.append(item)
            else:
                result.append(f'else {item}')

        return ' '.join(result) + ';'

    return re.sub(switch_pattern, replace_switch, content, flags=re.DOTALL)

# 读取文件
with open('$file', 'r', encoding='utf-8') as f:
    content = f.read()

# 转换switch语句
new_content = convert_switch_to_if_else(content)

# 写入文件
with open('$file', 'w', encoding='utf-8') as f:
    f.write(new_content)

print(f'已处理: $file')
"
    else
        echo "文件不存在: $file"
    fi
done

echo "修复完成！"
