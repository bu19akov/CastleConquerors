package game;

import java.util.Set;

import messagesbase.messagesfromserver.PlayerState;

import java.util.HashSet;

import messagesbase.UniqueGameIdentifier;
import messagesbase.messagesfromserver.FullMap;

public class GameInfo {
    private final UniqueGameIdentifier gameID;
    private long creationTime;
    private String stateID;
    private Set<PlayerState> players = new HashSet<>();
    private PlayerState currentPlayer;
    private FullMap fullMap;
    private int turnCount = 0;
    private long turnStartTime = 0;

    public GameInfo(UniqueGameIdentifier gameID, String stateID, long creationTime) {
        this.gameID = gameID;
        this.stateID = stateID;
        this.creationTime = creationTime;
        this.fullMap = new FullMap(new HashSet<>());
    }
    
    public UniqueGameIdentifier getGameID() {
    	return this.gameID;
    }

	public String getStateID() {
		return this.stateID;
	}

	public void setStateID(String newStateID) {
		this.stateID = newStateID;
	}

	public long getCreationTime() {
		return creationTime;
	}
	
	public void registerPlayer(PlayerState player) {
        this.players.add(player);
    }

    public Set<PlayerState> getPlayers() {
        return this.players;
    }
    
    public boolean containsPlayerWithID(String playerID) {
    	for (PlayerState player : this.players) {
    		if (player.getUniquePlayerID().equals(playerID)) {
    			return true;
    		}
    	}
    	return false;
    }
    
    public PlayerState getCurrentPlayer() {
    	return this.currentPlayer;
    }
    
    public void setCurrentPlayer(PlayerState player) {
    	this.currentPlayer = player;
    }
    
    public void setTurnStartTime(long turnStartTime) {
        this.turnStartTime = turnStartTime;
    }

    public long getTurnStartTime() {
        return this.turnStartTime;
    }

    public int getTurnCount() {
        return this.turnCount;
    }

    public void incrementTurnCount() {
        this.turnCount++;
    }
}
