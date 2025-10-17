package app.battleship.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ship {
    private String id;
    private ShipKind kind;
    private List<Coord> cells;
    private boolean sunk;
    
    public Ship(ShipKind kind, List<Coord> cells) {
        this.id = UUID.randomUUID().toString();
        this.kind = kind;
        this.cells = cells;
        this.sunk = false;
    }
}



