import { createSlice } from '@reduxjs/toolkit';
import type { PayloadAction } from '@reduxjs/toolkit';

export interface ConnectionState {
  wsConnected: boolean;
  lastHeartbeatAt: number | null;
  reconnecting: boolean;
}

const initialState: ConnectionState = {
  wsConnected: false,
  lastHeartbeatAt: null,
  reconnecting: false,
};

const connectionSlice = createSlice({
  name: 'connection',
  initialState,
  reducers: {
    setWsConnected: (state, action: PayloadAction<boolean>) => {
      state.wsConnected = action.payload;
    },
    setLastHeartbeat: (state, action: PayloadAction<number>) => {
      state.lastHeartbeatAt = action.payload;
    },
    setReconnecting: (state, action: PayloadAction<boolean>) => {
      state.reconnecting = action.payload;
    },
  },
});

export const { setWsConnected, setLastHeartbeat, setReconnecting } = connectionSlice.actions;
export default connectionSlice.reducer;

