import { createSlice } from '@reduxjs/toolkit';
import type { PayloadAction } from '@reduxjs/toolkit';

interface AuthState {
  userId: string | null;
  email: string | null;
  jwtPresent: boolean;
  loading: boolean;
  error: string | null;
}

const initialState: AuthState = {
  userId: null,
  email: null,
  jwtPresent: false,
  loading: false,
  error: null,
};

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setAuth: (state, action: PayloadAction<{ userId: string; email: string }>) => {
      state.userId = action.payload.userId;
      state.email = action.payload.email;
      state.jwtPresent = true;
      state.error = null;
    },
    setLoading: (state, action: PayloadAction<boolean>) => {
      state.loading = action.payload;
    },
    setError: (state, action: PayloadAction<string | null>) => {
      state.error = action.payload;
    },
    clearAuth: (state) => {
      state.userId = null;
      state.email = null;
      state.jwtPresent = false;
      state.error = null;
    },
  },
});

export const { setAuth, setLoading, setError, clearAuth } = authSlice.actions;
export default authSlice.reducer;

