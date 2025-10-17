package app.battleship.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Coord {
    private int r;
    private int c;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Coord coord)) return false;
        return r == coord.r && c == coord.c;
    }
    
    @Override
    public int hashCode() {
        return 31 * r + c;
    }
}


