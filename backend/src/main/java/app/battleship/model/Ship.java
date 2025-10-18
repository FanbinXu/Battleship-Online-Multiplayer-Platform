package app.battleship.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ship {
    private String id;
    private ShipKind kind;
    private List<Coord> cells;
    private boolean sunk;
    private Set<Integer> hitIndices = new HashSet<>();  // Track which relative positions are damaged (0-based)
    
    public Ship(ShipKind kind, List<Coord> cells) {
        this.id = UUID.randomUUID().toString();
        this.kind = kind;
        this.cells = cells;
        this.sunk = false;
        this.hitIndices = new HashSet<>();
    }
    
    /**
     * Check if all cells are hit (ship should be sunk)
     * JsonIgnore prevents this from being serialized as a field
     */
    @JsonIgnore
    public boolean isFullyDamaged() {
        return hitIndices != null && cells != null && hitIndices.size() >= cells.size();
    }
    
    /**
     * Get the actual coordinates that are damaged based on current position
     * JsonIgnore prevents this from being serialized as a field
     */
    @JsonIgnore
    public List<Coord> getDamagedCells() {
        List<Coord> damaged = new ArrayList<>();
        if (hitIndices != null && cells != null) {
            for (Integer idx : hitIndices) {
                if (idx >= 0 && idx < cells.size()) {
                    damaged.add(cells.get(idx));
                }
            }
        }
        return damaged;
    }
}



