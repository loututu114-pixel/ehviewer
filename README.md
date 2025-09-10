# EhViewer - 台灣版

<div align="center">

![EhViewer Logo](https://img.shields.io/badge/EhViewer-2.0.0.5-blue?style=for-the-badge&logo=android)
![Android](https://img.shields.io/badge/Android-6.0%2B-green?style=for-the-badge&logo=android)
![License](https://img.shields.io/badge/License-Apache%202.0-yellow?style=for-the-badge)
![Stars](https://img.shields.io/github/stars/loututu114-pixel/ehviewer?style=for-the-badge)

**用愛發電，快樂前行** ✨

一個功能強大的Android圖片瀏覽器應用，專為台灣用戶優化

[📱 下載最新版本](https://github.com/loututu114-pixel/ehviewer/releases) • [📖 使用說明](https://github.com/loututu114-pixel/ehviewer/wiki) • [🐛 問題回報](https://github.com/loututu114-pixel/ehviewer/issues)

</div>

---

## 🌟 主要特色

### 🖼️ 專業圖片瀏覽
- **多格式支援**: 支援 JPG、PNG、GIF、WebP 等主流圖片格式
- **高品質渲染**: 採用先進的圖片解碼技術，確保最佳顯示效果
- **智慧快取**: 智慧預載入和磁碟快取管理，提升瀏覽體驗
- **批次下載**: 支援斷點續傳和背景下載功能

### 🌐 內建瀏覽器引擎
- **雙核心支援**: 騰訊X5 WebView + 原生WebView智慧降級
- **完整瀏覽功能**: 網址列、書籤管理、歷史記錄、多分頁瀏覽
- **使用者腳本系統**: 20+ 預設增強腳本，Tampermonkey 相容
- **智慧下載**: 整合下載管理員，支援斷點續傳
- **檔案關聯**: APK 安裝、多媒體播放、文件檢視

### 📂 檔案管理器
- **完整功能**: 檔案瀏覽、複製、移動、刪除、搜尋
- **多媒體支援**: 圖片檢視、影片播放、音訊播放
- **文件預覽**: PDF、Office 文件線上檢視
- **壓縮檔支援**: 7Zip 直接瀏覽和提取
- **APK 安裝器**: 一鍵安裝 Android 應用程式
- **雲端儲存**: 支援外部儲存和網路位置

### 🎯 畫廊系統
- **多源瀏覽**: 網路畫廊、本機壓縮檔、目錄瀏覽
- **智慧快取**: 圖片預載入和磁碟快取管理
- **批次下載**: 支援斷點續傳和背景下載
- **標籤系統**: 使用者標籤、分類過濾、黑名單
- **收藏同步**: 本機收藏和雲端同步

---

## 📱 系統需求

| 項目 | 需求 |
|------|------|
| **Android 版本** | 6.0 (API 23) 以上 |
| **RAM** | 建議 2GB 以上 |
| **儲存空間** | 100MB 以上可用空間 |
| **權限** | 儲存權限（檔案管理功能必需） |
| **網路** | 首次使用需要網路下載瀏覽器核心 |

---

## 🚀 快速開始

### 下載安裝

1. **下載 APK**: 前往 [Releases 頁面](https://github.com/loututu114-pixel/ehviewer/releases) 下載最新版本
2. **啟用權限**: 設定 → 安全性 → 允許安裝未知應用程式
3. **安裝應用程式**: 點擊 APK 檔案，按照提示完成安裝
4. **初始化**: 首次啟動會下載騰訊X5核心（約30MB）
5. **權限授予**: 授予儲存權限以使用檔案管理功能

### 渠道選擇

| 渠道 | 名稱 | 適用對象 | 說明 |
|------|------|----------|------|
| **0000** | 預設版本 | 一般使用者 | 官方正式發布版本，推薦一般使用者使用 |
| **3001** | 合作渠道 | 渠道分發 | 特定渠道發布版本，包含渠道統計功能 |

---

## 🛠️ 技術規格

### 編譯環境
- **編譯 SDK**: Android 35
- **目標 SDK**: Android 34
- **最小 SDK**: Android 23
- **Java 版本**: JDK 17
- **Gradle**: 8.x + Android Gradle Plugin

### 支援架構
- **ARM64**: arm64-v8a (主要)
- **ARM32**: armeabi-v7a (相容)
- **x86_64**: x86_64 (模擬器)
- **x86**: x86 (相容)

### 核心依賴
```kotlin
// 網路和解析
implementation("com.squareup.okhttp3:okhttp:3.14.7")
implementation("org.jsoup:jsoup:1.15.3")
implementation("com.alibaba:fastjson:1.2.83")

// 瀏覽器核心
implementation("com.tencent.tbs:tbssdk:44286")

// 資料庫
implementation("org.greenrobot:greendao:3.0.0")

// 分析統計
implementation("com.google.firebase:firebase-analytics:22.4.0")
implementation("com.google.firebase:firebase-crashlytics:19.4.2")
```

---

## 📊 版本歷史

### v2.0.0.5 (最新版本)
- ✅ **新增**: 渠道統計 API 重試機制和頻率控制
- ✅ **優化**: 網路逾時時間和異常處理
- ✅ **修復**: 相依性衝突和建置問題
- ✅ **增強**: 統計資料的可靠性和準確性

### v2.0.0.3
- 🔄 **改進**: 渠道統計 API 重試機制
- 📊 **優化**: 統計功能增強
- 🛡️ **提升**: 穩定性改進

### v2.0.0.1
- 🎯 **發布**: 基礎功能版本
- 🌐 **內建**: 瀏覽器引擎和使用者腳本系統
- 📂 **整合**: 檔案管理和多媒體支援
- 📊 **新增**: 基礎渠道統計功能

---

## 🔧 建置說明

### Windows
```bash
git clone https://github.com/loututu114-pixel/ehviewer.git
cd EhViewer
gradlew app:assembleDebug
```

### Linux/macOS
```bash
git clone https://github.com/loututu114-pixel/ehviewer.git
cd EhViewer
./gradlew app:assembleDebug
```

生成的 APK 檔案位於 `app/build/outputs/apk` 目錄下

---

## 🤝 參與貢獻

我們歡迎各種形式的貢獻！

### 如何貢獻
- **程式碼貢獻**: 歡迎提交 Pull Request
- **問題回報**: 透過 GitHub Issues 回報問題
- **功能建議**: 在 Discussion 區域提出建議
- **翻譯協助**: 協助完善多語言支援

### 開發指南
1. Fork 本專案
2. 建立功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交變更 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 開啟 Pull Request

---

## 📞 支援與聯絡

### 獲得協助
- **使用者手冊**: 應用程式內建說明文件
- **線上支援**: 透過應用程式內回饋功能
- **社群討論**: GitHub Issues 和討論區
- **更新通知**: 關注專案 GitHub 倉庫

### 聯絡資訊
- **專案首頁**: [GitHub Repository](https://github.com/loututu114-pixel/ehviewer)
- **問題回報**: [GitHub Issues](https://github.com/loututu114-pixel/ehviewer/issues)
- **技術支援**: 透過 GitHub Issues
- **商務合作**: 透過專案 README 聯絡方式

---

## 📋 授權條款

本專案採用 [Apache License 2.0](LICENSE) 授權條款。

---

## 🙏 致謝

### 感謝名單
感謝 Ehviewer 奠基人 Hippo/seven332  
Thanks to Hippo/seven332, the founder of Ehviewer

本專案受到了諸多開源專案的幫助  
This project has received help from many open source projects

### 開源函式庫
- AOSP
- android-advancedrecyclerview
- Apache Commons Lang
- apng
- giflib
- greenDAO
- jsoup
- libjpeg-turbo
- libpng
- okhttp
- roaster
- ShowcaseView
- Slabo
- TagSoup

---

## ⚠️ 免責聲明

本應用程式僅供學習和研究使用。使用者應遵守當地法律法規，開發者不對使用者的任何行為負責。

---

<div align="center">

**EhViewer 台灣版** - 用愛發電，快樂前行 ✨

[⬆ 回到頂部](#ehviewer---台灣版)

</div>
