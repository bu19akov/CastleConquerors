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
    private final UniqueGameIdentifier gameID;
    private final long creationTime;
    private final FullMapGenerator fullMapGenerator = new FullMapGenerator();
    private final Set<PlayerState> players = new HashSet<>();
    private final Map<Integer, PlayerState> playerNumber = new HashMap<>();

    int randomX = -1, randomY = -1;
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
        	if (node.getFortState() == EFortState.MyFortPresent && node.getOwnedByPlayer() != currentPlayerNumber && node.getPlayerPositionState() == EPlayerPositionState.EnemyPlayerPosition) {
        		FullMapNode maskedNode;
        		if (currentPlayer.getCollectedTreasure()) {
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
            	} else {
            		maskedNode = new FullMapNode(
                        node.getTerrain(),
                        EPlayerPositionState.NoPlayerPresent, // hide enemy when he enters your treasure
                        ETreasureState.MyTreasureIsPresent,
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
        
        if (randomX != -1 && randomY != -1) {
        	Optional<FullMapNode> randomNodeOpt = filteredMap.get(randomX, randomY);
            if (randomNodeOpt.isPresent()) {
                FullMapNode randomNode = randomNodeOpt.get();
                if (randomNode.getPlayerPositionState() != EPlayerPositionState.MyPlayerPosition) {
                	randomNode.setPlayerPositionState(EPlayerPositionState.EnemyPlayerPosition);
                } else {
                    randomNode.setPlayerPositionState(EPlayerPositionState.BothPlayerPosition);
                }
            }
        }
        
        // For the first 16 turns, determine random x and y
        if (this.stateID != this.stateIDBackUp) {
        	System.out.println(stateID);
        	this.stateIDBackUp = this.stateID;
	        if (getTurnCount() <= 16) {
	            int randomX = rand.nextInt(filteredMap.getMaxX() + 1);
	            int randomY = rand.nextInt(filteredMap.getMaxY() + 1);
	
	            this.randomX = randomX;
	            this.randomY = randomY;
	        } 
//	        else {
//	            // show real opponent's location
//	        	int x = -1, y = -1;
//	        	for (FullMapNode node : fullMap) {
//	                if ((node.getPlayerPositionState() == EPlayerPositionState.MyPlayerPosition && 
//	                	node.getOwnedByPlayer() != getPlayerNumberByPlayerID(currentPlayer))) { 
//	                    x = node.getX();
//	                    y = node.getY();
//	                    break;
//	                }
//	            }
//	        	if (x != -1 && y != -1) {
//	        		Optional<FullMapNode> nodeOpt = filteredMap.get(x, y);
//	        		if (nodeOpt.isPresent()) {
//	                    FullMapNode node = nodeOpt.get();
//	                    if (node.getPlayerPositionState() != EPlayerPositionState.MyPlayerPosition && node.getPlayerPositionState() == EPlayerPositionState.BothPlayerPosition) {
//	                    	node.setPlayerPositionState(EPlayerPositionState.EnemyPlayerPosition);
//	                    } else {
//	                    	node.setPlayerPositionState(EPlayerPositionState.BothPlayerPosition);
//	                    }
//	                }
//	        	}
//	        }
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
