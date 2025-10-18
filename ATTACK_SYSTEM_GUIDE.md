# Battleship 攻击标记系统完整指南

## 📋 目录

1. [系统概述](#系统概述)
2. [核心功能](#核心功能)
3. [数据模型](#数据模型)
4. [游戏机制](#游戏机制)
5. [测试指南](#测试指南)
6. [故障排除](#故障排除)

---

## 系统概述

本系统实现了完整的攻击标记追踪机制，支持：
- ✅ 静态Hit/Miss标记（历史记录）
- ✅ 重复攻击验证对手移动
- ✅ 船只移动保持相对受损位置
- ✅ 完全受损船只自动沉没并移除
- ✅ Miss隐私保护（对手看不到你的Miss）
- ✅ AI智能感知玩家视角

---

## 核心功能

### 1. 攻击标记显示 ✅

**Opponent Board（对手棋盘）**：
- 💥 你的Hit攻击（红色背景）
- 💦 你的Miss攻击（蓝色背景）
- 🚢 击沉的敌方船只列表

**Your Board（你的棋盘）**：
- 🚢 你的船只（蓝色）
- 💥 对手击中你的位置（红色背景）
- ❌ 不显示对手的Miss（隐私保护）

### 2. 静态标记追踪 ✅

**核心原则**：
- 标记是**历史记录**，记录攻击时的结果
- 对手移动船只**不会**自动改变你的标记
- 只有重新攻击同一位置才会更新标记

**数据存储**：
```java
Board.attacksByMeHits    // 我的Hit攻击（静态列表）
Board.attacksByMeMisses  // 我的Miss攻击（静态列表）
```

### 3. 重复攻击机制 ✅

**功能**：允许玩家重复攻击同一位置来验证对手是否移动了船只

**场景示例**：
```
回合1: 攻击 (5,5) → Hit 💥
回合3: 对手移动船只离开 (5,5)
回合5: 再次攻击 (5,5) → Miss 💦（标记自动更新）
结论: 对手移动了船只！
```

**实现逻辑**：
```java
// 移除旧记录
attacksByMeHits.remove(target);
attacksByMeMisses.remove(target);

// 根据当前结果重新记录
if (isHit) {
    attacksByMeHits.add(target);
} else {
    attacksByMeMisses.add(target);
}
```

### 4. 相对受损位置追踪 ✅

**核心机制**：使用索引记录船只哪些相对部位受损

**数据结构**：
```java
Ship.hitIndices = {1, 3}  // 第2个和第4个格子受损
```

**移动时的处理**：
```
移动前: CARRIER_5 在 (5, 4-8)，索引1受损 → (5,5) 显示💥
移动后: CARRIER_5 在 (2, 3-7)，索引1受损 → (2,4) 显示💥
结果: 受损的相对位置保持在第2个格子
```

### 5. 船只沉没机制 ✅

**触发条件**：`hitIndices.size() >= cells.size()` （所有部位都被击中）

**处理流程**：
```java
if (ship.isFullyDamaged()) {
    ship.setSunk(true);
    board.ships.remove(ship);        // 从活跃船只移除
    board.sunkShips.add(ship);       // 添加到沉没列表
}
```

**视觉效果**：
- 船只从Your Board消失
- Opponent Board下方显示："Enemy Ships Destroyed: CARRIER_5"
- 对手可以看到沉没船只的完整最终位置

### 6. Miss隐私保护 ✅

**原则**：Miss攻击只有攻击者能看到

**实现**：
- Hit攻击：记录到双方棋盘
  ```java
  defender.board.hits.add(target);     // 对手看得到
  attacker.board.attacksByMeHits.add(target);  // 我也看得到
  ```
- Miss攻击：只记录到攻击者
  ```java
  // defender.board.misses.add(target);  // 不记录！对手看不到
  attacker.board.attacksByMeMisses.add(target);  // 只有我看得到
  ```

**游戏优势**：
- ✅ 对手不知道你探索了哪些区域
- ✅ 对手移动船只时有更大风险
- ✅ 增加不对称信息和策略深度

---

## 数据模型

### Ship.java

```java
public class Ship {
    private String id;
    private ShipKind kind;
    private List<Coord> cells;           // 当前绝对位置
    private boolean sunk;                // 是否沉没
    private Set<Integer> hitIndices;     // 受损的相对位置（0-based）
    
    @JsonIgnore  // 防止序列化问题
    public boolean isFullyDamaged() {
        return hitIndices != null && cells != null 
            && hitIndices.size() >= cells.size();
    }
    
    @JsonIgnore  // 防止序列化问题
    public List<Coord> getDamagedCells() {
        // 根据hitIndices计算当前受损的绝对坐标
        List<Coord> damaged = new ArrayList<>();
        if (hitIndices != null && cells != null) {
            for (Integer idx : hitIndices) {
                if (idx >= 0 && idx < cells.size()) {
                    damaged.add(cells.get(idx));
                }
            }
        }
        return damaged;
    }
}
```

### Board.java

```java
public class Board {
    // 船只列表
    private List<Ship> ships;              // 活跃船只（未沉没）
    private List<Ship> sunkShips;          // 已沉没船只
    
    // 己方棋盘上的攻击记录
    private List<Coord> hits;              // 对手对我的Hit攻击
    private List<Coord> misses;            // 空列表（对手Miss不记录）
    
    // 对对手的攻击记录（只有我能看到）
    private List<Coord> attacksByMeHits;   // 我的Hit攻击（静态）
    private List<Coord> attacksByMeMisses; // 我的Miss攻击（静态）
}
```

### 视图结构

```json
{
  "me": {
    "board": {
      "ships": [...],           // 我的所有船只
      "hits": [...],            // 对手击中我的位置
      "misses": []              // 空（对手Miss我看不到）
    }
  },
  "opponent": {
    "revealed": {
      "attacksByMe": {
        "hits": [...],          // 我的Hit攻击
        "misses": [...]         // 我的Miss攻击
      },
      "sunkShips": [...]        // 击沉的敌方船只
    }
  },
  "turn": 5,
  "currentPlayerId": "xxx",
  "stateVersion": 5
}
```

---

## 游戏机制

### 攻击流程

```
1. 玩家点击对手棋盘 (r, c)
    ↓
2. 后端检查该位置是否有船只（当前时刻）
    ↓
3a. 如果Hit:
    - defender.board.hits.add(target)
    - attacker.board.attacksByMeHits.add(target)
    - ship.hitIndices.add(relativeIndex)
    - 检查ship.isFullyDamaged() → 沉没
    ↓
3b. 如果Miss:
    - attacker.board.attacksByMeMisses.add(target)
    - 不记录到对手棋盘（隐私）
    ↓
4. 保存状态到Redis + MongoDB
    ↓
5. 切换回合：currentPlayerId = opponent
    ↓
6. 返回更新的视图给前端
    ↓
7. 前端显示标记并切换回合显示
```

### 船只移动流程

```
1. 玩家选择"Move Ship"模式
    ↓
2. 拖动船只到新位置
    ↓
3. 后端验证移动合法性
    ↓
4. 处理受损位置:
    - oldDamaged = ship.getDamagedCells()
    - board.hits.removeAll(oldDamaged)
    - ship.cells = newCells
    - newDamaged = ship.getDamagedCells()
    - board.hits.addAll(newDamaged)
    ↓
5. 保存状态
    ↓
6. 切换回合
    ↓
7. 前端更新显示：受损标记跟随船只
```

### 重复攻击流程

```
1. 玩家点击已攻击过的格子
    ↓
2. 后端处理:
    - wasHit = attacksByMeHits.remove(target)
    - wasMiss = attacksByMeMisses.remove(target)
    - 重新检查当前是否有船只
    - 根据新结果添加到相应列表
    ↓
3. 返回新结果
    ↓
4. 前端更新标记显示
    ↓
5. 玩家据此判断对手是否移动了船只
```

### 船只沉没流程

```
1. 攻击击中船只
    ↓
2. ship.hitIndices.add(index)
    ↓
3. 检查: hitIndices.size() >= cells.size()
    ↓
4. 如果完全受损:
    - ship.setSunk(true)
    - board.ships.remove(ship)
    - board.sunkShips.add(ship)
    ↓
5. 检查是否所有船只都沉没
    ↓
6. 如果是 → 游戏结束，判定胜者
```

---

## 测试指南

### 基本功能测试

#### 测试1：攻击标记显示

**步骤**：
1. 刷新浏览器 (http://localhost:5174)
2. 创建房间并开始游戏
3. 进行攻击

**期望结果**：
- ✅ 攻击后显示Toast："🎯 Hit!" 或 "💦 Miss!"
- ✅ Opponent Board显示 💥 或 💦
- ✅ 回合切换到对手
- ✅ 显示 "⏳ Opponent's Turn"

#### 测试2：重复攻击验证

**步骤**：
1. 攻击 (5,5) → 记录结果（假设是Hit 💥）
2. 等待对手移动船只
3. 再次点击 (5,5)

**期望结果**：
- ✅ 允许重复攻击（不会被拒绝）
- ✅ 如果对手移走船只 → 显示Miss 💦
- ✅ 如果对手未移动 → 仍显示Hit 💥
- ✅ 标记实时更新

#### 测试3：船只移动保持受损

**步骤**：
1. 让对手击中你的某艘船（例如CARRIER_5的第2个格子）
2. 轮到你时，选择"Move Ship"模式
3. 拖动该船只到新位置

**期望结果**：
- ✅ 💥标记跟随船只移动
- ✅ 受损的相对位置保持不变（仍是第2个格子）
- ✅ Your Board正确显示新位置的受损标记

#### 测试4：船只完全沉没

**步骤**：
1. 集中火力攻击对手的DESTROYER_2
2. 击中第1个格子
3. 击中第2个格子（最后一个）

**期望结果**：
- ✅ Toast显示："🚢 DESTROYER_2 sunk!"
- ✅ 船只从对手Your Board消失
- ✅ 你的Opponent Board下方列表显示："Enemy Ships Destroyed: DESTROYER_2"
- ✅ 对手不能再移动该船只

#### 测试5：Miss隐私保护

**步骤**：
1. 玩家A攻击玩家B的 (5,5) → Miss 💦
2. 切换到玩家B的视角
3. 查看Your Board的 (5,5) 位置

**期望结果**：
- ✅ 玩家A的Opponent Board：(5,5) 显示 💦
- ✅ 玩家B的Your Board：(5,5) **没有任何标记**
- ✅ 玩家B不知道玩家A miss了这里

#### 测试6：标记不随对手移动自动改变

**步骤**：
1. 玩家A攻击 (3,3) → Hit 💥
2. 玩家B移动船只离开 (3,3)
3. 玩家A刷新页面或等待更新

**期望结果**：
- ✅ 玩家A的Opponent Board：(3,3) **仍然显示** 💥
- ✅ 标记不会自动变成 💦
- ✅ 只有重新攻击 (3,3) 才会发现船只已移走

### AI功能测试

#### 测试7：AI感知正确

**步骤**：
1. 进行多次攻击（包括Hit和Miss）
2. 点击"Request AI Suggestion"
3. 观察浏览器Network标签中发送的请求

**期望结果**：
- ✅ AI接收到完整的 attacksByMeHits 和 attacksByMeMisses 数据
- ✅ AI建议避开已攻击的位置
- ✅ AI的建议合理（基于历史记录推测）

---

## 数据流程图

### 攻击数据流

```
前端: 点击 (r,c)
    ↓ HTTP POST /api/games/{id}/action/attack
后端: GameController.attack()
    ↓
GameService.processAttack()
    ↓ 判定Hit/Miss
    ┌─────────────┬─────────────┐
    │ Hit         │ Miss        │
    ├─────────────┼─────────────┤
    │ defender.   │             │
    │ board.hits  │（不记录）    │
    │ ↓           │ ↓           │
    │ attacker.   │ attacker.   │
    │ attacksByMe │ attacksByMe │
    │ Hits        │ Misses      │
    │ ↓           │ ↓           │
    │ ship.hit    │             │
    │ Indices     │             │
    │ ↓           │             │
    │ 检查沉没    │             │
    └─────────────┴─────────────┘
    ↓
saveGameState() → Redis
    ↓
switchTurn()
    ↓
saveGameState() → Redis
    ↓
getGameState() ← Redis
    ↓
ViewShapingService.createPlayerView()
    ↓ 返回
GameController
    ↓ HTTP Response
前端: 更新Redux Store
    ↓
OpponentBoard.tsx: 渲染标记
```

### 船只移动数据流

```
前端: 拖动船只
    ↓ HTTP POST /api/games/{id}/action/move
后端: GameController.moveShip()
    ↓
GameService.processShipMove()
    ↓
oldDamaged = ship.getDamagedCells()
    ↓
board.hits.removeAll(oldDamaged)
    ↓
ship.cells = newCells
    ↓
newDamaged = ship.getDamagedCells()
    ↓
board.hits.addAll(newDamaged)
    ↓
saveGameState() → Redis
    ↓
switchTurn()
    ↓
返回更新视图
    ↓
前端: MyBoard.tsx 重新渲染
```

---

## 代码位置

### 后端核心文件

| 文件 | 说明 | 关键方法 |
|------|------|----------|
| `model/Ship.java` | 船只模型 | `isFullyDamaged()`, `getDamagedCells()` |
| `model/Board.java` | 棋盘模型 | 数据字段定义 |
| `service/GameService.java` | 游戏逻辑 | `processAttack()`, `processShipMove()`, `markShipHit()` |
| `service/ViewShapingService.java` | 视图生成 | `createPlayerView()` |
| `service/AiSuggestionService.java` | AI建议 | `buildGameStateInfo()`, `buildPrompt()` |
| `api/GameController.java` | API接口 | `attack()`, `moveShip()` |

### 前端核心文件

| 文件 | 说明 | 关键功能 |
|------|------|----------|
| `components/OpponentBoard.tsx` | 对手棋盘 | 显示Hit/Miss标记 |
| `components/MyBoard.tsx` | 己方棋盘 | 显示船只和受损 |
| `pages/Game.tsx` | 游戏页面 | 攻击处理、状态管理 |
| `store/slices/gameSlice.ts` | Redux状态 | 游戏状态管理 |
| `api/client.ts` | API客户端 | 攻击、移动请求 |

---

## 故障排除

### 问题1：攻击后不切换回合

**症状**：攻击后仍然是你的回合

**原因**：序列化/反序列化失败，数据丢失

**解决方案**：
```bash
# 1. 清空Redis
redis-cli FLUSHALL

# 2. 重启后端
lsof -ti:8080 | xargs kill -9
cd backend && java -jar target/backend-0.0.1-SNAPSHOT.jar > /tmp/battleship-backend.log 2>&1 &

# 3. 刷新浏览器并开始新游戏
```

### 问题2：标记不显示

**症状**：攻击后Opponent Board没有显示💥或💦

**诊断步骤**：
1. 打开浏览器控制台（F12）
2. 查看是否有JavaScript错误
3. 检查攻击响应：`[Game] New attacksByMe.hits`

**可能原因**：
- 后端返回空数组 → 检查后端日志
- WebSocket覆盖了数据 → 检查STATE_UPDATED事件
- 前端渲染问题 → 检查OpponentBoard组件

**解决方案**：
```bash
# 查看后端日志
tail -f /tmp/battleship-backend.log | grep "ATTACK\|attacksByMe"

# 应该看到：
# [GameService] AttacksByMeHits AFTER: [Coord(...)]
# [ViewShaping] Static myHits: [...]
```

### 问题3：Failed to load game state

**症状**：页面显示"Failed to load game state"

**原因**：旧游戏数据与新模型不兼容

**解决方案**：
```bash
# 完全清空数据
./test.sh clean

# 或手动执行：
redis-cli FLUSHALL
docker-compose -f infra/docker-compose.dev.yml down -v
docker-compose -f infra/docker-compose.dev.yml up -d
```

### 问题4：船只移动后受损位置丢失

**症状**：移动船只后💥标记消失

**诊断**：
```bash
# 查看移动日志
grep "MOVING SHIP" /tmp/battleship-backend.log -A 10

# 应该看到：
# Old cells: [...]
# HitIndices: [1, 3]
# Removed old damaged cells: [...]
# New damaged cells added: [...]
```

**可能原因**：
- hitIndices为null
- getDamagedCells()返回空列表
- board.hits没有正确更新

---

## 测试脚本使用

### 快速测试

```bash
# 运行完整测试
./test.sh

# 查看实时日志
./test.sh logs

# 清空所有数据
./test.sh clean

# 重启所有服务
./test.sh restart
```

### 手动命令

```bash
# 查看后端日志
tail -f /tmp/battleship-backend.log

# 筛选攻击相关日志
tail -f /tmp/battleship-backend.log | grep -E "ATTACK|attacksByMe|SAVED|LOADED"

# 筛选回合切换日志
tail -f /tmp/battleship-backend.log | grep -E "Switching turn|After switchTurn|Turn:"

# 检查Redis数据
redis-cli KEYS "game:*"
redis-cli GET "game:<gameId>:state"
```

---

## 性能优化

### 静态vs动态

**旧实现（动态计算）**：
```java
// 每次都计算 - O(attacks × ships × cells)
myHits = attackedByMe.stream()
    .filter(coord -> ships.stream()
        .anyMatch(ship -> ship.getCells().contains(coord)))
    .collect(toList());
```

**新实现（静态记录）**：
```java
// 直接返回 - O(1)
myHits = board.getAttacksByMeHits();
```

**性能提升**：
- ✅ 减少CPU使用
- ✅ 降低响应延迟
- ✅ 简化代码逻辑

### 内存使用

- 最多记录100个攻击（10×10棋盘）
- hitIndices使用Set，每艘船最多5个索引
- sunkShips最多5艘船
- 总内存增加：< 10KB per game

---

## 游戏策略

### 进攻策略

**信息收集**：
```
✅ 你的Opponent Board记录所有攻击
✅ Hit标记 → 曾经有船只的位置
✅ Miss标记 → 曾经没有船只的位置
⚠️ 对手可能已经移动船只
```

**验证策略**：
```
何时重复攻击？
✅ 连续Hit区域 → 验证船只是否还在
✅ 关键战术位置 → 确保情报准确
✅ 怀疑对手移动 → 验证假设
❌ 随机验证 → 浪费回合
```

### 防守策略

**移动时机**：
```
应该移动：
✅ 受到连续攻击（对手发现了方向）
✅ 快要被击沉（4/5格子受损）
✅ 有明显更安全的位置

不应移动：
❌ 只是偶然被击中
❌ 新位置更危险
❌ 需要保存回合用于攻击
```

**移动目标**：
```
优先考虑：
✅ 对手Miss过的位置（他们认为安全）
✅ 角落和边缘（探索难度高）
✅ 已击沉船只的附近（对手可能不再关注）

避免移动到：
❌ 对手Hit密集的区域
❌ 对手探索路径上
❌ 棋盘中心（易被发现）
```

---

## 常见问题 FAQ

### Q1: 为什么我攻击后标记不显示？

**A**: 检查以下几点：
1. 后端是否正常运行？`curl http://localhost:8080/actuator/health`
2. Redis是否清空了旧数据？`redis-cli FLUSHALL`
3. 是否开始了新游戏？（旧游戏数据不兼容）
4. 浏览器控制台有错误吗？

### Q2: 为什么攻击后不切换回合？

**A**: 这通常是因为：
1. 后端序列化失败 → 检查后端日志有无"ERROR"
2. 旧游戏数据 → 清空数据并开始新游戏
3. WebSocket连接问题 → 刷新页面重新连接

### Q3: 船只移动后受损位置消失了？

**A**: 确保：
1. 使用的是新编译的后端
2. hitIndices字段正确保存
3. 查看后端日志："New damaged cells added"

### Q4: 对手能看到我的Miss吗？

**A**: 不能！Miss是私密信息：
- 你的Miss只显示在你的Opponent Board
- 对手的Your Board不会显示你的Miss
- 这是正确的游戏逻辑

### Q5: 可以重复攻击同一位置吗？

**A**: 可以！而且这是重要的游戏机制：
- 用于验证对手是否移动了船只
- 消耗一个回合，需要权衡
- 标记会根据当前结果更新

---

## 版本说明

### 当前版本特性

- **静态标记追踪**：攻击结果永久记录
- **重复攻击**：验证对手移动
- **相对受损追踪**：移动保持伤害
- **自动沉没移除**：完全受损船只消失
- **Miss隐私**：对手看不到你的Miss
- **AI智能感知**：基于历史记录决策

### 与传统战舰游戏的差异

| 特性 | 传统游戏 | 本系统 |
|------|----------|--------|
| 船只移动 | ❌ 不可移动 | ✅ 可以移动 |
| 重复攻击 | ❌ 不允许 | ✅ 允许验证 |
| 受损追踪 | ✅ 固定位置 | ✅ 相对位置 |
| Miss可见性 | ❌ 对手看不到 | ✅ 对手看不到 |
| 标记更新 | N/A | ✅ 重复攻击更新 |

---

## 技术架构

### 后端技术栈

```
Spring Boot 3.5.6
├── Spring Security (JWT认证)
├── Spring WebSocket (实时通信)
├── Spring Data MongoDB (游戏快照)
├── Spring Data Redis (状态缓存)
└── Jackson (JSON序列化)
```

### 前端技术栈

```
React 18 + TypeScript
├── Redux Toolkit (状态管理)
├── React Router (路由)
├── SockJS + STOMP (WebSocket)
├── Axios (HTTP客户端)
└── Vite (构建工具)
```

### 数据存储策略

| 存储 | 用途 | 特点 |
|------|------|------|
| Redis | 游戏状态缓存 | 快速读写，临时存储 |
| MongoDB | 游戏快照、事件 | 持久化，可回溯 |
| 内存 | 实时计算 | hitIndices, sunkShips |

---

## 开发指南

### 添加新字段到Board

1. 在 `Board.java` 添加字段
2. 确保字段有默认值（如 `= new ArrayList<>()`）
3. 如果是计算字段，添加 `@JsonIgnore`
4. 清空Redis： `redis-cli FLUSHALL`
5. 重启后端

### 修改攻击逻辑

1. 修改 `GameService.processAttack()`
2. 更新 `ViewShapingService.createPlayerView()`
3. 如果影响AI，修改 `AiSuggestionService`
4. 添加相应的日志
5. 测试完整流程

### 调试技巧

**启用详细日志**：
```java
System.out.println("[Module] Debug message: " + data);
```

**检查序列化**：
```java
String json = objectMapper.writeValueAsString(object);
System.out.println(json);
```

**追踪数据流**：
```
[GameService] PROCESSING ATTACK
[GameService] SAVED TO REDIS
[GameService] LOADED FROM REDIS
[ViewShaping] Creating view
[GameController] Returning response
```

---

## 总结

### 实现的核心价值

✅ **完整的攻击追踪**：所有攻击都有明确记录  
✅ **策略深度**：重复攻击、移动验证、信息战  
✅ **数据一致性**：静态记录，不会丢失  
✅ **性能优化**：O(1)查询，无需动态计算  
✅ **隐私保护**：Miss信息不泄露给对手  
✅ **AI增强**：准确理解玩家视角  

### 游戏体验提升

✅ **真实性**：符合传统战舰游戏规则  
✅ **可玩性**：船只移动增加变化  
✅ **策略性**：信息收集、验证、推测  
✅ **公平性**：双方信息对称（除Miss外）  
✅ **趣味性**：心理战、情报战  

---

## 快速参考

### 攻击标记

| 标记 | 含义 | 显示位置 |
|------|------|----------|
| 💥 | Hit（命中） | Opponent Board + 对手Your Board |
| 💦 | Miss（未中） | 仅Opponent Board |
| 🚢 | 沉没船只 | Opponent Board列表 |

### 回合行为

| 操作 | 消耗回合 | 效果 |
|------|----------|------|
| 普通攻击 | ✅ | 探索新区域 |
| 重复攻击 | ✅ | 验证船只位置 |
| 移动船只 | ✅ | 调整防御 |

### 命令速查

```bash
# 启动服务
docker-compose -f infra/docker-compose.dev.yml up -d
cd backend && java -jar target/backend-0.0.1-SNAPSHOT.jar &
cd frontend && npm run dev

# 查看日志
tail -f /tmp/battleship-backend.log

# 清空数据
redis-cli FLUSHALL

# 健康检查
curl http://localhost:8080/actuator/health
```

---

**文档版本**: 1.0  
**最后更新**: 2025-10-18  
**状态**: ✅ 所有功能已实现并测试就绪

