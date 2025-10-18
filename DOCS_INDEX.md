# 📚 文档索引

## 核心文档（按阅读顺序）

### 1. 快速入门
- **[README.md](README.md)** - 项目概述和快速开始
- **[QUICKSTART.md](QUICKSTART.md)** - 详细的快速开始指南

### 2. 功能说明
- **[ATTACK_SYSTEM_GUIDE.md](ATTACK_SYSTEM_GUIDE.md)** ⭐ **必读** - 攻击标记系统完整指南
  - 静态Hit/Miss追踪
  - 重复攻击验证
  - 相对受损位置追踪
  - 船只沉没机制
  - Miss隐私保护
  - 测试指南

### 3. 技术文档
- **[PROJECT_ARCHITECTURE.md](PROJECT_ARCHITECTURE.md)** - 项目架构详解
  - 系统架构
  - 数据流
  - 技术栈
- **[PROJECT_SUMMARY.md](PROJECT_SUMMARY.md)** - 项目功能总结

---

## 测试工具

### 统一测试脚本 `test.sh`

```bash
# 查看所有命令
./test.sh

# 常用命令
./test.sh test          # 运行完整测试流程
./test.sh logs          # 查看实时日志
./test.sh logs-attack   # 查看攻击日志
./test.sh clean         # 清空数据
./test.sh status        # 检查服务状态
```

### 日志位置

- **后端日志**: `/tmp/battleship-backend.log`
- **前端日志**: 浏览器控制台（F12）

---

## 快速查找

### 我想了解...

| 主题 | 查看文档 |
|------|----------|
| 如何启动项目 | README.md → Quick Start |
| 攻击标记如何工作 | ATTACK_SYSTEM_GUIDE.md → 核心功能 |
| 船只移动如何保持受损 | ATTACK_SYSTEM_GUIDE.md → 相对受损位置追踪 |
| 为什么对手看不到我的Miss | ATTACK_SYSTEM_GUIDE.md → Miss隐私保护 |
| 如何测试功能 | ATTACK_SYSTEM_GUIDE.md → 测试指南 |
| 项目整体架构 | PROJECT_ARCHITECTURE.md |
| 数据模型 | ATTACK_SYSTEM_GUIDE.md → 数据模型 |
| API接口 | PROJECT_ARCHITECTURE.md → API |
| 故障排除 | ATTACK_SYSTEM_GUIDE.md → 故障排除 |

### 我遇到了问题...

| 问题 | 解决方案 |
|------|----------|
| 攻击后不切换回合 | ATTACK_SYSTEM_GUIDE.md → 故障排除 → 问题2 |
| 标记不显示 | ATTACK_SYSTEM_GUIDE.md → 故障排除 → 问题2 |
| Failed to load game state | 运行 `./test.sh clean` |
| 船只移动后受损丢失 | ATTACK_SYSTEM_GUIDE.md → 故障排除 → 问题4 |
| 后端启动失败 | 检查端口占用：`lsof -ti:8080 \| xargs kill -9` |
| 数据不一致 | 清空数据：`./test.sh clean` |

---

## 文档维护

### 文档结构

```
BattleShip2/
├── README.md                    # 项目入口文档
├── DOCS_INDEX.md               # 本文档 - 文档索引
├── QUICKSTART.md               # 快速开始指南
├── PROJECT_ARCHITECTURE.md     # 架构文档
├── PROJECT_SUMMARY.md          # 功能总结
├── ATTACK_SYSTEM_GUIDE.md      # 攻击系统完整指南（最重要）
└── test.sh                     # 统一测试脚本
```

### 文档更新原则

1. ✅ **保持最新**：代码变更时同步更新文档
2. ✅ **避免重复**：相同内容只在一个文档中维护
3. ✅ **清晰引用**：通过链接引用其他文档
4. ✅ **实用优先**：提供可执行的示例和命令

---

## 给AI/大模型的说明

### 项目核心特点

1. **攻击标记系统**：
   - 静态记录（不是动态计算）
   - 重复攻击允许验证
   - Miss对对手隐私

2. **船只移动机制**：
   - 使用 `hitIndices` 追踪相对受损位置
   - 移动时自动更新 `board.hits` 坐标
   - 完全受损时自动沉没并移除

3. **数据模型关键点**：
   - `attacksByMeHits` + `attacksByMeMisses` 替代了旧的 `attackedByMe`
   - `ships` 只包含活跃船只，`sunkShips` 存储已沉没船只
   - `Ship.hitIndices` 使用Set<Integer>记录相对受损位置
   - 辅助方法需要 `@JsonIgnore` 防止序列化问题

4. **常见陷阱**：
   - ❌ 不要把getter方法序列化为字段（使用 `@JsonIgnore`）
   - ❌ 不要混用动态计算和静态记录
   - ❌ 不要忘记清空旧数据（`./test.sh clean`）
   - ✅ 重复攻击是允许的（不是bug）
   - ✅ Miss不显示给对手是正确的

### 关键代码位置

- **攻击处理**: `GameService.processAttack()` (line 95)
- **移动处理**: `GameService.processShipMove()` (line 236)
- **受损标记**: `GameService.markShipHit()` (line 217)
- **视图生成**: `ViewShapingService.createPlayerView()` (line 12)
- **前端显示**: `OpponentBoard.tsx` (line 25)

---

**最后更新**: 2025-10-18  
**维护者**: Battleship开发团队

