#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试渠道统计API的连通性和响应
"""

import json
import time
import requests
from urllib.parse import urljoin

API_BASE_URL = "https://qudao.eh-viewer.com/api"

def create_test_data(event_type, channel_code="0000"):
    """创建测试数据 - 使用正确的格式"""
    from datetime import datetime
    iso_time = datetime.now().strftime('%Y-%m-%dT%H:%M:%S.%f')[:-3] + 'Z'
    
    base_data = {
        "channelCode": channel_code,
        "softwareId": 1,  # 使用数字ID而不是字符串
        "deviceInfo": {
            "os": "Android 13",
            "model": "Test Device", 
            "manufacturer": "Test",
            "brand": "Test",
            "sdk": 33
        }
    }
    
    if event_type == "install":
        base_data["installTime"] = iso_time
    elif event_type == "download":
        base_data["downloadTime"] = iso_time
    elif event_type == "activate":
        base_data["activateTime"] = iso_time
        base_data["licenseKey"] = "test-license-key"
    
    return base_data

def test_api_endpoint(endpoint, data):
    """测试单个API端点"""
    url = urljoin(API_BASE_URL, endpoint)
    
    print(f"\n=== 测试 {endpoint} ===")
    print(f"URL: {url}")
    print(f"数据: {json.dumps(data, indent=2, ensure_ascii=False)}")
    
    try:
        response = requests.post(
            url, 
            json=data,
            timeout=10,
            headers={
                'Content-Type': 'application/json',
                'User-Agent': 'EhViewer-ChannelTracker/1.0'
            }
        )
        
        print(f"状态码: {response.status_code}")
        print(f"响应头: {dict(response.headers)}")
        
        if response.text:
            try:
                response_json = response.json()
                print(f"响应内容: {json.dumps(response_json, indent=2, ensure_ascii=False)}")
            except:
                print(f"响应内容 (文本): {response.text}")
        else:
            print("响应内容: (空)")
            
        return response.status_code, response.text
        
    except requests.exceptions.ConnectTimeout:
        print("❌ 连接超时")
        return None, "连接超时"
    except requests.exceptions.ReadTimeout:
        print("❌ 读取超时")
        return None, "读取超时"
    except requests.exceptions.ConnectionError as e:
        print(f"❌ 连接错误: {e}")
        return None, f"连接错误: {e}"
    except requests.exceptions.RequestException as e:
        print(f"❌ 请求异常: {e}")
        return None, f"请求异常: {e}"
    except Exception as e:
        print(f"❌ 未知错误: {e}")
        return None, f"未知错误: {e}"

def main():
    print("🔍 开始测试渠道统计API")
    print(f"目标API: {API_BASE_URL}")
    
    # 先测试基础连通性
    print(f"\n=== 测试基础连通性 ===")
    try:
        response = requests.get("https://qudao.eh-viewer.com", timeout=5)
        print(f"主站状态码: {response.status_code}")
    except Exception as e:
        print(f"主站连接失败: {e}")
    
    # 测试各个统计端点
    test_cases = [
        ("/stats/install", create_test_data("install")),
        ("/stats/download", create_test_data("download")), 
        ("/stats/activate", create_test_data("activate")),
        ("/stats/install", create_test_data("install", "3001")),  # 测试3001渠道
    ]
    
    results = []
    for endpoint, data in test_cases:
        status_code, response_text = test_api_endpoint(endpoint, data)
        results.append({
            'endpoint': endpoint,
            'status_code': status_code,
            'response': response_text,
            'channel': data.get('channelCode')
        })
        time.sleep(1)  # 避免请求过于频繁
    
    # 汇总结果
    print(f"\n=== 测试结果汇总 ===")
    for result in results:
        endpoint = result['endpoint']
        status = result['status_code']
        channel = result['channel']
        print(f"{endpoint} (渠道{channel}): {'✅' if status and 200 <= status < 300 else '❌'} {status}")
    
    print("\n📝 测试完成！请检查上述结果确认API状态。")

if __name__ == "__main__":
    main()