import { createSlice } from '@reduxjs/toolkit';
import type { PayloadAction } from '@reduxjs/toolkit';

interface Coord {
  r: number;
  c: number;
}

interface Ship {
  id: string;
  kind: string;
  cells: Coord[];
  sunk: boolean;
}

interface Board {
  ships: Ship[];
  hits: Coord[];
  misses: Coord[];
}

interface OpponentRevealed {
  attacksByMe: {
    hits: Coord[];
    misses: Coord[];
  };
  sunkShips: Array<{ kind: string; length: number }>;
}

interface YourView {
  me: {
    board: Board;
  };
  opponent: {
    revealed: OpponentRevealed;
  };
  turn: number;
  currentPlayerId: string;
  stateVersion: number;
  winnerPlayerId?: string;
}

interface Suggestion {
  type: string;
  confidence: number;
  detail: {
    target: Coord;
  };
}

export interface GameState {
  gameId: string | null;
  yourView: YourView | null;
  suggestion: Suggestion | null;
  lastEventSeq: number;
  loading: boolean;
  error: string | null;
}

const initialState: GameState = {
  gameId: null,
  yourView: null,
  suggestion: null,
  lastEventSeq: 0,
  loading: false,
  error: null,
};

const gameSlice = createSlice({
  name: 'game',
  initialState,
  reducers: {
    setGameId: (state, action: PayloadAction<string>) => {
      state.gameId = action.payload;
    },
    setYourView: (state, action: PayloadAction<YourView>) => {
      state.yourView = action.payload;
    },
    setSuggestion: (state, action: PayloadAction<Suggestion | null>) => {
      state.suggestion = action.payload;
    },
    setLastEventSeq: (state, action: PayloadAction<number>) => {
      state.lastEventSeq = action.payload;
    },
    setLoading: (state, action: PayloadAction<boolean>) => {
      state.loading = action.payload;
    },
    setError: (state, action: PayloadAction<string | null>) => {
      state.error = action.payload;
    },
    clearGame: (state) => {
      state.gameId = null;
      state.yourView = null;
      state.suggestion = null;
      state.lastEventSeq = 0;
    },
  },
});

export const { setGameId, setYourView, setSuggestion, setLastEventSeq, setLoading, setError, clearGame } = gameSlice.actions;
export default gameSlice.reducer;

