package game;

import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import messagesbase.UniqueGameIdentifier;
import messagesbase.UniquePlayerIdentifier;
import messagesbase.messagesfromclient.EMove;
import messagesbase.messagesfromserver.*;

public class GameInfo {
    private final UniqueGameIdentifier gameID;
    private final long creationTime;
    private final FullMapGenerator fullMapGenerator = new FullMapGenerator();
    private final Set<PlayerState> players = new HashSet<>();
    private final Map<Integer, PlayerState> playerNumber = new HashMap<>();

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

    public int getPlayerNumberByPlayerID(UniquePlayerIdentifier playerID) {
        return playerNumber.entrySet().stream()
                .filter(entry -> entry.getValue().equals(playerID))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Player ID not found"))
                .getKey();
    }

    public FullMap getFilteredMapForPlayer(UniquePlayerIdentifier playerID) {
        int currentPlayerNumber = getPlayerNumberByPlayerID(playerID);
        FullMap filteredMap = new FullMap();

        for (FullMapNode node : fullMap.getMapNodes()) {
            if (node.getTreasureState() != ETreasureState.MyTreasureIsPresent && (node.getOwnedByPlayer() == 0 || node.getOwnedByPlayer() == currentPlayerNumber)) {
                filteredMap.add(node);
            } else if (node.getTreasureState() == ETreasureState.MyTreasureIsPresent && node.getOwnedByPlayer() == currentPlayerNumber && playerNumber.get(currentPlayerNumber).getRevealedTreasure()) {
            	filteredMap.add(node);
            } else if (node.getFortState() == EFortState.MyFortPresent && node.getOwnedByPlayer() != currentPlayerNumber && playerNumber.get(currentPlayerNumber).getRevealedEnemyFort()) {
            	FullMapNode maskedNode = new FullMapNode(
                        node.getTerrain(),
                        EPlayerPositionState.NoPlayerPresent,
                        ETreasureState.NoOrUnknownTreasureState,
                        EFortState.EnemyFortPresent,
                        node.getX(),
                        node.getY(),
                        0
                );
                filteredMap.add(maskedNode);
            } else {
                FullMapNode maskedNode = new FullMapNode(
                        node.getTerrain(),
                        EPlayerPositionState.NoPlayerPresent,
                        ETreasureState.NoOrUnknownTreasureState,
                        EFortState.NoOrUnknownFortState,
                        node.getX(),
                        node.getY(),
                        0
                );
                filteredMap.add(maskedNode);
            }
        }
        return filteredMap;
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
