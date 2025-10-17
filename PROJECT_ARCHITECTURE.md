# BattleShip 项目架构文档

## 技术栈概览

**后端**: Spring Boot 3.5.6 + Java 21 + Redis + MongoDB  
**前端**: React 19 + Vite 7 + TypeScript 5.9 + Redux Toolkit  
**通信**: REST API + WebSocket (STOMP)

---

## 后端架构

### 技术栈
- Spring Boot 3.5.6, Java 21, Maven
- Spring Web (REST API)
- Spring WebSocket (STOMP 实时通信)
- Spring Data Redis (序列号生成)
- Spring Data MongoDB (事件存储)

### 项目结构
```
backend/src/main/java/app/battleship/
├── api/
│   └── RoomController.java          # REST 控制器
├── service/
│   └── GameService.java             # 游戏业务逻辑
├── model/
│   ├── GameEvent.java               # 游戏事件模型
│   └── MoveRequest.java             # 走子请求模型
├── persist/
│   ├── EventDoc.java                # MongoDB 文档
│   └── EventRepository.java         # MongoDB Repository
├── config/
│   ├── WebSocketConfig.java         # WebSocket 配置
│   └── CorsConfig.java              # CORS 配置
└── BattleshipApplication.java       # 启动类
```

### 核心 API

#### REST 端点
- `GET /api/health` - 健康检查
- `POST /api/rooms/{roomId}/moves` - 提交走子

#### WebSocket
- **连接端点**: `ws://localhost:8080/ws` (支持 SockJS)
- **订阅主题**: `/topic/rooms/{roomId}`
- **应用前缀**: `/app`

### 数据流程
```
1. REST API 接收走子请求
2. Redis 自增序列号: room:{roomId}:seq
3. MongoDB 存储事件: EventDoc
4. STOMP 广播到订阅者: /topic/rooms/{roomId}
```

### 数据模型

**MoveRequest** (走子请求)
```java
{
  moveId: String,    // 走子唯一ID
  playerId: String,  // 玩家ID
  turn: Integer,     // 回合数
  cell: String       // 坐标 (如 "B3")
}
```

**GameEvent** (游戏事件)
```java
{
  seq: long,                    // 序列号
  type: String,                 // 事件类型 (如 "MoveApplied")
  payload: Map<String,Object>,  // 事件数据
  ts: Instant                   // 时间戳
}
```

### 配置 (application.yml)
```yaml
server:
  port: 8080

spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/battleship
    redis:
      host: localhost
      port: 6379
```

---

## 前端架构

### 技术栈
- React 19.1.1, TypeScript 5.9.3, Vite 7.1.7
- Redux Toolkit 2.9.0 (状态管理)
- @stomp/stompjs + sockjs-client (WebSocket)

### 项目结构
```
frontend/src/
├── main.tsx        # 入口文件
├── App.tsx         # 根组件
├── index.css       # 全局样式
└── assets/         # 静态资源
```

### 关键配置

**vite.config.ts**
```typescript
export default defineConfig({
  plugins: [react()],
  define: {
    global: 'globalThis'  // 兼容 SockJS
  }
})
```

**WebSocket 连接示例**
```typescript
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const client = new Client({
  webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
  onConnect: () => {
    client.subscribe('/topic/rooms/{roomId}', (message) => {
      const event = JSON.parse(message.body);
      // 处理游戏事件
    });
  }
});
client.activate();
```

---

## 基础设施

### Docker Compose (infra/docker-compose.dev.yml)
```yaml
services:
  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]
  
  mongo:
    image: mongo:6
    ports: ["27017:27017"]
```

### 启动命令
```bash
# 1. 启动基础设施
docker compose -f infra/docker-compose.dev.yml up -d

# 2. 启动后端 (backend/)
mvn spring-boot:run

# 3. 启动前端 (frontend/)
npm run dev
```

---

## 开发指南

### 后端扩展点
- 添加新的 REST 端点到 `RoomController`
- 扩展 `GameService` 添加游戏逻辑
- 创建新的事件类型 (如 `GameStarted`, `PlayerJoined`)
- 在 `model/` 添加新的数据模型

### 前端扩展点
- 配置 Redux store (未配置)
- 创建 WebSocket hook (`useWebSocket`)
- 添加路由 (需安装 react-router-dom)
- 开发游戏 UI 组件

### 当前状态
- ✅ 基础架构已搭建
- ✅ REST API 和 WebSocket 可用
- ✅ Redis/MongoDB 集成完成
- ⏳ 游戏逻辑待实现
- ⏳ 前端 UI 待开发
- ⏳ 用户认证待添加

---

## 接口示例

### 提交走子
```bash
curl -X POST http://localhost:8080/api/rooms/room1/moves \
  -H "Content-Type: application/json" \
  -d '{
    "moveId": "m1",
    "playerId": "player1",
    "turn": 1,
    "cell": "B3"
  }'
```

**响应**
```json
{
  "seq": 1,
  "type": "MoveApplied",
  "payload": {
    "roomId": "room1",
    "moveId": "m1",
    "playerId": "player1",
    "turn": 1,
    "cell": "B3"
  },
  "ts": "2025-10-15T10:30:00.123Z"
}
```

---

## 架构设计原则

1. **事件溯源**: 所有游戏操作以事件形式存储，支持回放和状态重建
2. **实时通信**: WebSocket 确保低延迟的双向通信
3. **解耦设计**: 前后端通过 REST + WebSocket 分离
4. **可扩展性**: 模块化设计便于添加新功能

