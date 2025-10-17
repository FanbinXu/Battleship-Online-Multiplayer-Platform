package app.battleship.service;

import app.battleship.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ShipPlacementService {
    
    private static final int BOARD_SIZE = 10;
    private static final List<ShipKind> FLEET = Arrays.asList(
            ShipKind.CARRIER_5,
            ShipKind.BATTLESHIP_4,
            ShipKind.CRUISER_3,
            ShipKind.SUBMARINE_3,
            ShipKind.DESTROYER_2
    );
    
    public List<Ship> placeShipsRandomly() {
        List<Ship> ships = new ArrayList<>();
        boolean[][] occupied = new boolean[BOARD_SIZE][BOARD_SIZE];
        Random random = new Random();
        
        for (ShipKind kind : FLEET) {
            Ship ship = null;
            int attempts = 0;
            int maxAttempts = 1000;
            
            while (ship == null && attempts < maxAttempts) {
                attempts++;
                
                boolean horizontal = random.nextBoolean();
                int length = kind.getLength();
                
                int r, c;
                if (horizontal) {
                    r = random.nextInt(BOARD_SIZE);
                    c = random.nextInt(BOARD_SIZE - length + 1);
                } else {
                    r = random.nextInt(BOARD_SIZE - length + 1);
                    c = random.nextInt(BOARD_SIZE);
                }
                
                List<Coord> cells = new ArrayList<>();
                boolean valid = true;
                
                for (int i = 0; i < length; i++) {
                    int cr = horizontal ? r : r + i;
                    int cc = horizontal ? c + i : c;
                    
                    if (occupied[cr][cc]) {
                        valid = false;
                        break;
                    }
                    cells.add(new Coord(cr, cc));
                }
                
                if (valid) {
                    ship = new Ship(kind, cells);
                    for (Coord coord : cells) {
                        occupied[coord.getR()][coord.getC()] = true;
                    }
                }
            }
            
            if (ship == null) {
                throw new RuntimeException("Failed to place ship: " + kind);
            }
            
            ships.add(ship);
        }
        
        return ships;
    }
    
    public boolean isValidCoord(Coord coord) {
        return coord.getR() >= 0 && coord.getR() < BOARD_SIZE && 
               coord.getC() >= 0 && coord.getC() < BOARD_SIZE;
    }
}



