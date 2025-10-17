import React, { useMemo } from 'react';
import './Board.css';

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

interface MyBoardProps {
  ships: Ship[];
  hits: Coord[];
  misses: Coord[];
}

const MyBoard: React.FC<MyBoardProps> = ({ ships, hits, misses }) => {
  const board = useMemo(() => {
    const grid: string[][] = Array(10).fill(null).map(() => Array(10).fill(''));
    
    // Mark ship positions
    ships.forEach(ship => {
      ship.cells.forEach(cell => {
        grid[cell.r][cell.c] = ship.sunk ? 'ship-sunk' : 'ship';
      });
    });
    
    // Mark hits
    hits.forEach(hit => {
      if (grid[hit.r][hit.c].startsWith('ship')) {
        grid[hit.r][hit.c] = 'hit';
      }
    });
    
    // Mark misses
    misses.forEach(miss => {
      grid[miss.r][miss.c] = 'miss';
    });
    
    return grid;
  }, [ships, hits, misses]);

  return (
    <div className="board">
      {board.map((row, r) => (
        <div key={r} className="board-row">
          {row.map((cell, c) => (
            <div
              key={`${r}-${c}`}
              className={`cell ${cell}`}
            >
              {cell === 'hit' && '💥'}
              {cell === 'miss' && '💦'}
            </div>
          ))}
        </div>
      ))}
    </div>
  );
};

export default MyBoard;



