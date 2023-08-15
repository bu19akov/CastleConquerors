package messagesbase.messagesfromserver;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "mapNode")
@XmlAccessorType(XmlAccessType.NONE)
public final class FullMapNode {
	private final int MAX_FULL_MAP_X = 19;
	private final int MAX_FULL_MAP_Y = 9;

	@XmlElement(name = "playerPositionState", required = true)
	private EPlayerPositionState playerPositionState;

	@XmlElement(name = "terrain", required = true)
	private final ETerrain terrain;

	@XmlElement(name = "treasureState", required = true)
	private final ETreasureState treasureState;

	@XmlElement(name = "fortState", required = true)
	private final EFortState fortState;

	@XmlElement(name = "X", required = true)
	@Positive
	@Min(0)
	@Max(MAX_FULL_MAP_X)
	private final int X;

	@XmlElement(name = "Y", required = true)
	@Positive
	@Min(0)
	@Max(MAX_FULL_MAP_Y)
	private final int Y;
	
	private int ownedByPlayer; // 1 for Player 1, 2 for Player 2, 0 for neutral

	public FullMapNode() {
		super();
		this.terrain = null;
		this.X = 0;
		this.Y = 0;
		this.playerPositionState = null;
		this.treasureState = null;
		this.fortState = null;
		this.ownedByPlayer = 0;
	}

	public FullMapNode(ETerrain terrain, EPlayerPositionState playerPos, ETreasureState treasure, EFortState fort, int X, int Y, int playerNumber) {
		super();

		if (terrain == null || playerPos == null || treasure == null || fort == null) {
			throw new IllegalArgumentException("Arguments must not be null");
		}
		if (X < 0 || Y < 0) {
			throw new IllegalArgumentException("X and Y must not be negative");
		}

		this.terrain = terrain;
		this.X = X;
		this.Y = Y;
		this.playerPositionState = playerPos;
		this.treasureState = treasure;
		this.fortState = fort;
		this.ownedByPlayer = playerNumber;
	}

	public int getX() {
		return X;
	}

	public int getY() {
		return Y;
	}

	public int getOwnedByPlayer() {
		return ownedByPlayer;
	}
	
	public void setOwnedByPlayer(int ownedByPlayer) {
		this.ownedByPlayer = ownedByPlayer;
	}

	public EPlayerPositionState getPlayerPositionState() {
		return playerPositionState;
	}
	
	public void setPlayerPositionState(EPlayerPositionState state) {
		this.playerPositionState = state;
	}

	public ETerrain getTerrain() {
		return terrain;
	}

	public ETreasureState getTreasureState() {
		return treasureState;
	}

	public EFortState getFortState() {
		return fortState;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + X;
		result = prime * result + Y;
		return result;
	}

	@Override
	public String toString() {
		return "FMN [P=" + playerPositionState + ", T=" + terrain + ", TS=" + treasureState + ", FS=" + fortState + ", X=" + X + ", Y=" + Y + "]\n";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FullMapNode other = (FullMapNode) obj;
		if (X != other.X)
			return false;
		if (Y != other.Y)
			return false;
		return true;
	}
}
