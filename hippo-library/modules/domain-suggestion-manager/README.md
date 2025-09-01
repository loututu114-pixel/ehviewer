# 💡 域名建议管理器模块 (Domain Suggestion Manager Module)

## 🎯 概述

Android Library域名建议管理器提供智能的域名补全和建议功能，支持协议补全、域名别名、历史记录等多种建议类型。

## ✨ 主要特性

- ✅ **协议补全**: 智能补全HTTP/HTTPS等协议
- ✅ **域名别名**: 支持常用网站的缩写输入
- ✅ **历史建议**: 基于用户历史的个性化建议
- ✅ **实时过滤**: 输入时实时提供相关建议
- ✅ **学习优化**: 根据使用频率优化建议顺序

## 🚀 快速开始

```java
// 初始化域名建议管理器
DomainSuggestionManager.initialize(context);

// 获取建议列表
String partialInput = "git";
DomainSuggestionManager.getInstance().getSuggestions(partialInput,
    new SuggestionCallback() {
        @Override
        public void onSuggestions(List<SuggestionItem> suggestions) {
            for (SuggestionItem item : suggestions) {
                Log.d(TAG, "Suggestion: " + item.getText());
            }
        }
    });

// 添加自定义别名
DomainSuggestionManager.getInstance().addAlias("yt", "https://youtube.com");
```

## 📄 许可证

本项目采用 Apache License 2.0 许可证 - 查看 [LICENSE](../LICENSE) 文件了解详情
