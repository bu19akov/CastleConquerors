package game;

import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import messagesbase.UniqueGameIdentifier;
import messagesbase.UniquePlayerIdentifier;
import messagesbase.messagesfromclient.EMove;
import messagesbase.messagesfromserver.*;

public class GameInfo {
	private static final int FAKE_OPPONENT_POSITION_TURNS = 16; // need to be 16
    private final UniqueGameIdentifier gameID;
    private final long creationTime;
    private final FullMapGenerator fullMapGenerator = new FullMapGenerator();
    private final Set<PlayerState> players = new HashSet<>();
    private final Map<Integer, PlayerState> playerNumber = new HashMap<>();

    int fakeOpponentX = -1, fakeOpponentY = -1;
    private String stateIDBackUp;
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
        Random rand = new Random(); // Create a Random instance to decide enemy's position

        filteredMap.setMaxX(fullMap.getMaxX());
        filteredMap.setMaxY(fullMap.getMaxY());

        for (FullMapNode node : fullMap.getMapNodes()) {
        	if (getTurnCount() >= FAKE_OPPONENT_POSITION_TURNS && node.getOwnedByPlayer() != currentPlayerNumber && node.getPlayerPositionState() == EPlayerPositionState.MyPlayerPosition) {
        		FullMapNode maskedNode = new FullMapNode(
                        node.getTerrain(),
                        EPlayerPositionState.EnemyPlayerPosition,
                        ETreasureState.NoOrUnknownTreasureState,
                        EFortState.NoOrUnknownFortState,
                        node.getX(),
                        node.getY(),
                        0
                );
                filteredMap.add(maskedNode);
        	}
        	if (node.getFortState() == EFortState.MyFortPresent && node.getOwnedByPlayer() != currentPlayerNumber && node.getPlayerPositionState() == EPlayerPositionState.EnemyPlayerPosition) {
        		FullMapNode maskedNode;
        		if (getPlayerWithID(playerID.getUniquePlayerID()).getCollectedTreasure()) { // if (currentPlayer.getCollectedTreasure()) {
        			maskedNode = new FullMapNode(
                            node.getTerrain(),
                            EPlayerPositionState.MyPlayerPosition,
                            ETreasureState.NoOrUnknownTreasureState,
                            EFortState.EnemyFortPresent,
                            node.getX(),
                            node.getY(),
                            0
                    );
        		} else {
	        		maskedNode = new FullMapNode(
	                        node.getTerrain(),
	                        EPlayerPositionState.MyPlayerPosition,
	                        ETreasureState.NoOrUnknownTreasureState,
	                        EFortState.NoOrUnknownFortState,
	                        node.getX(),
	                        node.getY(),
	                        0
	                );
        		}
        		filteredMap.add(maskedNode);
        	}
        		
        	// show my player position
        	else if (node.getTreasureState() != ETreasureState.MyTreasureIsPresent && (node.getOwnedByPlayer() == 0 || node.getOwnedByPlayer() == currentPlayerNumber)) {
        		FullMapNode maskedNode = new FullMapNode(
                        node.getTerrain(),
                        node.getPlayerPositionState(),
                        node.getTreasureState(),
                        node.getFortState(),
                        node.getX(),
                        node.getY(),
                        node.getOwnedByPlayer()
                );
                filteredMap.add(maskedNode);
            } // show my treasure
            else if (node.getPlayerPositionState() == EPlayerPositionState.EnemyPlayerPosition && node.getTreasureState() == ETreasureState.MyTreasureIsPresent) {
            	FullMapNode maskedNode;
            	if (node.getOwnedByPlayer() != currentPlayerNumber) {
            		maskedNode = new FullMapNode(
                        node.getTerrain(),
                        EPlayerPositionState.MyPlayerPosition,
                        ETreasureState.NoOrUnknownTreasureState,
                        EFortState.NoOrUnknownFortState,
                        node.getX(),
                        node.getY(),
                        0
                    );
            	}
            	else if (getPlayerWithID(playerID.getUniquePlayerID()).getCollectedTreasure() || getPlayerWithID(playerID.getUniquePlayerID()).getRevealedTreasure()) { // } else if (currentPlayer.getRevealedTreasure() || currentPlayer.getCollectedTreasure()) {
            		maskedNode = new FullMapNode(
                        node.getTerrain(),
                        EPlayerPositionState.EnemyPlayerPosition,
                        ETreasureState.MyTreasureIsPresent,
                        EFortState.NoOrUnknownFortState,
                        node.getX(),
                        node.getY(),
                        0
                    );
            	} else { // If enemy enters my treasure, that has been discovered, and I send move request, the treasure disappears
            		maskedNode = new FullMapNode(
                        node.getTerrain(),
                        EPlayerPositionState.EnemyPlayerPosition,
                        ETreasureState.NoOrUnknownTreasureState,
                        EFortState.NoOrUnknownFortState,
                        node.getX(),
                        node.getY(),
                        0
                    );
            	}
                filteredMap.add(maskedNode);
            }
            else if (node.getTreasureState() == ETreasureState.MyTreasureIsPresent && node.getOwnedByPlayer() == currentPlayerNumber && (playerNumber.get(currentPlayerNumber).getRevealedTreasure() || playerNumber.get(currentPlayerNumber).getCollectedTreasure())) {
            	FullMapNode maskedNode = new FullMapNode(
                        node.getTerrain(),
                        node.getPlayerPositionState(),
                        node.getTreasureState(),
                        node.getFortState(),
                        node.getX(),
                        node.getY(),
                        node.getOwnedByPlayer()
                );
                filteredMap.add(maskedNode);
            } // show enemy fort location
            else if (node.getFortState() == EFortState.MyFortPresent && node.getOwnedByPlayer() != currentPlayerNumber && playerNumber.get(currentPlayerNumber).getRevealedEnemyFort()) {
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
            } // get normal nodes
            else {
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
        
//        if (getTurnCount() <= FAKE_OPPONENT_POSITION_TURNS && fakeOpponentX != -1 && fakeOpponentY != -1) {
//        	Optional<FullMapNode> randomNodeOpt = filteredMap.get(fakeOpponentX, fakeOpponentY);
//            if (randomNodeOpt.isPresent()) {
//                FullMapNode randomNode = randomNodeOpt.get();
//                if (randomNode.getPlayerPositionState() != EPlayerPositionState.MyPlayerPosition) {
//                	randomNode.setPlayerPositionState(EPlayerPositionState.EnemyPlayerPosition);
//                } else {
//                    randomNode.setPlayerPositionState(EPlayerPositionState.BothPlayerPosition);
//                }
//            }
//        }
//        
//        // For the first 16 turns, determine random x and y
//        if (this.stateID != this.stateIDBackUp) {
//        	System.out.println(stateID);
//        	this.stateIDBackUp = this.stateID;
//	        if (getTurnCount() <= FAKE_OPPONENT_POSITION_TURNS) {
//	            int randomX = rand.nextInt(filteredMap.getMaxX() + 1);
//	            int randomY = rand.nextInt(filteredMap.getMaxY() + 1);
//	
//	            this.fakeOpponentX = randomX;
//	            this.fakeOpponentY = randomY;
//	        } 
//        }
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
