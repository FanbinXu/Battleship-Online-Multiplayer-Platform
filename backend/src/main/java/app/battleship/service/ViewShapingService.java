package app.battleship.service;

import app.battleship.model.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ViewShapingService {
    
    public Map<String, Object> createPlayerView(GameState gameState, String playerId) {
        PlayerState myState = gameState.getPlayers().get(playerId);
        String opponentId = gameState.getPlayers().keySet().stream()
                .filter(id -> !id.equals(playerId))
                .findFirst()
                .orElseThrow();
        PlayerState opponentState = gameState.getPlayers().get(opponentId);
        
        System.out.println("[ViewShaping] Creating view for player: " + playerId);
        System.out.println("[ViewShaping] AttacksByMeHits size: " + myState.getBoard().getAttacksByMeHits().size());
        System.out.println("[ViewShaping] AttacksByMeHits coords: " + myState.getBoard().getAttacksByMeHits());
        System.out.println("[ViewShaping] AttacksByMeMisses size: " + myState.getBoard().getAttacksByMeMisses().size());
        System.out.println("[ViewShaping] AttacksByMeMisses coords: " + myState.getBoard().getAttacksByMeMisses());
        
        Map<String, Object> view = new HashMap<>();
        
        // My board (fully visible)
        Map<String, Object> myBoard = new HashMap<>();
        myBoard.put("ships", myState.getBoard().getShips().stream()
                .map(ship -> Map.of(
                        "id", ship.getId(),
                        "kind", ship.getKind().name(),
                        "cells", ship.getCells(),
                        "sunk", ship.isSunk()
                ))
                .collect(Collectors.toList())
        );
        myBoard.put("hits", myState.getBoard().getHits());  // Only opponent's hits on my ships
        myBoard.put("misses", myState.getBoard().getMisses());  // Should be empty - opponent's misses are not recorded
        
        Map<String, Object> me = new HashMap<>();
        me.put("board", myBoard);
        view.put("me", me);
        
        // Opponent board (fogged - only show what I've attacked)
        Map<String, Object> revealed = new HashMap<>();
        
        // Use static records of my attacks (not dynamically calculated)
        List<Coord> myHits = myState.getBoard().getAttacksByMeHits();
        List<Coord> myMisses = myState.getBoard().getAttacksByMeMisses();
        
        System.out.println("[ViewShaping] Static myHits: " + myHits);
        System.out.println("[ViewShaping] Static myMisses: " + myMisses);
        
        Map<String, Object> attacksByMe = new HashMap<>();
        attacksByMe.put("hits", myHits);
        attacksByMe.put("misses", myMisses);
        revealed.put("attacksByMe", attacksByMe);
        
        System.out.println("[ViewShaping] attacksByMe map: " + attacksByMe);
        
        // Sunk opponent ships (from sunkShips list, not from active ships)
        List<Map<String, Object>> sunkShips = opponentState.getBoard().getSunkShips().stream()
                .map(ship -> {
                    Map<String, Object> shipMap = new HashMap<>();
                    shipMap.put("kind", ship.getKind().name());
                    shipMap.put("length", ship.getKind().getLength());
                    shipMap.put("cells", ship.getCells());  // Show final position of sunk ship
                    return shipMap;
                })
                .collect(Collectors.toList());
        revealed.put("sunkShips", sunkShips);
        
        System.out.println("[ViewShaping] Sunk ships count: " + sunkShips.size());
        
        Map<String, Object> opponent = new HashMap<>();
        opponent.put("revealed", revealed);
        view.put("opponent", opponent);
        
        // Game metadata
        view.put("turn", gameState.getTurn());
        view.put("currentPlayerId", gameState.getCurrentPlayerId());
        view.put("stateVersion", gameState.getStateVersion());
        if (gameState.getWinnerPlayerId() != null) {
            view.put("winnerPlayerId", gameState.getWinnerPlayerId());
        }
        
        return view;
    }
}

