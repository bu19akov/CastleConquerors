package main.game;

import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import messagesbase.UniqueGameIdentifier;
import messagesbase.UniquePlayerIdentifier;
import messagesbase.messagesfromclient.EMove;
import messagesbase.messagesfromserver.*;

public class GameInfo {
	private static final int FAKE_OPPONENT_POSITION_TURNS = 16;
    private final UniqueGameIdentifier gameID;
    private final long creationTime;
    private final FullMapGenerator fullMapGenerator = new FullMapGenerator();
    private final Set<PlayerState> players = new HashSet<>();
    private final Map<Integer, PlayerState> playerNumber = new HashMap<>();

    int fakeOpponentX = -1, fakeOpponentY = -1;
    private String stateID;
    private PlayerState currentPlayer;
    private FullMap fullMap;
    private int turnCount = 0;
    private long turnStartTime = 0;
    private Map<PlayerState, Map<EMove, Integer>> moveCounter = new HashMap<>(new HashMap<>());

    public GameInfo(UniqueGameIdentifier gameID, String stateID, long creationTime) {
        this.gameID = gameID;
        this.stateID = stateID;
        this.creationTime = creationTime;
        this.fullMap = fullMapGenerator.generateFullMap();
    }

    public UniqueGameIdentifier getGameID() {
        return gameID;
    }
    
    public Map<PlayerState, Map<EMove, Integer>> getMoveCounter() {
    	return this.moveCounter;
    }

    public String getStateID() {
        return stateID;
    }

