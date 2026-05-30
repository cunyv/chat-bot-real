#!/bin/bash

# 智能客服API测试脚本

BASE_URL="http://localhost:8080"

echo "=== 智能客服API测试 ==="
echo ""

# 1. 测试添加知识
echo "1. 添加教练知识"
curl -X POST "$BASE_URL/api/knowledge" \
  -H "Content-Type: application/json" \
  -d '{
    "category": "COACH",
    "title": "陈教练 - 羽毛球专项",
    "content": "陈教练，国家二级羽毛球运动员，5年教学经验。擅长羽毛球基本功训练、步法训练、单双打战术指导。教学风格活泼，深受学员喜爱。",
    "metadata": {
      "specialty": "羽毛球",
      "experience": "5年",
      "level": "国家二级",
      "price": 200,
      "location": "北京"
    }
  }'
echo ""
echo ""

# 2. 测试对话
echo "2. 测试对话 - 询问篮球教练"
curl -X POST "$BASE_URL/api/chat/send" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test_user_001",
    "message": "我想学篮球，有什么推荐的教练吗？"
  }'
echo ""
echo ""

# 3. 获取会话列表
echo "3. 获取用户会话列表"
curl -X GET "$BASE_URL/api/chat/sessions/test_user_001"
echo ""
echo ""

# 4. 测试场地查询
echo "4. 测试对话 - 询问场地"
curl -X POST "$BASE_URL/api/chat/send" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test_user_001",
    "message": "北京有合适的篮球场地吗？",
    "sessionId": "从上一步获取的sessionId"
  }'
echo ""
echo ""

# 5. 搜索知识
echo "5. 搜索知识 - 关键词：篮球"
curl -X GET "$BASE_URL/api/knowledge/search?keyword=篮球"
echo ""
echo ""

# 6. 知识检索测试
echo "6. RAG检索测试"
curl -X POST "$BASE_URL/api/knowledge/search" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "推荐一个游泳教练"
  }'
echo ""
echo ""

echo "=== 测试完成 ==="
