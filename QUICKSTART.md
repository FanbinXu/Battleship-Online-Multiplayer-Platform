# Battleship - Quick Start Guide

## Prerequisites Check

Before starting, ensure you have:
- ✅ Java 21 or higher (`java -version`)
- ✅ Node.js 18 or higher (`node -v`)
- ✅ Docker and Docker Compose (`docker --version`)
- ✅ Maven 3.8+ (`mvn -v`)

## 5-Minute Setup

### Step 1: Start Infrastructure (30 seconds)

```bash
cd infra
docker compose -f docker-compose.dev.yml up -d
```

Verify:
```bash
docker ps
# You should see bs-redis and bs-mongo running
```

### Step 2: Start Backend (2 minutes)

Open a new terminal:

```bash
cd backend
mvn clean package -DskipTests
mvn spring-boot:run
```

Wait for: `Started BattleshipApplication in X seconds`

Backend is ready at: http://localhost:8080

### Step 3: Start Frontend (1 minute)

Open another terminal:

```bash
cd frontend
npm install
npm run dev
```

Frontend is ready at: http://localhost:5173

### Step 4: Play! (1 minute)

1. Open http://localhost:5173 in your browser
2. Click "Register" and create an account (e.g., `player1@test.com` / `password123`)
3. Click "Create New Room"
4. Open another browser/incognito window
5. Register another account (e.g., `player2@test.com` / `password123`)
6. Click on the waiting room to join
7. Game starts automatically! 🎮

## First Game Guide

### Your Turn Indicator
- 🎯 **Your Turn** (green) - Click opponent's board to attack
- ⏳ **Opponent's Turn** (orange) - Wait for your turn

### Making an Attack
1. When it's your turn, click any cell on the **Opponent Board** (right side)
2. 💥 = Hit, 💦 = Miss
3. Turn automatically switches to opponent

### Using AI Suggestions
1. Click "Get AI Suggestion" button
2. Wait a few seconds
3. Review the suggested coordinates
4. Click "Apply Suggestion" to auto-attack that cell

### Winning the Game
- Sink all 5 enemy ships to win!
- Ships: Carrier(5), Battleship(4), Cruiser(3), Submarine(3), Destroyer(2)
- Sunk ships appear in "Enemy Ships Destroyed" list

## Troubleshooting

### Backend won't start
```bash
# Check if ports are available
lsof -i :8080  # Should be empty
lsof -i :27017 # MongoDB
lsof -i :6379  # Redis

# Restart infrastructure
cd infra
docker compose -f docker-compose.dev.yml down
docker compose -f docker-compose.dev.yml up -d
```

### Frontend won't start
```bash
# Clear node_modules and reinstall
cd frontend
rm -rf node_modules package-lock.json
npm install
npm run dev
```

### Can't connect to game
1. Check browser console for errors (F12)
2. Verify backend is running (visit http://localhost:8080/api/health)
3. Clear browser cookies and re-login

### WebSocket not connecting
- Check CORS settings in backend
- Verify firewall isn't blocking port 8080
- Try the SockJS fallback (should be automatic)

## Configuration

### Enable OpenAI Suggestions

Edit `backend/src/main/resources/application.yml`:

```yaml
openai:
  api:
    key: sk-your-actual-openai-api-key-here
```

Restart backend. AI suggestions will now use GPT-4 instead of local heuristic.

### Change Ports

**Backend:**
Edit `backend/src/main/resources/application.yml`:
```yaml
server:
  port: 9000
```

**Frontend:**
Edit `frontend/vite.config.ts`:
```typescript
server: {
  port: 3000
}
```

Update `frontend/.env`:
```
VITE_API_URL=http://localhost:9000
VITE_WS_URL=http://localhost:9000/ws
```

## Next Steps

- Read [README.md](README.md) for full documentation
- Explore the codebase
- Add custom game modes
- Implement manual ship placement
- Add chat functionality

## Stop Everything

```bash
# Frontend: Ctrl+C in terminal

# Backend: Ctrl+C in terminal

# Infrastructure:
cd infra
docker compose -f docker-compose.dev.yml down
```

## Clean Slate

To completely reset:

```bash
# Stop and remove all data
cd infra
docker compose -f docker-compose.dev.yml down -v

# Clear backend build
cd ../backend
mvn clean

# Clear frontend build
cd ../frontend
rm -rf node_modules dist

# Start fresh from Step 1
```

Happy gaming! 🎮🚢



