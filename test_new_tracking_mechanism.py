#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试新的渠道统计机制
验证激活统计（5次访问）和下载统计的API调用
"""

import json
import requests
from datetime import datetime

API_BASE_URL = "https://qudao.eh-viewer.com/api"

def create_test_data(event_type, channel_code="3001", license_key=None):
    """创建测试数据"""
    iso_time = datetime.now().strftime('%Y-%m-%dT%H:%M:%S.%f')[:-3] + 'Z'
    
    base_data = {
        "channelCode": channel_code,
        "softwareId": 1,
        "deviceInfo": {
            "os": "Android 13",
            "model": "Test Device",
            "manufacturer": "Test",
            "brand": "Test", 
            "sdk": 33
        }
    }
    
    if event_type == "activate":
        base_data["activateTime"] = iso_time
        if license_key:
            base_data["licenseKey"] = license_key
    elif event_type == "download":
        base_data["downloadTime"] = iso_time
    
    return base_data

def test_activation_tracking():
    """测试激活统计 - 5次访问后触发"""
    print("\n🎯 测试激活统计机制")
    print("="*50)
    
    # 模拟浏览器激活（5次访问）
    activation_key = "browser_activation_5_visits"
    data = create_test_data("activate", "3001", activation_key)
    
    print(f"激活密钥: {activation_key}")
    print(f"渠道代码: {data['channelCode']}")
    print(f"数据: {json.dumps(data, indent=2, ensure_ascii=False)}")
    
    try:
        response = requests.post(
            f"{API_BASE_URL}/stats/activate",
            json=data,
            headers={'Content-Type': 'application/json'},
            timeout=10
        )
        
        print(f"\n响应状态: {response.status_code}")
        if response.text:
            print(f"响应内容: {response.text}")
        
        if response.status_code == 200:
            print("✅ 激活统计成功！")
            return True
        else:
            print("❌ 激活统计失败")
            return False
            
    except Exception as e:
        print(f"❌ 请求异常: {e}")
        return False

def test_download_tracking():
    """测试下载统计"""
    print("\n📥 测试下载统计机制")
    print("="*50)
    
    data = create_test_data("download", "3001")
    
    print(f"渠道代码: {data['channelCode']}")
    print(f"数据: {json.dumps(data, indent=2, ensure_ascii=False)}")
    
    try:
        response = requests.post(
            f"{API_BASE_URL}/stats/download",
            json=data,
            headers={'Content-Type': 'application/json'},
            timeout=10
        )
        
        print(f"\n响应状态: {response.status_code}")
        if response.text:
            print(f"响应内容: {response.text}")
        
        if response.status_code == 200:
            print("✅ 下载统计成功！")
            return True
        else:
            print("❌ 下载统计失败")
            return False
            
    except Exception as e:
        print(f"❌ 请求异常: {e}")
        return False

def test_multiple_channels():
    """测试多个渠道的统计"""
    print("\n🔄 测试多渠道统计")
    print("="*50)
    
    channels = ["0000", "3001", "3002", "3003"]
    results = {}
    
    for channel in channels:
        print(f"\n测试渠道: {channel}")
        data = create_test_data("activate", channel, f"test_activation_{channel}")
        
        try:
            response = requests.post(
                f"{API_BASE_URL}/stats/activate",
                json=data,
                headers={'Content-Type': 'application/json'},
                timeout=5
            )
            
            success = response.status_code == 200
            results[channel] = success
            print(f"渠道 {channel}: {'✅ 成功' if success else '❌ 失败'} (HTTP {response.status_code})")
            
        except Exception as e:
            results[channel] = False
            print(f"渠道 {channel}: ❌ 异常 - {e}")
    
    print(f"\n多渠道测试结果:")
    for channel, success in results.items():
        print(f"  渠道 {channel}: {'✅ 通过' if success else '❌ 失败'}")
    
    return results

def simulate_user_journey():
    """模拟用户使用流程"""
    print("\n👤 模拟用户完整使用流程")
    print("="*50)
    
    print("1. 用户首次安装应用 (已自动发送)")
    print("2. 用户开始浏览网站...")
    
    for i in range(1, 6):
        print(f"   第{i}次访问网站 {'🎯 触发激活!' if i == 5 else ''}")
    
    print("3. 用户下载文件")
    
    # 测试激活统计
    activation_success = test_activation_tracking()
    
    # 测试下载统计
    download_success = test_download_tracking()
    
    print(f"\n📊 用户流程统计结果:")
    print(f"  激活统计: {'✅ 成功' if activation_success else '❌ 失败'}")
    print(f"  下载统计: {'✅ 成功' if download_success else '❌ 失败'}")
    
    return activation_success and download_success

def main():
    """主测试函数"""
    print("🧪 ChannelTracker 新机制测试")
    print("="*60)
    print("测试范围:")
    print("  ✅ 安装统计 - 自动发送（应用启动时）")
    print("  🎯 激活统计 - 5次网站访问后触发")
    print("  📥 下载统计 - 文件下载完成后触发")
    print("  🔄 多渠道支持")
    
    # 执行各项测试
    tests = [
        ("激活统计测试", test_activation_tracking),
        ("下载统计测试", test_download_tracking), 
        ("用户流程模拟", simulate_user_journey),
        ("多渠道测试", test_multiple_channels)
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
    print("🏆 测试结果汇总")
    print("="*60)
    
    success_count = 0
    total_count = len(results)
    
    for test_name, success in results.items():
        if isinstance(success, dict):
            # 多渠道测试结果
            channel_success = sum(1 for v in success.values() if v)
            channel_total = len(success)
            print(f"  {test_name}: {channel_success}/{channel_total} 渠道通过")
            if channel_success == channel_total:
                success_count += 1
        else:
            print(f"  {test_name}: {'✅ 通过' if success else '❌ 失败'}")
            if success:
                success_count += 1
    
    print(f"\n总体成功率: {success_count}/{total_count} ({success_count/total_count*100:.1f}%)")
    
    if success_count == total_count:
        print("🎉 所有测试通过！新统计机制工作正常")
    else:
        print("⚠️  部分测试失败，请检查API连接或数据格式")

if __name__ == "__main__":
    main()