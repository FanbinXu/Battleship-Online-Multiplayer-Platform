#!/bin/bash

# Battleship游戏统一测试脚本

LOG_FILE="/tmp/battleship-backend.log"
FRONTEND_PORT=5174
BACKEND_PORT=8080

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 显示帮助
show_help() {
    echo -e "${BLUE}Battleship测试脚本${NC}"
    echo ""
    echo "用法: ./test.sh [command]"
    echo ""
    echo "命令:"
    echo "  ${GREEN}start${NC}      - 启动所有服务（数据库、后端、前端）"
    echo "  ${GREEN}stop${NC}       - 停止所有服务"
    echo "  ${GREEN}restart${NC}    - 重启所有服务"
    echo "  ${GREEN}clean${NC}      - 清空所有数据（Redis + MongoDB）"
    echo "  ${GREEN}logs${NC}       - 实时查看后端日志"
    echo "  ${GREEN}logs-attack${NC} - 查看攻击相关日志"
    echo "  ${GREEN}logs-turn${NC}  - 查看回合切换日志"
    echo "  ${GREEN}status${NC}     - 检查所有服务状态"
    echo "  ${GREEN}build${NC}      - 重新编译后端"
    echo "  ${GREEN}test${NC}       - 运行完整测试流程"
    echo ""
    echo "示例:"
    echo "  ./test.sh clean     # 清空数据"
    echo "  ./test.sh restart   # 重启服务"
    echo "  ./test.sh logs      # 查看日志"
}

# 检查服务状态
check_status() {
    echo -e "${BLUE}检查服务状态...${NC}"
    echo ""
    
    # 检查Docker
    if docker ps | grep -q bs-redis; then
        echo -e "${GREEN}✅ Redis${NC} - 运行中"
    else
        echo -e "${RED}❌ Redis${NC} - 未运行"
    fi
    
    if docker ps | grep -q bs-mongo; then
        echo -e "${GREEN}✅ MongoDB${NC} - 运行中"
    else
        echo -e "${RED}❌ MongoDB${NC} - 未运行"
    fi
    
    # 检查后端
    if lsof -ti:$BACKEND_PORT > /dev/null 2>&1; then
        if curl -s http://localhost:$BACKEND_PORT/actuator/health > /dev/null 2>&1; then
            echo -e "${GREEN}✅ Backend${NC} - 运行中 (http://localhost:$BACKEND_PORT)"
        else
            echo -e "${YELLOW}⚠️  Backend${NC} - 进程存在但未响应"
        fi
    else
        echo -e "${RED}❌ Backend${NC} - 未运行"
    fi
    
    # 检查前端
    if lsof -ti:$FRONTEND_PORT > /dev/null 2>&1; then
        echo -e "${GREEN}✅ Frontend${NC} - 运行中 (http://localhost:$FRONTEND_PORT)"
    else
        echo -e "${RED}❌ Frontend${NC} - 未运行"
    fi
    
    echo ""
}

# 启动数据库
start_db() {
    echo -e "${BLUE}启动数据库...${NC}"
    cd infra
    docker-compose -f docker-compose.dev.yml up -d
    cd ..
    sleep 2
    echo -e "${GREEN}✅ 数据库已启动${NC}"
}

# 停止数据库
stop_db() {
    echo -e "${YELLOW}停止数据库...${NC}"
    cd infra
    docker-compose -f docker-compose.dev.yml down
    cd ..
    echo -e "${GREEN}✅ 数据库已停止${NC}"
}

# 清空数据
clean_data() {
    echo -e "${YELLOW}清空所有数据...${NC}"
    
    # 清空Redis
    redis-cli FLUSHALL > /dev/null 2>&1
    echo -e "${GREEN}✅ Redis数据已清空${NC}"
    
    # 清空MongoDB
    cd infra
    docker-compose -f docker-compose.dev.yml down -v > /dev/null 2>&1
    docker-compose -f docker-compose.dev.yml up -d > /dev/null 2>&1
    cd ..
    echo -e "${GREEN}✅ MongoDB数据已清空${NC}"
    
    echo ""
    echo -e "${BLUE}提示: 请刷新浏览器并开始新游戏${NC}"
}

# 编译后端
build_backend() {
    echo -e "${BLUE}编译后端...${NC}"
    cd backend
    mvn clean package -DskipTests -q
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ 后端编译成功${NC}"
    else
        echo -e "${RED}❌ 后端编译失败${NC}"
        cd ..
        exit 1
    fi
    cd ..
}

