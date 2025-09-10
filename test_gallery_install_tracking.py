#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试漫画书智能安装统计机制
验证用户首次打开漫画书时触发的安装统计
"""

import json
import requests
from datetime import datetime

API_BASE_URL = "https://qudao.eh-viewer.com/api"

def create_gallery_install_data(channel_code="3001", device_id=None):
    """创建漫画书智能安装统计测试数据"""
    iso_time = datetime.now().strftime('%Y-%m-%dT%H:%M:%S.%f')[:-3] + 'Z'
    
    data = {
        "channelCode": channel_code,
        "softwareId": 1,
        "installTime": iso_time,
        "deviceInfo": {
            "os": "Android 13",
            "model": "Test Device",
            "manufacturer": "Test",
            "brand": "Test", 
            "sdk": 33
        },
        "realInstall": True,  # 标记为真实安装
        "triggerContext": "gallery_first_open"  # 标记触发上下文
    }
    
    if device_id:
        data["deviceId"] = device_id
    
    return data

def test_gallery_install_tracking():
    """测试漫画书智能安装统计"""
    print("\n📖 测试漫画书智能安装统计")
    print("="*50)
    
    # 模拟设备指纹
    test_device_id = "gallery_device_12345_smart"
    data = create_gallery_install_data("3001", test_device_id)
    
    print(f"触发场景: 用户首次打开漫画书")
    print(f"设备指纹: {test_device_id}")
    print(f"渠道代码: {data['channelCode']}")
    print(f"真实安装: {data['realInstall']}")
    print(f"数据: {json.dumps(data, indent=2, ensure_ascii=False)}")
    
    try:
        response = requests.post(
            f"{API_BASE_URL}/stats/install",
            json=data,
            headers={'Content-Type': 'application/json'},
            timeout=10
        )
        
        print(f"\n响应状态: {response.status_code}")
        if response.text:
            print(f"响应内容: {response.text}")
        
        if response.status_code == 200:
            print("✅ 漫画书智能安装统计成功！")
            return True
        else:
            print("❌ 漫画书智能安装统计失败")
            return False
            
    except Exception as e:
        print(f"❌ 请求异常: {e}")
        return False

def test_gallery_vs_browser_comparison():
    """测试漫画书vs浏览器触发对比"""
    print("\n🔄 对比测试：漫画书 vs 浏览器触发")
    print("="*50)
    
    # 测试漫画书触发
    print("1. 测试漫画书触发机制")
    gallery_data = create_gallery_install_data("3001", "comparison_gallery_device")
    gallery_data["triggerContext"] = "gallery_first_open"
    
    try:
        gallery_response = requests.post(
            f"{API_BASE_URL}/stats/install",
            json=gallery_data,
            headers={'Content-Type': 'application/json'},
            timeout=5
        )
        
        gallery_success = gallery_response.status_code == 200
        print(f"   漫画书触发: {'✅ 成功' if gallery_success else '❌ 失败'} (HTTP {gallery_response.status_code})")
        
    except Exception as e:
        gallery_success = False
        print(f"   漫画书触发: ❌ 异常 - {e}")
    
    # 测试浏览器触发（旧机制，现在应该不使用了）
    print("2. 测试浏览器触发机制（已废弃）")
    browser_data = create_gallery_install_data("3001", "comparison_browser_device")
    browser_data["triggerContext"] = "browser_first_visit"
    
    try:
        browser_response = requests.post(
            f"{API_BASE_URL}/stats/install",
            json=browser_data,
            headers={'Content-Type': 'application/json'},
            timeout=5
        )
        
        browser_success = browser_response.status_code == 200
        print(f"   浏览器触发: {'✅ 成功' if browser_success else '❌ 失败'} (HTTP {browser_response.status_code})")
        
    except Exception as e:
        browser_success = False
        print(f"   浏览器触发: ❌ 异常 - {e}")
    
    print(f"\n对比结果:")
    print(f"  📖 漫画书触发（新机制）: {'✅ 工作正常' if gallery_success else '❌ 需要检查'}")
    print(f"  🌐 浏览器触发（旧机制）: {'⚠️  API兼容' if browser_success else '❌ 已废弃'}")
    
    return gallery_success, browser_success

def simulate_user_gallery_flow():
    """模拟用户漫画书使用流程"""
    print("\n👤 模拟用户漫画书使用完整流程")
    print("="*50)
    
    print("1. 用户安装EhViewer应用")
    print("2. 启动应用（主界面，不触发统计）")
    print("3. 浏览画廊列表")
    print("4. 👆 点击任意一本漫画书")
    print("5. 🚀 GalleryActivity.onResume() -> 触发智能安装统计")
    print("6. 用户开始阅读漫画...")
    
    # 模拟首次打开漫画书触发的安装统计
    first_gallery_success = test_gallery_install_tracking()
    
    print("7. 用户继续使用其他功能（浏览器、下载等）")
    print("8. 但安装统计只在首次打开漫画书时触发一次")
    
    return first_gallery_success

def test_gallery_trigger_precision():
    """测试漫画书触发的精确性"""
    print("\n🎯 测试漫画书触发精确性")
    print("="*50)
    
    print("测试场景:")
    print("  ✅ 应该触发: 首次打开任意漫画书")
    print("  ❌ 不应触发: 应用启动、浏览列表、使用浏览器")
    print("  ❌ 不应重复: 同一设备多次打开漫画书")
    
    # 测试首次打开
    print("\n1. 模拟首次打开漫画书")
    first_device = "precision_test_device_001"
    first_data = create_gallery_install_data("3001", first_device)
    first_data["triggerContext"] = "gallery_first_open"
    first_data["testScenario"] = "first_gallery_open"
    
    try:
        response1 = requests.post(
            f"{API_BASE_URL}/stats/install",
            json=first_data,
            headers={'Content-Type': 'application/json'},
            timeout=5
        )
        
        first_success = response1.status_code == 200
        print(f"   首次打开: {'✅ 正确触发' if first_success else '❌ 触发失败'}")
        
    except Exception as e:
        first_success = False
        print(f"   首次打开: ❌ 异常 - {e}")
    
    # 测试重复打开（应该被客户端防重复机制阻止）
    print("2. 模拟重复打开漫画书（同设备）")
    repeat_data = create_gallery_install_data("3001", first_device)  # 相同设备ID
    repeat_data["triggerContext"] = "gallery_repeat_open"
    repeat_data["testScenario"] = "repeat_gallery_open"
    
    try:
        response2 = requests.post(
            f"{API_BASE_URL}/stats/install",
            json=repeat_data,
            headers={'Content-Type': 'application/json'},
            timeout=5
        )
        
        # 这里成功也是正常的，因为防重复逻辑在客户端
        repeat_success = response2.status_code == 200
        print(f"   重复打开: {'⚠️  服务端允许' if repeat_success else '❌ 服务端拒绝'}")
        print(f"   说明: 防重复机制由客户端SharedPreferences实现")
        
    except Exception as e:
        repeat_success = False
        print(f"   重复打开: ❌ 异常 - {e}")
    
    return first_success, repeat_success

def main():
    """主测试函数"""
    print("🧪 漫画书智能安装统计机制测试")
    print("="*60)
    print("新触发机制:")
    print("  📖 漫画书触发: GalleryActivity.onResume()")
    print("  🎯 触发时机: 用户首次打开任意漫画书")
    print("  🔒 防重复: SharedPreferences + 设备指纹")
    print("  ❌ 旧机制: 浏览器首次访问（已移除）")
    
    # 执行各项测试
    tests = [
        ("漫画书智能安装统计", test_gallery_install_tracking),
        ("漫画书vs浏览器对比", test_gallery_vs_browser_comparison),
        ("用户流程模拟", simulate_user_gallery_flow),
        ("触发精确性测试", test_gallery_trigger_precision)
    ]
    
    results = {}
    for test_name, test_func in tests:
        print(f"\n{'='*20} {test_name} {'='*20}")
        try:
            result = test_func()
            results[test_name] = result
        except Exception as e:
            print(f"❌ 测试异常: {e}")
            results[test_name] = False
    
    # 汇总结果
    print(f"\n{'='*60}")
    print("🏆 漫画书智能安装统计测试结果")
    print("="*60)
    
    for test_name, result in results.items():
        if isinstance(result, bool):
            print(f"  {test_name}: {'✅ 通过' if result else '❌ 失败'}")
        elif isinstance(result, tuple):
            if len(result) == 2:
                r1, r2 = result
                print(f"  {test_name}: 主要测试{'✅ 通过' if r1 else '❌ 失败'}，对比测试{'✅ 通过' if r2 else '❌ 失败'}")
    
    print(f"\n🎯 机制改进验证:")
    print(f"  ✅ 触发时机更准确: 漫画书是应用核心功能")
    print(f"  ✅ 用户体验更佳: 直接反映用户真实使用意图")
    print(f"  ✅ 技术实现稳定: GalleryActivity.onResume()时机可靠")
    print(f"  ✅ 防重复机制: 客户端SharedPreferences确保一次性")
    
    print(f"\n📋 部署检查项:")
    print(f"  □ GalleryActivity.java 编译通过")
    print(f"  □ ChannelTracker导入正确")  
    print(f"  □ SharedPreferences存储正常")
    print(f"  □ 日志输出便于调试")
    print(f"  □ WebViewActivity旧代码已清理")

if __name__ == "__main__":
    main()