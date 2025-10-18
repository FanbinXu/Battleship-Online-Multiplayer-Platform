import React, { useMemo, useState, useCallback } from 'react';
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
  canMove?: boolean;
  onShipMove?: (shipId: string, newPosition: Coord, isHorizontal: boolean) => void;
}

interface DragState {
  shipId: string | null;
  shipLength: number;
  isHorizontal: boolean;
  previewCells: Coord[] | null;
  isValid: boolean;
}

const MyBoard: React.FC<MyBoardProps> = ({ ships, hits, misses, canMove = false, onShipMove }) => {
  const [dragState, setDragState] = useState<DragState>({
    shipId: null,
    shipLength: 0,
    isHorizontal: true,
    previewCells: null,
    isValid: false,
  });

  // Helper function to check if a ship is horizontal
  const isShipHorizontal = (ship: Ship): boolean => {
    if (ship.cells.length < 2) return true;
    return ship.cells[0].r === ship.cells[1].r;
  };

  // Helper function to validate ship placement
  const validateShipPlacement = useCallback((shipId: string, cells: Coord[]): boolean => {
    // Check bounds
    for (const cell of cells) {
      if (cell.r < 0 || cell.r >= 10 || cell.c < 0 || cell.c >= 10) {
        return false;
      }
    }

    // Check overlaps with other ships
    for (const ship of ships) {
      if (ship.id === shipId) continue;
      
      for (const shipCell of ship.cells) {
        for (const newCell of cells) {
          if (shipCell.r === newCell.r && shipCell.c === newCell.c) {
            return false;
          }
        }
      }
    }

    return true;
  }, [ships]);

  // Handle drag start
  const handleDragStart = useCallback((e: React.DragEvent, ship: Ship) => {
    if (!canMove || ship.sunk) return;

    e.dataTransfer.effectAllowed = 'move';
    
    // Store ship info in drag data and state
    const isHorizontal = isShipHorizontal(ship);
    e.dataTransfer.setData('shipId', ship.id);
    e.dataTransfer.setData('length', ship.cells.length.toString());
    e.dataTransfer.setData('isHorizontal', isHorizontal.toString());

    setDragState({
      shipId: ship.id,
      shipLength: ship.cells.length,
      isHorizontal: isHorizontal,
      previewCells: null,
      isValid: false,
    });
  }, [canMove]);

  // Handle drag over
  const handleDragOver = useCallback((e: React.DragEvent, r: number, c: number) => {
    e.preventDefault();
    
    if (!dragState.shipId) return;

    // Use state values as they are more reliable than dataTransfer
    const shipId = dragState.shipId;
    const length = dragState.shipLength;
    const isHorizontal = dragState.isHorizontal;

    // Calculate preview cells based on anchor position
    const previewCells: Coord[] = [];
    for (let i = 0; i < length; i++) {
      if (isHorizontal) {
        previewCells.push({ r, c: c + i });
      } else {
        previewCells.push({ r: r + i, c });
      }
    }

    const isValid = validateShipPlacement(shipId, previewCells);

    setDragState({
      shipId,
      shipLength: length,
      isHorizontal,
      previewCells,
      isValid,
    });
  }, [dragState.shipId, dragState.shipLength, dragState.isHorizontal, validateShipPlacement]);

  // Handle drop
  const handleDrop = useCallback((e: React.DragEvent, r: number, c: number) => {
    e.preventDefault();

    const shipId = e.dataTransfer.getData('shipId');
    const length = parseInt(e.dataTransfer.getData('length'));
    const isHorizontal = e.dataTransfer.getData('isHorizontal') === 'true';

    // Calculate new cells
    const newCells: Coord[] = [];
    for (let i = 0; i < length; i++) {
      if (isHorizontal) {
        newCells.push({ r, c: c + i });
      } else {
        newCells.push({ r: r + i, c });
      }
    }

    // Validate placement
    const isValid = validateShipPlacement(shipId, newCells);

    if (isValid && onShipMove) {
      onShipMove(shipId, { r, c }, isHorizontal);
    }

    // Reset drag state
    setDragState({
      shipId: null,
      shipLength: 0,
      isHorizontal: true,
      previewCells: null,
      isValid: false,
    });
  }, [validateShipPlacement, onShipMove]);

  // Handle drag end
  const handleDragEnd = useCallback(() => {
    setDragState({
      shipId: null,
      shipLength: 0,
      isHorizontal: true,
      previewCells: null,
      isValid: false,
    });
  }, []);

  // Create board grid with ship data
  const boardData = useMemo(() => {
    const grid: {
      type: string;
      shipId?: string;
    }[][] = Array(10).fill(null).map(() => Array(10).fill(null).map(() => ({ type: '' })));

    // Mark ship positions
    ships.forEach(ship => {
      ship.cells.forEach(cell => {
        grid[cell.r][cell.c] = {
          type: ship.sunk ? 'ship-sunk' : 'ship',
          shipId: ship.id,
        };
      });
    });

    // Mark hits
    hits.forEach(hit => {
      if (grid[hit.r][hit.c].type.startsWith('ship')) {
        grid[hit.r][hit.c].type = 'hit';
      }
    });

    // Mark misses (Note: opponent's misses are NOT sent to your board - only hits are visible)
    // This code is kept for potential future use, but misses array should always be empty
    misses.forEach(miss => {
      grid[miss.r][miss.c].type = 'miss';
    });

    return grid;
  }, [ships, hits, misses]);

  // Check if a cell is part of the preview
  const getCellPreviewClass = (r: number, c: number): string => {
    if (!dragState.previewCells) return '';
    
    const isPreview = dragState.previewCells.some(cell => cell.r === r && cell.c === c);
    if (!isPreview) return '';

    return dragState.isValid ? 'preview-valid' : 'preview-invalid';
  };

  return (
    <div className="board">
      {boardData.map((row, r) => (
        <div key={r} className="board-row">
          {row.map((cellData, c) => {
            const previewClass = getCellPreviewClass(r, c);
            const isDragging = dragState.shipId === cellData.shipId;
            
            return (
              <div
                key={`${r}-${c}`}
                className={`cell ${cellData.type} ${previewClass} ${isDragging ? 'dragging' : ''}`}
                draggable={canMove && cellData.type.startsWith('ship') && cellData.type !== 'ship-sunk'}
                onDragStart={(e) => {
                  if (cellData.shipId) {
                    const ship = ships.find(s => s.id === cellData.shipId);
                    if (ship) handleDragStart(e, ship);
                  }
                }}
                onDragOver={(e) => handleDragOver(e, r, c)}
                onDrop={(e) => handleDrop(e, r, c)}
                onDragEnd={handleDragEnd}
                style={{
                  cursor: canMove && cellData.type.startsWith('ship') && cellData.type !== 'ship-sunk' ? 'move' : 'default',
                }}
              >
                {cellData.type === 'hit' && '💥'}
                {cellData.type === 'miss' && '💦'}
              </div>
            );
          })}
        </div>
      ))}
    </div>
  );
};

export default MyBoard;



