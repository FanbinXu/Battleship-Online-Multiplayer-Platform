package app.battleship.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerState {
    private String playerId;
    private Board board;
    
    public PlayerState(String playerId) {
        this.playerId = playerId;
        this.board = new Board();
    }
}



