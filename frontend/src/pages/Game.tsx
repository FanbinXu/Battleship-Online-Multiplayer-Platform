import React, { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { gameApi } from '../api/client';
import { setYourView, setGameId, setSuggestion, setLastEventSeq } from '../store/slices/gameSlice';
import type { RootState } from '../store';
import { toast } from 'react-toastify';
import { useWebSocket } from '../hooks/useWebSocket';
import MyBoard from '../components/MyBoard';
import OpponentBoard from '../components/OpponentBoard';
import AiSuggestionPanel from '../components/AiSuggestionPanel';
import './Game.css';

const Game: React.FC = () => {
  const { gameId } = useParams<{ gameId: string }>();
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { yourView, suggestion } = useSelector((state: RootState) => state.game);
  const auth = useSelector((state: RootState) => state.auth);
  const { wsConnected } = useSelector((state: RootState) => state.connection);
  const ws = useWebSocket();
  const [roomId, setRoomId] = useState<string | null>(null);
  const [actionMode, setActionMode] = useState<'attack' | 'move'>('attack');

  useEffect(() => {
    if (!gameId) return;
    
    dispatch(setGameId(gameId));
    ws.connect();
    loadGameState();
  }, [gameId]);

  useEffect(() => {
    if (yourView && roomId && wsConnected) {
      subscribeToRoom(roomId);
    }
  }, [yourView, roomId, wsConnected]);

  const loadGameState = async () => {
    if (!gameId) return;
    
    try {
      const response = await gameApi.getGameState(gameId);
      const { yourView: view, roomId: responseRoomId } = response.data;
      
      dispatch(setYourView(view));
      
      // Set room ID for WebSocket subscription
      if (responseRoomId && !roomId) {
        setRoomId(responseRoomId);
      }
    } catch (error: any) {
      toast.error('Failed to load game state');
      console.error(error);
    }
  };

  const subscribeToRoom = (roomId: string) => {
    ws.subscribe(`/topic/rooms/${roomId}`, handleGameEvent);
  };

  const handleGameEvent = useCallback((event: any) => {
    console.log('Game event:', event);
    
    switch (event.type) {
      case 'STATE_UPDATED':
        // Reload game state
        loadGameState();
        if (event.eventSeq) {
          dispatch(setLastEventSeq(event.eventSeq));
        }
        break;
        
      case 'SUGGESTION_READY':
        dispatch(setSuggestion(event.payload.suggestion));
        toast.info('AI suggestion ready!');
        break;
        
      case 'GAME_ENDED':
        toast.success(`Game ended! Winner: ${event.payload.winnerPlayerId === auth.userId ? 'You' : 'Opponent'}`);
        setTimeout(() => navigate('/rooms'), 3000);
        break;
        
      case 'ACTION_REJECTED':
        toast.error(`Action rejected: ${event.payload.reason}`);
        break;
    }
  }, [dispatch, auth.userId, navigate]);

  const handleAttack = async (target: { r: number; c: number }) => {
    if (!gameId || !yourView) return;
    
    if (yourView.currentPlayerId !== auth.userId) {
      toast.warning("Not your turn!");
      return;
    }

    try {
      const actionId = `attack-${Date.now()}-${Math.random()}`;
      console.log('[Game] ============ ATTACK START ============');
      console.log('[Game] Sending attack to:', target);
      console.log('[Game] Current turn:', yourView.turn);
      console.log('[Game] Current attacksByMe BEFORE attack:', yourView.opponent?.revealed?.attacksByMe);
      
      const response = await gameApi.attack(gameId, actionId, yourView.turn, target);
      
      console.log('[Game] ============ ATTACK RESPONSE ============');
      console.log('[Game] Full response:', JSON.stringify(response.data, null, 2));
      console.log('[Game] Response success:', response.data.success);
      console.log('[Game] Response isHit:', response.data.isHit);
      console.log('[Game] Response target:', response.data.target);
      
      if (response.data.success) {
        const isHit = response.data.isHit;
        toast.success(isHit ? '🎯 Hit!' : '💦 Miss!');
        
        if (response.data.sunkShip) {
          toast.success(`🚢 ${response.data.sunkShip.kind} sunk!`);
        }
        
        if (response.data.yourView) {
          console.log('[Game] ============ UPDATING VIEW ============');
          console.log('[Game] New yourView full object:', response.data.yourView);
          console.log('[Game] New opponent object:', response.data.yourView.opponent);
          console.log('[Game] New revealed object:', response.data.yourView.opponent?.revealed);
          console.log('[Game] New attacksByMe object:', response.data.yourView.opponent?.revealed?.attacksByMe);
          console.log('[Game] New attacksByMe.hits:', response.data.yourView.opponent?.revealed?.attacksByMe?.hits);
          console.log('[Game] New attacksByMe.misses:', response.data.yourView.opponent?.revealed?.attacksByMe?.misses);
          
          dispatch(setYourView(response.data.yourView));
          
          console.log('[Game] ============ VIEW UPDATED ============');
        } else {
          console.error('[Game] ❌ No yourView in response!');
        }
      }
    } catch (error: any) {
      console.error('[Game] ============ ATTACK ERROR ============');
      console.error('[Game] Error:', error);
      console.error('[Game] Error response:', error.response?.data);
      toast.error(error.response?.data?.error || 'Attack failed');
    }
  };

  const handleRequestSuggestion = async () => {
    if (!gameId) return;
    
    try {
      await gameApi.requestSuggestion(gameId);
      toast.info('Requesting AI suggestion...');
    } catch (error: any) {
      toast.error('Failed to request suggestion');
    }
  };

  const handleApplySuggestion = () => {
    if (suggestion && suggestion.detail?.target) {
      handleAttack(suggestion.detail.target);
      dispatch(setSuggestion(null));
    }
  };

  const handleShipMove = async (shipId: string, newPosition: { r: number; c: number }, isHorizontal: boolean) => {
    if (!gameId || !yourView) return;
    
    if (yourView.currentPlayerId !== auth.userId) {
      toast.warning("Not your turn!");
      return;
    }

    try {
      const actionId = `move-${Date.now()}-${Math.random()}`;
      const response = await gameApi.moveShip(gameId, actionId, yourView.turn, shipId, newPosition, isHorizontal);
      
      if (response.data.success) {
        toast.success('🚢 Ship moved!');
        
        if (response.data.yourView) {
          dispatch(setYourView(response.data.yourView));
        }
        
        // Switch back to attack mode after move
        setActionMode('attack');
      }
    } catch (error: any) {
      toast.error(error.response?.data?.reason || error.response?.data?.error || 'Move failed');
    }
  };

  if (!yourView) {
    return <div className="loading">Loading game...</div>;
  }

  // Debug: Log the complete yourView data
  console.log('[Game] Complete yourView:', yourView);
  console.log('[Game] Opponent revealed:', yourView.opponent?.revealed);
  console.log('[Game] AttacksByMe:', yourView.opponent?.revealed?.attacksByMe);

  const isMyTurn = yourView.currentPlayerId === auth.userId;

  return (
    <div className="game-container">
      <div className="game-header">
        <h1>Battleship Game</h1>
        <div className="game-status">
          <span className={isMyTurn ? 'turn-active' : 'turn-waiting'}>
            {isMyTurn ? '🎯 Your Turn' : '⏳ Opponent\'s Turn'}
          </span>
          <span>Turn: {yourView.turn}</span>
        </div>
      </div>

      {isMyTurn && !yourView.winnerPlayerId && (
        <div className="action-selector">
          <button 
            className={`action-btn ${actionMode === 'attack' ? 'active' : ''}`}
            onClick={() => setActionMode('attack')}
          >
            ⚔️ Attack
          </button>
          <button 
            className={`action-btn ${actionMode === 'move' ? 'active' : ''}`}
            onClick={() => setActionMode('move')}
          >
            🚢 Move Ship
          </button>
          <div className="action-hint">
            {actionMode === 'attack' 
              ? 'Click on opponent\'s board to attack' 
              : 'Drag your ships to move them (valid: blue, invalid: red)'}
          </div>
        </div>
      )}

      <div className="game-boards">
        <div className="board-section">
          <h2>Your Board</h2>
          <MyBoard
            ships={yourView.me.board.ships}
            hits={yourView.me.board.hits}
            misses={yourView.me.board.misses}
            canMove={isMyTurn && actionMode === 'move' && !yourView.winnerPlayerId}
            onShipMove={handleShipMove}
          />
        </div>

        <div className="board-section">
          <h2>Opponent Board</h2>
          <OpponentBoard
            attacksByMe={yourView.opponent.revealed.attacksByMe}
            sunkShips={yourView.opponent.revealed.sunkShips}
            onAttack={handleAttack}
            disabled={!isMyTurn || yourView.winnerPlayerId !== undefined || actionMode === 'move'}
          />
        </div>
      </div>

      <AiSuggestionPanel
        suggestion={suggestion}
        onRequest={handleRequestSuggestion}
        onApply={handleApplySuggestion}
        disabled={!isMyTurn || yourView.winnerPlayerId !== undefined}
      />

      {yourView.winnerPlayerId && (
        <div className="game-over">
          <h2>{yourView.winnerPlayerId === auth.userId ? '🎉 You Won!' : '😔 You Lost'}</h2>
          <button onClick={() => navigate('/rooms')} className="btn-primary">
            Back to Rooms
          </button>
        </div>
      )}
    </div>
  );
};

export default Game;

