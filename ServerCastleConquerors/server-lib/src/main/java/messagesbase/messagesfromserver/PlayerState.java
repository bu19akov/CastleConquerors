package messagesbase.messagesfromserver;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import messagesbase.UniquePlayerIdentifier;

@XmlRootElement(name = "player")
@XmlAccessorType(XmlAccessType.NONE)
public class PlayerState extends UniquePlayerIdentifier {

	@XmlElement(name = "playerUsername", required = true)
	private final String playerUsername;

	@XmlElement(name = "state", required = true)
	private EPlayerGameState state;

	@XmlElement(name = "collectedTreasure", required = true)
	private boolean collectedTreasure;
	
	@XmlElement(name = "revealedTreasure", required = true)
	private boolean revealedTreasure;
	
	private boolean revealedEnemyFort;

	public PlayerState() {
		super();
		this.playerUsername = null;
		this.collectedTreasure = false;
		this.state = null;
	}

	public PlayerState(String playerUsername, EPlayerGameState state, UniquePlayerIdentifier playerIdentifier,
	      boolean collectedTreasure) {
		super(checkNotNull(playerIdentifier, "Player identifier must not be null").getUniquePlayerID());
		this.playerUsername = checkNotNull(playerUsername, "Player username must not be null");
		this.state = checkNotNull(state, "Player state should not be null");
		this.collectedTreasure = collectedTreasure;
		this.revealedTreasure = false;
	}
	
	public boolean getRevealedTreasure() {
		return this.revealedTreasure;
	}
	
	public void setRevealedTreasureToTrue() {
		this.revealedTreasure = true;
	}
	
	public boolean getRevealedEnemyFort() {
		return this.revealedEnemyFort;
	}
	
	public void setRevealedEnemyFortToTrue() {
		this.revealedEnemyFort = true;
	}

	public String getPlayerUsername() {
		return playerUsername;
	}

	public EPlayerGameState getState() {
		return state;
	}
	
	public void setPlayerGameState(EPlayerGameState state) {
		this.state = state;
	}
	
	public boolean getCollectedTreasure() {
		return this.collectedTreasure;
	}
	
	public void setCollectedTreasureToTrue() {
		this.collectedTreasure = true;
	}

	@Override
	public String toString() {
		return "Player [username=" + playerUsername + ", state=" + state + ", " + super.toString() + "]";
	}

	private static <T> T checkNotNull(T reference, String errorMessage) {
		if (reference == null) {
			throw new IllegalArgumentException(errorMessage);
		}
		return reference;
	}
}
