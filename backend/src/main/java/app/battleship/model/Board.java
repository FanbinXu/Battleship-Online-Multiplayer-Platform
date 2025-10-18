package app.battleship.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Board {
    private List<Ship> ships = new ArrayList<>();  // Active ships (not sunk)
    private List<Ship> sunkShips = new ArrayList<>();  // Ships that have been sunk and removed
    private List<Coord> hits = new ArrayList<>();  // Attacks received on my board (hits)
    private List<Coord> misses = new ArrayList<>();  // Attacks received on my board (misses) - should be empty
    private List<Coord> attacksByMeHits = new ArrayList<>();  // My attacks that hit opponent (static record)
    private List<Coord> attacksByMeMisses = new ArrayList<>();  // My attacks that missed opponent (static record)
}



