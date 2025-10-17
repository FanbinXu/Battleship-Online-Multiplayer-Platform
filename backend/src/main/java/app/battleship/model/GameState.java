package app.battleship.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameState {
    private String gameId;
    private String roomId;
    private int turn;
    private String currentPlayerId;
    private Map<String, PlayerState> players = new HashMap<>();
    private String winnerPlayerId;
    private int stateVersion;
    
    public GameState(String gameId, String roomId, String firstPlayerId, String secondPlayerId) {
        this.gameId = gameId;
        this.roomId = roomId;
        this.turn = 1;
        this.currentPlayerId = firstPlayerId;
        this.players = new HashMap<>();
        this.players.put(firstPlayerId, new PlayerState(firstPlayerId));
        this.players.put(secondPlayerId, new PlayerState(secondPlayerId));
        this.stateVersion = 1;
    }
}



