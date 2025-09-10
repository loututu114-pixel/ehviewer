#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
最终的渠道统计API测试
"""

import json
import time
import requests
from datetime import datetime

API_BASE_URL = "https://qudao.eh-viewer.com/api"

def test_api_with_curl_style():
    """使用与curl相同的方式测试API"""
    
    # 测试数据
    test_cases = [
        {
            "name": "安装统计 - 渠道3001",
            "endpoint": "/stats/install",
            "data": {
                "channelCode": "3001",
                "softwareId": 1,
                "installTime": "2025-09-04T01:20:00.000Z"
            }
        },
        {
            "name": "激活统计 - 渠道3001（有收益）",
            "endpoint": "/stats/activate", 
            "data": {
                "channelCode": "3001",
                "softwareId": 1,
                "activateTime": "2025-09-04T01:20:00.000Z"
            }
        },
        {
            "name": "下载统计 - 渠道3001",
            "endpoint": "/stats/download",
            "data": {
                "channelCode": "3001", 
                "softwareId": 1,
                "downloadTime": "2025-09-04T01:20:00.000Z"
            }
        },
        {
            "name": "安装统计 - 渠道0000（测试不存在）",
            "endpoint": "/stats/install",
            "data": {
                "channelCode": "0000",
                "softwareId": 1, 
                "installTime": "2025-09-04T01:20:00.000Z"
            }
        }
    ]
    
    print("🔍 开始最终API测试")
    print(f"目标API: {API_BASE_URL}")
    print("=" * 50)
    
    results = []
    
    for test_case in test_cases:
        print(f"\n📋 {test_case['name']}")
        url = API_BASE_URL + test_case['endpoint']
        
        try:
            # 使用更简单的请求方式，模拟curl
            response = requests.post(
                url,
                json=test_case['data'],
                headers={
                    'Content-Type': 'application/json'
                },
                timeout=10
            )
            
            print(f"🔗 URL: {url}")
            print(f"📊 状态码: {response.status_code}")
            
            if response.text:
                try:
                    result = response.json()
                    print(f"📝 响应: {json.dumps(result, ensure_ascii=False, indent=2)}")
                    
                    if result.get('success'):
                        print("✅ 成功")
                        if 'revenue' in result.get('data', {}):
                            print(f"💰 收益: ¥{result['data']['revenue']}")
                    else:
                        print(f"❌ 失败: {result.get('message', '未知错误')}")
                        
                except Exception as e:
                    print(f"📄 响应文本: {response.text}")
                    
            results.append({
                'name': test_case['name'],
                'status_code': response.status_code,
                'success': response.status_code == 200,
                'response': response.text
            })
            
        except Exception as e:
            print(f"❌ 请求失败: {e}")
            results.append({
                'name': test_case['name'],
                'status_code': None,
                'success': False,
                'error': str(e)
            })
        
        time.sleep(1)  # 避免请求过快
    
    # 汇总报告
    print("\n" + "=" * 50)
    print("📊 测试结果汇总")
    print("=" * 50)
    
    success_count = 0
    for result in results:
        status = "✅ 成功" if result['success'] else "❌ 失败"
        print(f"{result['name']}: {status}")
        if result['success']:
            success_count += 1
    
    print(f"\n📈 成功率: {success_count}/{len(results)} ({success_count/len(results)*100:.1f}%)")
    
    if success_count > 0:
        print("\n🎉 恭喜！API已经可以正常工作了！")
        print("📱 你的ChannelTracker SDK已经准备就绪，可以开始统计了。")
    else:
        print("\n⚠️  API还有问题，需要进一步检查。")

if __name__ == "__main__":
    test_api_with_curl_style()