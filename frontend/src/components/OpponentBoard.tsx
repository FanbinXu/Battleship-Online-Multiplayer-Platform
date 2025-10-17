import React, { useMemo } from 'react';
import './Board.css';

interface Coord {
  r: number;
  c: number;
}

interface OpponentBoardProps {
  attacksByMe: {
    hits: Coord[];
    misses: Coord[];
  };
  sunkShips: Array<{ kind: string; length: number }>;
  onAttack: (target: Coord) => void;
  disabled: boolean;
}

const OpponentBoard: React.FC<OpponentBoardProps> = ({ attacksByMe, sunkShips, onAttack, disabled }) => {
  const board = useMemo(() => {
    const grid: string[][] = Array(10).fill(null).map(() => Array(10).fill(''));
    
    // Mark hits
    attacksByMe.hits.forEach(hit => {
      grid[hit.r][hit.c] = 'hit';
    });
    
    // Mark misses
    attacksByMe.misses.forEach(miss => {
      grid[miss.r][miss.c] = 'miss';
    });
    
    return grid;
  }, [attacksByMe]);

  const handleCellClick = (r: number, c: number) => {
    if (disabled) return;
    
    // Check if already attacked
    if (board[r][c] !== '') {
      return;
    }
    
    onAttack({ r, c });
  };

  return (
    <div>
      <div className="board">
        {board.map((row, r) => (
          <div key={r} className="board-row">
            {row.map((cell, c) => (
              <div
                key={`${r}-${c}`}
                className={`cell ${cell} ${!disabled && cell === '' ? 'attackable' : ''}`}
                onClick={() => handleCellClick(r, c)}
              >
                {cell === 'hit' && '💥'}
                {cell === 'miss' && '💦'}
              </div>
            ))}
          </div>
        ))}
      </div>
      
      {sunkShips.length > 0 && (
        <div className="sunk-ships">
          <h3>Enemy Ships Destroyed:</h3>
          <ul>
            {sunkShips.map((ship, index) => (
              <li key={index}>🚢 {ship.kind.replace('_', ' ')} ({ship.length})</li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
};

export default OpponentBoard;



