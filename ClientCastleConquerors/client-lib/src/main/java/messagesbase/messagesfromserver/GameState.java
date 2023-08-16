package messagesbase.messagesfromserver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "gameState")
@XmlAccessorType(XmlAccessType.NONE)
public final class GameState {
	@XmlElementWrapper(name = "players")
	@XmlElement(name = "player")
	private final Set<PlayerState> players = new HashSet<>();

	@XmlElement(name = "map")
	private FullMap map;

	@XmlElement(name = "gameStateId", required = true)
	private final String gameStateId;

	public GameState() {
		gameStateId = UUID.randomUUID().toString();
	}

	public GameState(FullMap map, Collection<PlayerState> players, String gameStateID) {
		if (map == null) {
			throw new IllegalArgumentException("Map must not be null but can be empty. Use, for example, a different CTOR if no map information is available.");
		}
		if (gameStateID == null || gameStateID.trim().isEmpty()) {
			throw new IllegalArgumentException("Game state ID must be defined");
		}

		this.map = map;
		this.gameStateId = gameStateID;

		if (players != null && !players.isEmpty()) {
			this.players.addAll(players);
		}
	}

	public GameState(Collection<PlayerState> players, String gameStateID) {
		this(new FullMap(), players, gameStateID);
	}

	public GameState(String gameStateID) {
		this(new ArrayList<>(), gameStateID);
	}

	public Set<PlayerState> getPlayers() {
		return Collections.unmodifiableSet(players);
	}

	public FullMap getMap() {
		return (map != null) ? map : new FullMap();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((gameStateId == null) ? 0 : gameStateId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GameState other = (GameState) obj;
		if (gameStateId == null) {
			if (other.gameStateId != null)
				return false;
		} else if (!gameStateId.equals(other.gameStateId))
			return false;
		return true;
	}

	public String getGameStateId() {
		return gameStateId;
	}

	@Override
	public String toString() {
		return "GameState with gameStateId=" + gameStateId;
	}
}
