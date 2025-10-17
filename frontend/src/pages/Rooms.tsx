import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { roomsApi, authApi } from '../api/client';
import { setRooms, setLoading } from '../store/slices/roomsSlice';
import { clearAuth } from '../store/slices/authSlice';
import { setGameId } from '../store/slices/gameSlice';
import type { RootState } from '../store';
import { toast } from 'react-toastify';
import { useWebSocket } from '../hooks/useWebSocket';
import './Rooms.css';

const Rooms: React.FC = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { list: rooms, loading } = useSelector((state: RootState) => state.rooms);
  const auth = useSelector((state: RootState) => state.auth);
  const [subscribedRoom, setSubscribedRoom] = useState<string | null>(null);
  const ws = useWebSocket();

  useEffect(() => {
    ws.connect();
    loadRooms();
    
    const interval = setInterval(loadRooms, 5000);
    return () => clearInterval(interval);
  }, []);

  const loadRooms = async () => {
    try {
      const response = await roomsApi.getRooms();
      dispatch(setRooms(response.data));
    } catch (error: any) {
      console.error('Failed to load rooms:', error);
    }
  };

  const handleCreateRoom = async () => {
    dispatch(setLoading(true));
    try {
      const response = await roomsApi.createRoom();
      const roomId = response.data.roomId;
      
      // Subscribe to room
      subscribeToRoom(roomId);
      toast.success('Room created! Waiting for opponent...');
      await loadRooms();
    } catch (error: any) {
      toast.error('Failed to create room');
    } finally {
      dispatch(setLoading(false));
    }
  };

  const handleJoinRoom = async (roomId: string) => {
    if (loading) return; // Prevent double-clicks
    
    dispatch(setLoading(true));
    try {
      console.log('Joining room:', roomId);
      const response = await roomsApi.joinRoom(roomId);
      console.log('Join response:', response.data);
      
      const room = response.data;
      
      // If room is already in game, navigate directly to the game
      if (room.status === 'IN_GAME' && room.gameId) {
        console.log('Room is already in game, navigating to game:', room.gameId);
        dispatch(setGameId(room.gameId));
        toast.success('Joined game!');
        navigate(`/game/${room.gameId}`);
        return;
      }
      
      // Otherwise, subscribe to room events and wait for game to start
      subscribeToRoom(roomId);
      toast.success('Joined room!');
    } catch (error: any) {
      console.error('Join room error:', error);
      const errorMsg = error.response?.data?.error || 'Failed to join room';
      toast.error(errorMsg);
    } finally {
      dispatch(setLoading(false));
    }
  };

  const subscribeToRoom = (roomId: string) => {
    if (subscribedRoom) {
      return;
    }
    
    setSubscribedRoom(roomId);
    ws.subscribe(`/topic/rooms/${roomId}`, (message: any) => {
      console.log('Room event:', message);
      
      if (message.type === 'GAME_STARTED') {
        const gameId = message.payload.gameId;
        dispatch(setGameId(gameId));
        toast.success('Game started!');
        navigate(`/game/${gameId}`);
      }
    });
  };

  const handleLogout = async () => {
    try {
      await authApi.logout();
      dispatch(clearAuth());
      ws.disconnect();
      navigate('/login');
    } catch (error) {
      console.error('Logout error:', error);
    }
  };

  return (
    <div className="rooms-container">
      <div className="rooms-header">
        <h1>Battleship Rooms</h1>
        <div className="user-info">
          <span>{auth.email}</span>
          <button onClick={handleLogout} className="btn-secondary">Logout</button>
        </div>
      </div>
      
      <div className="rooms-actions">
        <button onClick={handleCreateRoom} className="btn-primary" disabled={loading}>
          Create New Room
        </button>
        <button onClick={loadRooms} className="btn-secondary">
          Refresh
        </button>
      </div>
      
      <div className="rooms-list">
        <h2>Available Rooms</h2>
        {rooms.length === 0 ? (
          <p className="no-rooms">No rooms available. Create one to start playing!</p>
        ) : (
          <div className="rooms-grid">
            {rooms.map((room) => (
              <div key={room.id} className="room-card">
                <h3>Room {room.id.slice(0, 8)}</h3>
                <p>Players: {room.playerIds.length}/2</p>
                <p>Status: {room.status}</p>
                <button
                  onClick={() => handleJoinRoom(room.id)}
                  className="btn-primary"
                  disabled={loading}
                >
                  Join Room
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default Rooms;


