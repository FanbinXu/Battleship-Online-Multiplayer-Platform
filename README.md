# Battleship Online PvP Platform

A full-stack, real-time multiplayer Battleship game with AI-powered attack suggestions.

## Features

- 🎮 Real-time PvP gameplay via WebSocket (STOMP)
- 🔐 Email/password authentication with JWT
- 🤖 AI-powered attack suggestions (OpenAI integration with fallback heuristic)
- 🎯 Advanced Battleship rules (10x10 board, 5 ships)
- 🚢 Dynamic ship movement during gameplay
- 💥 Attack marking system (Hit/Miss tracking)
- 🔄 Re-attack mechanism to verify opponent ship movement
- 🎯 Relative damage tracking (damage persists when ships move)
- 🔒 Miss privacy (opponents don't see your misses)
- 📊 Event sourcing architecture
- 💾 Redis for hot state, MongoDB for persistence
- 🎨 Modern, responsive UI

## Tech Stack

### Backend
- **Spring Boot 3.5.6** - Java 21
- **Spring Security** - JWT authentication
- **Spring WebSocket** - STOMP protocol
- **MongoDB** - Event sourcing, user data
- **Redis** - Hot game state, sequences
- **OpenAI API** - AI suggestions (optional)

### Frontend
- **React 19** - TypeScript 5.9
- **Redux Toolkit** - State management
- **React Router** - Navigation
- **STOMP.js** - WebSocket client
- **React Toastify** - Notifications
- **Vite** - Build tool

## Prerequisites

- Java 21+
- Node.js 18+
- Docker & Docker Compose
- Maven 3.8+

## Quick Start

### 1. Start Infrastructure

```bash
cd infra
docker compose -f docker-compose.dev.yml up -d
```

This starts:
- MongoDB on port 27017
- Redis on port 6379

### 2. Start Backend

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

Backend runs on `http://localhost:8080`

### 3. Start Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on `http://localhost:5173`

### 4. Play!

1. Open `http://localhost:5173` in your browser
2. Register a new account
3. Create a room or join an existing one
4. Wait for an opponent
5. Play Battleship!

## Environment Variables

### Backend (`backend/src/main/resources/application.yml`)

```yaml
MONGO_URI: mongodb://localhost:27017/battleship
REDIS_HOST: localhost
REDIS_PORT: 6379
JWT_SECRET: your_secret_key_here
JWT_EXPIRATION: 86400  # 24 hours in seconds
OPENAI_API_KEY: sk-...  # Optional
ROOM_EMPTY_TTL_SEC: 60
TURN_TIMEOUT_SEC: 30
RECONNECT_GRACE_SEC: 60
```

### Frontend (`.env` in frontend/)

```env
VITE_API_URL=http://localhost:8080
VITE_WS_URL=http://localhost:8080/ws
```

## Game Rules

### Board & Fleet
- 10x10 grid (0-indexed)
- 5 ships per player:
  - Carrier (5 cells)
  - Battleship (4 cells)
  - Cruiser (3 cells)
  - Submarine (3 cells)
  - Destroyer (2 cells)

### Gameplay
- Ships are placed automatically at game start
- Players take turns attacking one cell at a time
- No re-attacking the same cell
- Ship is sunk when all its cells are hit
- Game ends when all ships of one player are sunk

### Fog of War
- You see your full board (ships, hits, misses)
- You only see your attacks on opponent's board (hits/misses)
- Sunk enemy ships are revealed

## API Endpoints

### Authentication
- `POST /auth/register` - Register new user
- `POST /auth/login` - Login (sets JWT cookie)
- `POST /auth/logout` - Logout

### Rooms
- `GET /api/rooms` - List waiting rooms
- `POST /api/rooms` - Create room
- `POST /api/rooms/{id}/join` - Join room
- `POST /api/rooms/{id}/leave` - Leave room

### Game
- `GET /api/games/{id}` - Get game state (your view)
- `POST /api/games/{id}/action/attack` - Attack a coordinate
- `POST /api/games/{id}/suggest` - Request AI suggestion (async)

### WebSocket
- Connect: `/ws` (with SockJS fallback)
- Subscribe: `/topic/rooms/{roomId}`
- Events: `GAME_STARTED`, `STATE_UPDATED`, `SUGGESTION_READY`, `GAME_ENDED`, `ACTION_REJECTED`

## AI Suggestions

The platform includes AI-powered attack suggestions:

1. **OpenAI Mode**: If `OPENAI_API_KEY` is configured, uses GPT-4 for intelligent suggestions
2. **Fallback Heuristic**: Local algorithm that prioritizes cells adjacent to previous hits

To request a suggestion:
1. Click "Get AI Suggestion" during your turn
2. Wait for the suggestion to appear
3. Review the target and confidence score
4. Click "Apply Suggestion" to attack that cell

## Architecture Highlights

### Event Sourcing
- All game actions stored as events in MongoDB
- Snapshots created periodically for fast recovery
- Full game replay capability

### Real-time Communication
- STOMP over WebSocket for bidirectional messaging
- Heartbeats every 10 seconds
- Auto-reconnect on disconnect
- Room-scoped broadcasts

### State Management
- **Redis**: Hot game state, event sequences, idempotency keys
- **MongoDB**: Users, rooms, games, events, snapshots
- **Frontend Redux**: Local UI state synchronized with backend

### Security
- BCrypt password hashing
- JWT authentication (HttpOnly cookies)
- Protected routes on frontend
- Request validation on backend

## Development

### Backend Development
```bash
cd backend
mvn spring-boot:run
# Auto-reload with spring-boot-devtools
```

### Frontend Development
```bash
cd frontend
npm run dev
# Hot reload enabled
```

### Running Tests
```bash
# Backend
cd backend
mvn test

# Frontend
cd frontend
npm test
```

## Troubleshooting

### WebSocket Connection Issues
- Ensure backend is running on port 8080
- Check CORS configuration in `SecurityConfig.java`
- Verify SockJS fallback is working

### Authentication Issues
- Clear browser cookies
- Check JWT_SECRET is set
- Verify MongoDB is running

### Game State Sync Issues
- Check Redis connection
- Verify event sequences are incrementing
- Review WebSocket subscription logs

## Project Structure

```
BattleShip2/
├── backend/
│   └── src/main/java/app/battleship/
│       ├── api/           # REST controllers
│       ├── config/        # Spring config
│       ├── model/         # Domain models
│       ├── persist/       # Repositories
│       ├── security/      # JWT & auth
│       └── service/       # Business logic
├── frontend/
│   └── src/
│       ├── api/           # API client
│       ├── components/    # React components
│       ├── hooks/         # Custom hooks
│       ├── pages/         # Page components
│       └── store/         # Redux store
└── infra/
    └── docker-compose.dev.yml
```

## Documentation

- **[QUICKSTART.md](QUICKSTART.md)** - 快速开始指南
- **[PROJECT_ARCHITECTURE.md](PROJECT_ARCHITECTURE.md)** - 项目架构详解
- **[PROJECT_SUMMARY.md](PROJECT_SUMMARY.md)** - 项目功能总结
- **[ATTACK_SYSTEM_GUIDE.md](ATTACK_SYSTEM_GUIDE.md)** - 攻击标记系统完整指南（必读）

## Testing

Use the unified test script for easy testing and debugging:

```bash
# Make script executable (first time only)
chmod +x test.sh

# Run complete test setup
./test.sh test

# View real-time backend logs
./test.sh logs

# View attack-specific logs
./test.sh logs-attack

# Check service status
./test.sh status

# Clean all data (for fresh start)
./test.sh clean

# Restart backend
./test.sh restart

# Show all commands
./test.sh
```

Backend logs are saved to `/tmp/battleship-backend.log`

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## License

MIT License

## Credits

Built with ❤️ using Spring Boot, React, and OpenAI.


