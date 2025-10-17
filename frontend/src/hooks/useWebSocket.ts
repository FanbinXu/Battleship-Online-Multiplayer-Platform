import { useEffect, useRef, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import type { IFrame } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useDispatch } from 'react-redux';
import { setWsConnected, setReconnecting } from '../store/slices/connectionSlice';

const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws';

export const useWebSocket = (onMessage?: (message: any) => void) => {
  const clientRef = useRef<Client | null>(null);
  const dispatch = useDispatch();
  const reconnectAttempts = useRef(0);
  const maxReconnectAttempts = 5;

  const connect = useCallback(() => {
    if (clientRef.current?.active) {
      return;
    }

    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL) as any,
      
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      
      onConnect: (frame: IFrame) => {
        console.log('WebSocket connected:', frame);
        dispatch(setWsConnected(true));
        dispatch(setReconnecting(false));
        reconnectAttempts.current = 0;
      },
      
      onDisconnect: () => {
        console.log('WebSocket disconnected');
        dispatch(setWsConnected(false));
      },
      
      onStompError: (frame: IFrame) => {
        console.error('STOMP error:', frame);
        dispatch(setWsConnected(false));
        
        if (reconnectAttempts.current < maxReconnectAttempts) {
          reconnectAttempts.current++;
          dispatch(setReconnecting(true));
        }
      },
      
      onWebSocketClose: () => {
        console.log('WebSocket connection closed');
        dispatch(setWsConnected(false));
      },
    });

    client.activate();
    clientRef.current = client;
  }, [dispatch]);

  const disconnect = useCallback(() => {
    if (clientRef.current) {
      clientRef.current.deactivate();
      clientRef.current = null;
      dispatch(setWsConnected(false));
    }
  }, [dispatch]);

  const subscribe = useCallback((destination: string, callback: (message: any) => void) => {
    if (!clientRef.current?.active) {
      console.warn('Cannot subscribe: WebSocket not connected');
      return;
    }

    return clientRef.current.subscribe(destination, (message) => {
      try {
        const payload = JSON.parse(message.body);
        callback(payload);
        if (onMessage) {
          onMessage(payload);
        }
      } catch (error) {
        console.error('Error parsing message:', error);
      }
    });
  }, [onMessage]);

  const publish = useCallback((destination: string, body: any) => {
    if (!clientRef.current?.active) {
      console.warn('Cannot publish: WebSocket not connected');
      return;
    }

    clientRef.current.publish({
      destination,
      body: JSON.stringify(body),
    });
  }, []);

  useEffect(() => {
    return () => {
      disconnect();
    };
  }, [disconnect]);

  return {
    connect,
    disconnect,
    subscribe,
    publish,
    isConnected: clientRef.current?.active || false,
  };
};