    public void setStateID(String stateID) {
        this.stateID = stateID;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public void registerPlayer(PlayerState player) {
        players.add(player);
        playerNumber.put(players.size(), player);
    }

    public Set<PlayerState> getPlayers() {
        return players;
    }

    public boolean containsPlayerWithID(String playerID) {
        return players.stream()
                .anyMatch(player -> player.getUniquePlayerID().equals(playerID));
    }
    
    public PlayerState getPlayerWithID(String playerID) {
        return players.stream()
                    .filter(player -> player.getUniquePlayerID().equals(playerID))
                    .findFirst()
                    .orElse(null);
    }

    public FullMap getFilteredMapForPlayer(UniquePlayerIdentifier playerID) {
    	FullMap filteredMap = initializeFilteredMap();
        int currentPlayerNumber = getPlayerNumberByPlayerID(playerID);
        
        for (FullMapNode node : fullMap.getMapNodes()) {
            FullMapNode maskedNode = determineNodeState(node, currentPlayerNumber, playerID);
            filteredMap.add(maskedNode);
        }
        
        return filteredMap;
    }
    
    private FullMap initializeFilteredMap() {
        FullMap filteredMap = new FullMap();
        filteredMap.setMaxX(fullMap.getMaxX());
        filteredMap.setMaxY(fullMap.getMaxY());
        return filteredMap;
    }
    
    public int getPlayerNumberByPlayerID(UniquePlayerIdentifier playerID) {
        return playerNumber.entrySet().stream()
                .filter(entry -> entry.getValue().equals(playerID))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Player ID not found"))
                .getKey();
    }
    
    private FullMapNode determineNodeState(FullMapNode node, int currentPlayerNumber, UniquePlayerIdentifier playerID) {
    	if (getTurnCount() >= FAKE_OPPONENT_POSITION_TURNS && 
    			node.getOwnedByPlayer() != currentPlayerNumber && 
    			node.getPlayerPositionState() == EPlayerPositionState.MyPlayerPosition) {
    		return getMaskedEnemyPlayer(node, ETreasureState.NoOrUnknownTreasureState);
    	} 
    	if (node.getPlayerPositionState() == EPlayerPositionState.BothPlayerPosition && 
    			node.getFortState() == EFortState.MyFortPresent) {
    		if (node.getOwnedByPlayer() == currentPlayerNumber) {
    			return getMaskedBothPlayers(node, EFortState.MyFortPresent);
    		} else {
    			if (getPlayerWithID(playerID.getUniquePlayerID()).getRevealedEnemyFort()) {
    				return getMaskedBothPlayers(node, EFortState.EnemyFortPresent);
    			} else {
    				return getMaskedBothPlayers(node, EFortState.NoOrUnknownFortState);
    			}
    		}
    	} else if (node.getFortState() == EFortState.MyFortPresent && 
    			node.getOwnedByPlayer() != currentPlayerNumber && 
    			node.getPlayerPositionState() == EPlayerPositionState.EnemyPlayerPosition) {
    		if (getPlayerWithID(playerID.getUniquePlayerID()).getCollectedTreasure()) { 
    			return getMaskedMyPlayer(node, EFortState.EnemyFortPresent);
    		} else {
    			return getMaskedMyPlayer(node, EFortState.NoOrUnknownFortState);
    		}
    	} else if (node.getTreasureState() != ETreasureState.MyTreasureIsPresent && 
    			(node.getOwnedByPlayer() == 0 || node.getOwnedByPlayer() == currentPlayerNumber)) {
    		return getNodeDeepCopy(node);
    	} else if (node.getPlayerPositionState() == EPlayerPositionState.EnemyPlayerPosition && 
    			node.getTreasureState() == ETreasureState.MyTreasureIsPresent) {
    		if (node.getOwnedByPlayer() != currentPlayerNumber) {
    			getMaskedMyPlayer(node, EFortState.NoOrUnknownFortState);
    		} else if (getPlayerWithID(playerID.getUniquePlayerID()).getCollectedTreasure() || 
    				getPlayerWithID(playerID.getUniquePlayerID()).getRevealedTreasure()) {
    			return getMaskedEnemyPlayer(node, ETreasureState.MyTreasureIsPresent);
    		} else {
    			return getMaskedEnemyPlayer(node, ETreasureState.NoOrUnknownTreasureState);
    		}
    	} else if (node.getTreasureState() == ETreasureState.MyTreasureIsPresent && 
    			node.getOwnedByPlayer() == currentPlayerNumber && 
    			(playerNumber.get(currentPlayerNumber).getRevealedTreasure() || playerNumber.get(currentPlayerNumber).getCollectedTreasure())) {
    		return getNodeDeepCopy(node);
    	} else if (node.getPlayerPositionState() == EPlayerPositionState.BothPlayerPosition && 
    			node.getTreasureState() == ETreasureState.MyTreasureIsPresent && 
    			node.getOwnedByPlayer() != currentPlayerNumber) {
    		return getMaskedBothPlayers(node, EFortState.NoOrUnknownFortState);
    	} else if (node.getFortState() == EFortState.MyFortPresent && 
    			node.getOwnedByPlayer() != currentPlayerNumber && 
    			playerNumber.get(currentPlayerNumber).getRevealedEnemyFort()) {
    		return getMaskedFortState(node, EFortState.EnemyFortPresent);
    	}
		return getMaskedFortState(node, EFortState.NoOrUnknownFortState);
    }
    
    private FullMapNode getMaskedFortState(FullMapNode node, EFortState fortState) {
    	return new FullMapNode(
                node.getTerrain(),
                EPlayerPositionState.NoPlayerPresent,
                ETreasureState.NoOrUnknownTreasureState,
                fortState,
                node.getX(),
                node.getY(),
                0
        );
    }
    
    private FullMapNode getNodeDeepCopy(FullMapNode node) {
    	return new FullMapNode(
                node.getTerrain(),
                node.getPlayerPositionState(),
                node.getTreasureState(),
                node.getFortState(),
                node.getX(),
                node.getY(),
                node.getOwnedByPlayer()
        );
    }
    
    private FullMapNode getMaskedMyPlayer(FullMapNode node, EFortState state) {
    	return new FullMapNode(
                node.getTerrain(),
                EPlayerPositionState.MyPlayerPosition,
                ETreasureState.NoOrUnknownTreasureState,
                state,
                node.getX(),
                node.getY(),
                0
        );
    }
    
    private FullMapNode getMaskedBothPlayers(FullMapNode node, EFortState state) {
    	return new FullMapNode(
                node.getTerrain(),
                EPlayerPositionState.BothPlayerPosition,
                ETreasureState.NoOrUnknownTreasureState,
                state,
                node.getX(),
                node.getY(),
                0
        );
    }
    
    private FullMapNode getMaskedEnemyPlayer(FullMapNode node, ETreasureState treasureState) {
    	return new FullMapNode(
                node.getTerrain(),
                EPlayerPositionState.EnemyPlayerPosition,
                treasureState,
                EFortState.NoOrUnknownFortState,
                node.getX(),
                node.getY(),
                0
        );
    }

    public PlayerState getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(PlayerState currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

    public long getTurnStartTime() {
        return turnStartTime;
    }

    public void setTurnStartTime(long turnStartTime) {
        this.turnStartTime = turnStartTime;
    }

    public int getTurnCount() {
        return turnCount;
    }

    public void incrementTurnCount() {
        turnCount++;
    }
    
    public FullMap getFullMap() {
    	return this.fullMap;
    }
}
