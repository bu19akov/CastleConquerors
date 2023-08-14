package messagesbase;

import java.util.Random;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@XmlRootElement(name = "uniqueGameIdentifier")
@XmlAccessorType(XmlAccessType.NONE)
public final class UniqueGameIdentifier {

	private final int GAME_ID_LENGTH = 5;

	@XmlElement(name = "uniqueGameID", required = true)
	@NotNull
	@Size(min = GAME_ID_LENGTH, max = GAME_ID_LENGTH)
	private final String uniqueGameID;

	public UniqueGameIdentifier(String uniqueGameID) {
		super();
		this.uniqueGameID = checkNotNullOrEmpty(uniqueGameID, "Game ID should not be null or empty");
		checkLength(this.uniqueGameID, GAME_ID_LENGTH, "Game ID should have an exact length of " + GAME_ID_LENGTH + " characters");
	}

	public static UniqueGameIdentifier of(String uniqueGameID) {
		return new UniqueGameIdentifier(uniqueGameID);
	}

	public UniqueGameIdentifier() {
		uniqueGameID = null;
	}

	public String getUniqueGameID() {
		return uniqueGameID;
	}
	
	public static UniqueGameIdentifier random() {
        String characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder gameID = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            int randomIndex = new Random().nextInt(characters.length());
            gameID.append(characters.charAt(randomIndex));
        }
        return new UniqueGameIdentifier(gameID.toString());
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((uniqueGameID == null) ? 0 : uniqueGameID.hashCode());
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
		UniqueGameIdentifier other = (UniqueGameIdentifier) obj;
		if (uniqueGameID == null) {
			if (other.uniqueGameID != null)
				return false;
		}
		else if (!uniqueGameID.equals(other.uniqueGameID))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "GameIdentifier=" + uniqueGameID;
	}

	private static String checkNotNullOrEmpty(String reference, String errorMessage) {
		if (reference == null || reference.isEmpty()) {
			throw new IllegalArgumentException(errorMessage);
		}
		return reference;
	}

	private static void checkLength(String reference, int length, String errorMessage) {
		if (reference.length() != length) {
			throw new IllegalArgumentException(errorMessage);
		}
	}
}
