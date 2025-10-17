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
        myBoard.put("hits", myState.getBoard().getHits());
        myBoard.put("misses", myState.getBoard().getMisses());
        
        Map<String, Object> me = new HashMap<>();
        me.put("board", myBoard);
        view.put("me", me);
        
        // Opponent board (fogged - only show what I've attacked)
        Map<String, Object> revealed = new HashMap<>();
        
        // My attacks on opponent
        List<Coord> myHits = myState.getBoard().getAttackedByMe().stream()
                .filter(coord -> opponentState.getBoard().getShips().stream()
                        .anyMatch(ship -> ship.getCells().contains(coord)))
                .collect(Collectors.toList());
        
        List<Coord> myMisses = myState.getBoard().getAttackedByMe().stream()
                .filter(coord -> opponentState.getBoard().getShips().stream()
                        .noneMatch(ship -> ship.getCells().contains(coord)))
                .collect(Collectors.toList());
        
        Map<String, Object> attacksByMe = new HashMap<>();
        attacksByMe.put("hits", myHits);
        attacksByMe.put("misses", myMisses);
        revealed.put("attacksByMe", attacksByMe);
        
        // Sunk opponent ships
        List<Map<String, Object>> sunkShips = opponentState.getBoard().getShips().stream()
                .filter(Ship::isSunk)
                .map(ship -> {
                    Map<String, Object> shipMap = new HashMap<>();
                    shipMap.put("kind", ship.getKind().name());
                    shipMap.put("length", ship.getKind().getLength());
                    return shipMap;
                })
                .collect(Collectors.toList());
        revealed.put("sunkShips", sunkShips);
        
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

