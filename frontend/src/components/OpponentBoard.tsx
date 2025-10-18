import React, { useMemo } from 'react';
import './Board.css';

interface Coord {
  r: number;
  c: number;
}

interface Ship {
  kind: string;
  length: number;
  cells?: Coord[];  // Optional: full position of sunk ship
}

interface OpponentBoardProps {
  attacksByMe: {
    hits: Coord[];
    misses: Coord[];
  };
  sunkShips: Ship[];
  onAttack: (target: Coord) => void;
  disabled: boolean;
}

// Custom comparison function for deep props comparison
const arePropsEqual = (prevProps: OpponentBoardProps, nextProps: OpponentBoardProps): boolean => {
  // Compare primitive values
  if (prevProps.disabled !== nextProps.disabled) return false;
  
  // Compare function references (should be stable with useCallback)
  if (prevProps.onAttack !== nextProps.onAttack) return false;
  
  // Deep compare attacksByMe
  if (!prevProps.attacksByMe || !nextProps.attacksByMe) {
    return prevProps.attacksByMe === nextProps.attacksByMe;
  }
  
  const prevHits = prevProps.attacksByMe.hits || [];
  const nextHits = nextProps.attacksByMe.hits || [];
  const prevMisses = prevProps.attacksByMe.misses || [];
  const nextMisses = nextProps.attacksByMe.misses || [];
  
  if (prevHits.length !== nextHits.length) return false;
  if (prevMisses.length !== nextMisses.length) return false;
  
  // Compare hits
  for (let i = 0; i < prevHits.length; i++) {
    if (prevHits[i].r !== nextHits[i].r || prevHits[i].c !== nextHits[i].c) {
      return false;
    }
  }
  
  // Compare misses
  for (let i = 0; i < prevMisses.length; i++) {
    if (prevMisses[i].r !== nextMisses[i].r || prevMisses[i].c !== nextMisses[i].c) {
      return false;
    }
  }
  
  // Compare sunkShips
  if (prevProps.sunkShips.length !== nextProps.sunkShips.length) return false;
  
  for (let i = 0; i < prevProps.sunkShips.length; i++) {
    const prevShip = prevProps.sunkShips[i];
    const nextShip = nextProps.sunkShips[i];
    
    if (prevShip.kind !== nextShip.kind || prevShip.length !== nextShip.length) {
      return false;
    }
    
    // Compare cells if present
    const prevCells = prevShip.cells || [];
    const nextCells = nextShip.cells || [];
    
    if (prevCells.length !== nextCells.length) return false;
    
    for (let j = 0; j < prevCells.length; j++) {
      if (prevCells[j].r !== nextCells[j].r || prevCells[j].c !== nextCells[j].c) {
        return false;
      }
    }
  }
  
  return true;
};

const OpponentBoard: React.FC<OpponentBoardProps> = ({ attacksByMe, sunkShips, onAttack, disabled }) => {
  // Debug: Log received data
  console.log('[OpponentBoard] Render - Full attacksByMe object:', JSON.stringify(attacksByMe, null, 2));
  console.log('[OpponentBoard] Render - Hits array:', attacksByMe?.hits);
  console.log('[OpponentBoard] Render - Misses array:', attacksByMe?.misses);
  console.log('[OpponentBoard] Render - Type check:', {
    attacksByMeType: typeof attacksByMe,
    hitsType: typeof attacksByMe?.hits,
    missesType: typeof attacksByMe?.misses,
    hitsIsArray: Array.isArray(attacksByMe?.hits),
    missesIsArray: Array.isArray(attacksByMe?.misses)
  });
  
  const board = useMemo(() => {
    console.log('[OpponentBoard] useMemo - Rebuilding board grid');
    const grid: string[][] = Array(10).fill(null).map(() => Array(10).fill(''));
    
    let hitCount = 0;
    let missCount = 0;
    
    // Mark hits
    if (attacksByMe?.hits && Array.isArray(attacksByMe.hits)) {
      console.log('[OpponentBoard] useMemo - Processing hits:', attacksByMe.hits.length);
      attacksByMe.hits.forEach(hit => {
        console.log('[OpponentBoard] useMemo - Marking hit at row:', hit.r, 'col:', hit.c);
        if (hit.r >= 0 && hit.r < 10 && hit.c >= 0 && hit.c < 10) {
          grid[hit.r][hit.c] = 'hit';
          hitCount++;
        } else {
          console.error('[OpponentBoard] Invalid hit coordinate:', hit);
        }
      });
    } else {
      console.warn('[OpponentBoard] useMemo - No hits or hits is not an array');
    }
    
    // Mark misses
    if (attacksByMe?.misses && Array.isArray(attacksByMe.misses)) {
      console.log('[OpponentBoard] useMemo - Processing misses:', attacksByMe.misses.length);
      attacksByMe.misses.forEach(miss => {
        console.log('[OpponentBoard] useMemo - Marking miss at row:', miss.r, 'col:', miss.c);
        if (miss.r >= 0 && miss.r < 10 && miss.c >= 0 && miss.c < 10) {
          grid[miss.r][miss.c] = 'miss';
          missCount++;
        } else {
          console.error('[OpponentBoard] Invalid miss coordinate:', miss);
        }
      });
    } else {
      console.warn('[OpponentBoard] useMemo - No misses or misses is not an array');
    }
    
    console.log('[OpponentBoard] useMemo - Grid built with', hitCount, 'hits and', missCount, 'misses');
    console.log('[OpponentBoard] useMemo - Sample grid cells:', {
      '0,0': grid[0][0],
      '0,1': grid[0][1],
      '5,5': grid[5][5]
    });
    
    return grid;
  }, [attacksByMe]);

  const handleCellClick = (r: number, c: number) => {
    if (disabled) {
      console.log('[OpponentBoard] Click disabled');
      return;
    }
    
    // Allow re-attacking to verify if opponent moved ships
    if (board[r][c] !== '') {
      console.log('[OpponentBoard] RE-ATTACKING cell:', r, c, 'previous status:', board[r][c]);
    } else {
      console.log('[OpponentBoard] Attacking cell:', r, c);
    }
    
    onAttack({ r, c });
  };

  console.log('[OpponentBoard] Rendering board with grid:', board.map((row, r) => 
    row.map((cell, c) => cell !== '' ? `[${r},${c}]=${cell}` : null).filter(Boolean)
  ).flat());

  return (
    <div>
      <div className="board">
        {board.map((row, r) => (
          <div key={r} className="board-row">
            {row.map((cell, c) => {
              // All cells are attackable if not disabled (allow re-attacking)
              const cellClass = `cell ${cell} ${!disabled ? 'attackable' : ''}`;
              const cellContent = cell === 'hit' ? '💥' : cell === 'miss' ? '💦' : '';
              
              if (cell !== '') {
                console.log(`[OpponentBoard] Rendering cell [${r},${c}]:`, {
                  cellValue: cell,
                  className: cellClass,
                  content: cellContent
                });
              }
              
              return (
                <div
                  key={`${r}-${c}`}
                  className={cellClass}
                  onClick={() => handleCellClick(r, c)}
                  title={`[${r},${c}] ${cell || 'empty'} (click to ${cell ? 're-attack' : 'attack'})`}
                >
                  {cellContent}
                </div>
              );
            })}
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

// Wrap with React.memo for performance optimization
export default React.memo(OpponentBoard, arePropsEqual);



