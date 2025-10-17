import { createSlice } from '@reduxjs/toolkit';
import type { PayloadAction } from '@reduxjs/toolkit';

interface Room {
  id: string;
  status: string;
  playerIds: string[];
  createdAt: string;
  updatedAt: string;
}

interface RoomsState {
  list: Room[];
  loading: boolean;
  error: string | null;
}

const initialState: RoomsState = {
  list: [],
  loading: false,
  error: null,
};

const roomsSlice = createSlice({
  name: 'rooms',
  initialState,
  reducers: {
    setRooms: (state, action: PayloadAction<Room[]>) => {
      state.list = action.payload;
    },
    setLoading: (state, action: PayloadAction<boolean>) => {
      state.loading = action.payload;
    },
    setError: (state, action: PayloadAction<string | null>) => {
      state.error = action.payload;
    },
  },
});

export const { setRooms, setLoading, setError } = roomsSlice.actions;
export default roomsSlice.reducer;

