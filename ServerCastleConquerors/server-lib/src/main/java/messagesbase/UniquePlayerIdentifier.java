package messagesbase;

import java.util.UUID;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@XmlRootElement(name = "uniquePlayerIdentifier")
@XmlAccessorType(XmlAccessType.NONE)
public class UniquePlayerIdentifier {

	@XmlElement(name = "uniquePlayerID", required = true)
	@NotNull
	@Size(min = 1)
	private final String uniquePlayerID;

	public UniquePlayerIdentifier() {
		this.uniquePlayerID = "";
	}

	public UniquePlayerIdentifier(String uniquePlayerID) {
		if (uniquePlayerID == null) {
			uniquePlayerID = "";
		}

		this.uniquePlayerID = uniquePlayerID;
	}

	public UniquePlayerIdentifier(UniquePlayerIdentifier uniquePlayerID) {
		if (uniquePlayerID == null) {
			throw new IllegalArgumentException("Unique player id object to clone must not be null.");
		}
		
		this.uniquePlayerID = uniquePlayerID.uniquePlayerID;
	}

	public String getUniquePlayerID() {
		return uniquePlayerID;
	}

	public static UniquePlayerIdentifier of(String uniquePlayerID) {
		return new UniquePlayerIdentifier(uniquePlayerID);
	}

	public static UniquePlayerIdentifier random() {
		return new UniquePlayerIdentifier(UUID.randomUUID().toString());
	}

	public boolean isDefined() {
		return uniquePlayerID != null && !uniquePlayerID.isEmpty();
	}

	public static boolean isDefined(UniquePlayerIdentifier identifier) {
		return identifier != null && identifier.isDefined();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((uniquePlayerID == null) ? 0 : uniquePlayerID.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof UniquePlayerIdentifier))
			return false;
		UniquePlayerIdentifier other = (UniquePlayerIdentifier) obj;
		if (uniquePlayerID == null) {
			if (other.uniquePlayerID != null)
				return false;
		}
		else if (!uniquePlayerID.equals(other.uniquePlayerID))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "PlayerIdentifier=" + uniquePlayerID;
	}
}
