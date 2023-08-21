package messagesbase.messagesfromclient;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import messagesbase.UniquePlayerIdentifier;

@XmlRootElement(name = "playerMove")
@XmlAccessorType(XmlAccessType.NONE)
public final class PlayerMove extends UniquePlayerIdentifier {

    @XmlElement(name = "move", required = true)
    private final EMove move;

    public PlayerMove() {
        move = null;
    }

    public PlayerMove(String uniquePlayerID, EMove move) {
        super(checkNotNullOrEmpty(uniquePlayerID, "Each move must contain a non-null and non-empty player id"));
        this.move = checkNotNull(move, "Move must not be null");
    }

    private PlayerMove(UniquePlayerIdentifier uniquePlayerID, EMove move) {
        this(checkNotNull(uniquePlayerID, "Player id must not be null").getUniquePlayerID(), move);
    }

    public static PlayerMove of(String uniquePlayerID, EMove move) {
        return new PlayerMove(uniquePlayerID, move);
    }

    public static PlayerMove of(UniquePlayerIdentifier uniquePlayerID, EMove move) {
        return new PlayerMove(uniquePlayerID, move);
    }

    public EMove getMove() {
        return move;
    }

    // Helper method to check if an object is not null
    private static <T> T checkNotNull(T obj, String errorMessage) {
        if (obj == null) {
            throw new IllegalArgumentException(errorMessage);
        }
        return obj;
    }

    // Helper method to check if a string is not null or empty
    private static String checkNotNullOrEmpty(String str, String errorMessage) {
        if (str == null || str.isEmpty()) {
            throw new IllegalArgumentException(errorMessage);
        }
        return str;
    }
}
