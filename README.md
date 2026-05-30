# 运动品类撮合交易平台 - 智能客服系统

基于Spring Boot + RAG的智能客服系统，帮助用户咨询教练和场地信息。

## 技术栈

- **后端**: Spring Boot 3.2, Java 17
- **数据库**: MySQL 8.0
- **缓存**: Redis
- **LLM**: 小米MiMo (mimo-v2.5-pro)
- **检索**: 基于关键词匹配的RAG（无需embedding模型）

## 前置条件

1. 安装Java 17+
2. 安装MySQL 8.0
3. 安装Redis
4. 获取小米MiMo API密钥

## 快速开始

### 1. 初始化数据库

```bash
mysql -u root -p123456 < src/main/resources/db/init.sql
```

### 2. 配置API密钥

设置环境变量（可选，已有默认值）：
```bash
export MIMO_API_KEY=你的API密钥
```

或直接修改 `application.yml` 中的配置。

### 3. 编译运行

```bash
mvn clean package -DskipTests
java -jar target/customer-service-1.0.0.jar
```

或使用Maven插件直接运行：
```bash
mvn spring-boot:run
```

## API接口

### 对话接口

#### 发送消息
```http
POST /api/chat/send
Content-Type: application/json

{
    "userId": "user001",
    "message": "我想学篮球，有什么推荐的教练吗？",
    "sessionId": ""  // 可选，为空则创建新会话
}
```

#### 获取会话历史
```http
GET /api/chat/history/{sessionId}
```

#### 获取用户会话列表
```http
GET /api/chat/sessions/{userId}
```

#### 关闭会话
```http
POST /api/chat/session/{sessionId}/close
```

### 知识库接口

#### 添加知识
```http
POST /api/knowledge
Content-Type: application/json

{
    "category": "COACH",
    "title": "新教练",
    "content": "教练详细信息...",
    "metadata": {
        "specialty": "篮球",
        "price": 300
    }
}
```

#### 获取所有知识
```http
GET /api/knowledge
```

#### 搜索知识
```http
GET /api/knowledge/search?keyword=篮球
```

#### 知识检索测试
```http
POST /api/knowledge/search
Content-Type: application/json

{
    "query": "有什么好的篮球教练？"
}
```

## 示例对话

```
用户: 我想学篮球，有什么推荐的教练吗？
助手: 🏀 推荐您几位优秀的篮球教练：

1. **张教练** - 篮球专项
   - 国家一级篮球运动员，10年教学经验
   - 擅长青少年篮球基础训练、投篮技术纠正
   - 授课价格：300元/小时
   - 评分：⭐ 4.9

张教练教学经验丰富，特别适合初学者。您可以在我们平台直接预约体验课哦！😊

用户: 北京有合适的场地吗？
助手: 🏟️ 北京有以下优质篮球场地推荐：

1. **星耀篮球馆**
   - 地址：北京市朝阳区
   - 4块标准篮球场，专业枫木地板
   - 配备LED照明和空调
   - 价格：200元/小时
   - 设施：更衣室、淋浴、停车场

这个场馆设施齐全，环境很好，很多教练都在这里授课。需要帮您预约吗？😊
```

## 项目结构

```
src/main/java/com/sports/customer/
├── CustomerServiceApplication.java  # 启动类
├── config/                          # 配置类
│   ├── ChatConfig.java
│   ├── DataInitializer.java
│   ├── MimoConfig.java
│   ├── RagConfig.java
│   └── RedisConfig.java
├── controller/                      # 控制器
│   ├── ChatController.java
│   ├── GlobalExceptionHandler.java
│   └── KnowledgeController.java
├── dto/                            # 数据传输对象
│   ├── ChatRequest.java
│   ├── ChatResponse.java
│   └── KnowledgeRequest.java
├── entity/                         # 实体类
│   ├── ChatSession.java
│   ├── Conversation.java
│   └── KnowledgeBase.java
├── repository/                     # 数据访问层
│   ├── ChatSessionRepository.java
│   ├── ConversationRepository.java
│   └── KnowledgeBaseRepository.java
├── service/                        # 业务逻辑层
│   ├── ChatService.java
│   ├── MimoService.java
│   └── RagService.java
└── util/                          # 工具类
    └── VectorUtil.java
```

## 配置说明

### application.yml主要配置

```yaml
# MySQL配置
spring.datasource.url: jdbc:mysql://localhost:3306/customer_service
spring.datasource.username: root
spring.datasource.password: 123456

# Redis配置
spring.data.redis.host: localhost
spring.data.redis.port: 6379

# MiMo API配置
mimo.api-key: ${MIMO_API_KEY:你的密钥}
mimo.base-url: https://token-plan-cn.xiaomimimo.com/v1
mimo.chat-model: mimo-v2.5-pro

# RAG配置
rag.top-k: 3
rag.similarity-threshold: 0.7
```

## RAG检索原理

当前使用**基于关键词匹配**的检索方案：

1. **关键词提取**: 从用户查询中提取关键词，过滤停用词
2. **领域扩展**: 根据运动领域映射表扩展相关词
3. **相关性计算**:
   - 标题匹配（权重3分）
   - 内容匹配（每个关键词最多2分）
   - 分类匹配（2分）
   - 元数据匹配（1分）
4. **排序返回**: 按分数降序，返回Top-K结果

## 扩展建议

1. **向量检索**: 如需更精准的语义检索，可接入embedding模型
2. **流式响应**: 实现SSE流式返回，提升用户体验
3. **多轮对话优化**: 引入对话摘要机制，减少Token消耗
4. **知识库管理界面**: 开发Web管理界面，方便运营人员维护
5. **用户反馈机制**: 收集用户对回答的满意度，持续优化

## 常见问题

### Q: 如何添加新的知识？
A: 调用 `POST /api/knowledge` 接口添加，系统会自动索引。

### Q: 如何更换LLM模型？
A: 修改 `application.yml` 中的 `mimo.chat-model` 配置。

### Q: 检索效果不好怎么办？
A: 可以调整 `RagService` 中的关键词映射表和权重配置。

## License

MIT
