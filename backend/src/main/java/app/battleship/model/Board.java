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
    private List<Ship> ships = new ArrayList<>();
    private List<Coord> hits = new ArrayList<>();
    private List<Coord> misses = new ArrayList<>();
    private List<Coord> attackedByMe = new ArrayList<>();
}



