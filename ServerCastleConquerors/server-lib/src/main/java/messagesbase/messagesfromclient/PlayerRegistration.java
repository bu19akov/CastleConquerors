package messagesbase.messagesfromclient;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "playerRegistration")
@XmlAccessorType(XmlAccessType.NONE)
public final class PlayerRegistration {

	@XmlElement(name = "playerUsername", required = true)
	@NotNull
	@Size(min = 1, max = 50)
	private final String playerUsername;

	public PlayerRegistration() {
		playerUsername = "";
	}

	public PlayerRegistration(String playerUsername) {
		super();
		this.playerUsername = checkNotNullOrEmpty(playerUsername, "Provide a non-null and non-empty first name");
	}

	public String getPlayerUsername() {
		return playerUsername;
	}

	private static String checkNotNullOrEmpty(String reference, String errorMessage) {
		if (reference == null || reference.isEmpty()) {
			throw new IllegalArgumentException(errorMessage);
		}
		return reference;
	}
}