# 启动后端
start_backend() {
    echo -e "${BLUE}启动后端...${NC}"
    
    # 停止旧进程
    lsof -ti:$BACKEND_PORT | xargs kill -9 > /dev/null 2>&1
    sleep 1
    
    # 启动新进程
    cd backend
    java -jar target/backend-0.0.1-SNAPSHOT.jar > $LOG_FILE 2>&1 &
    cd ..
    
    # 等待启动
    echo -n "等待后端启动"
    for i in {1..10}; do
        sleep 1
        echo -n "."
        if curl -s http://localhost:$BACKEND_PORT/actuator/health > /dev/null 2>&1; then
            echo ""
            echo -e "${GREEN}✅ 后端已启动${NC} (http://localhost:$BACKEND_PORT)"
            return
        fi
    done
    
    echo ""
    echo -e "${RED}❌ 后端启动超时${NC}"
    echo -e "${YELLOW}查看日志: tail -f $LOG_FILE${NC}"
}

# 停止后端
stop_backend() {
    echo -e "${YELLOW}停止后端...${NC}"
    lsof -ti:$BACKEND_PORT | xargs kill -9 > /dev/null 2>&1
    echo -e "${GREEN}✅ 后端已停止${NC}"
}

# 查看日志
view_logs() {
    echo -e "${BLUE}实时查看后端日志...${NC}"
    echo -e "${YELLOW}按 Ctrl+C 退出${NC}"
    echo ""
    tail -f $LOG_FILE
}

# 查看攻击日志
view_attack_logs() {
    echo -e "${BLUE}实时查看攻击相关日志...${NC}"
    echo -e "${YELLOW}按 Ctrl+C 退出${NC}"
    echo ""
    tail -f $LOG_FILE | grep --line-buffered -E "ATTACK|attacksByMe|SAVED|LOADED|hit|miss"
}

# 查看回合日志
view_turn_logs() {
    echo -e "${BLUE}实时查看回合切换日志...${NC}"
    echo -e "${YELLOW}按 Ctrl+C 退出${NC}"
    echo ""
    tail -f $LOG_FILE | grep --line-buffered -E "Switching turn|After switchTurn|Turn:|Current player"
}

# 启动所有服务
start_all() {
    echo -e "${BLUE}=== 启动所有服务 ===${NC}"
    echo ""
    
    start_db
    sleep 2
    
    build_backend
    start_backend
    
    echo ""
    echo -e "${GREEN}=== 所有服务已启动 ===${NC}"
    echo ""
    echo "访问游戏："
    echo -e "  前端: ${BLUE}http://localhost:$FRONTEND_PORT${NC}"
    echo -e "  后端: ${BLUE}http://localhost:$BACKEND_PORT${NC}"
    echo ""
    echo "提示："
    echo "  - 前端需要手动启动: cd frontend && npm run dev"
    echo "  - 查看日志: ./test.sh logs"
    echo "  - 检查状态: ./test.sh status"
}

# 停止所有服务
stop_all() {
    echo -e "${YELLOW}=== 停止所有服务 ===${NC}"
    echo ""
    
    stop_backend
    stop_db
    
    echo ""
    echo -e "${GREEN}=== 所有服务已停止 ===${NC}"
}

# 重启所有服务
restart_all() {
    echo -e "${BLUE}=== 重启所有服务 ===${NC}"
    echo ""
    
    stop_backend
    sleep 1
    
    build_backend
    start_backend
    
    echo ""
    echo -e "${GREEN}=== 服务已重启 ===${NC}"
}

# 运行完整测试
run_test() {
    echo -e "${BLUE}=== 运行完整测试流程 ===${NC}"
    echo ""
    
    # 1. 检查状态
    check_status
    
    # 2. 清空数据
    echo -e "${YELLOW}清空旧数据...${NC}"
    redis-cli FLUSHALL > /dev/null 2>&1
    
    # 3. 重启后端
    echo -e "${YELLOW}重启后端...${NC}"
    restart_all
    
    # 4. 显示测试指引
    echo ""
    echo -e "${GREEN}=== 准备测试 ===${NC}"
    echo ""
    echo "请按以下步骤测试："
    echo ""
    echo "1. 刷新浏览器: http://localhost:$FRONTEND_PORT"
    echo "2. 创建房间并开始游戏"
    echo "3. 进行攻击测试"
    echo ""
    echo "测试清单:"
    echo "  [ ] 攻击后显示标记（💥 或 💦）"
    echo "  [ ] 攻击后切换回合"
    echo "  [ ] 标记只在Opponent Board显示"
    echo "  [ ] 可以重复攻击验证"
    echo "  [ ] 移动船只时受损位置跟随"
    echo "  [ ] 船只完全受损后沉没并消失"
    echo ""
    echo -e "查看实时日志: ${BLUE}./test.sh logs${NC}"
}

# 主逻辑
case "${1}" in
    start)
        start_all
        ;;
    stop)
        stop_all
        ;;
    restart)
        restart_all
        ;;
    clean)
        clean_data
        ;;
    logs)
        view_logs
        ;;
    logs-attack)
        view_attack_logs
        ;;
    logs-turn)
        view_turn_logs
        ;;
    status)
        check_status
        ;;
    build)
        build_backend
        ;;
    test)
        run_test
        ;;
    *)
        show_help
        ;;
esac

