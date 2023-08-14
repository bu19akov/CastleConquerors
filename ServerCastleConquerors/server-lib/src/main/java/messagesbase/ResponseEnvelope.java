package messagesbase;

import java.util.Optional;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import messagesbase.messagesfromclient.ERequestState;
import messagesbase.messagesfromserver.GameState;

@XmlRootElement(name = "responseEnvelope")
@XmlAccessorType(XmlAccessType.NONE)
@XmlSeeAlso({ UniquePlayerIdentifier.class, GameState.class })
public final class ResponseEnvelope<T> {

	private final String defaultErrorMessage = "No error information available. Potentially, everything worked just fine.";

	@XmlElement(name = "exceptionName")
	private final String exceptionName;

	@XmlElement(name = "exceptionMessage")
	private final String exceptionMessage;

	@XmlElement(name = "state", required = true)
	private final ERequestState state;

	@XmlElement(name = "data")
	private final T data;

	public ResponseEnvelope() {
		state = ERequestState.Okay;
		this.data = null;

		exceptionMessage = defaultErrorMessage;
		exceptionName = defaultErrorMessage;
	}

	public ResponseEnvelope(T data) {
		state = ERequestState.Okay;
		this.data = checkNotNull(data,
		      "Data must not be null, use the default ctor without arguments if you would like to send an empty response without data.");

		exceptionMessage = defaultErrorMessage;
		exceptionName = defaultErrorMessage;
	}

	public ResponseEnvelope(Exception e) {
		this(checkNotNull(e, "Exception should not be null, use this ctor only if you want to report errors!").getClass().getName(), e.getMessage());
	}

	public ResponseEnvelope(String exceptionName, String exceptionMessage) {
		state = ERequestState.Error;
		this.data = null;

		this.exceptionMessage = checkNotNullOrEmpty(exceptionMessage,
		      "Provide a non-null and non-empty exception message. Use this ctor only if you want to report errors.");
		this.exceptionName = checkNotNullOrEmpty(exceptionName,
		      "Provide a non-null and non-empty exception name. Use this ctor only if you want to report errors.");
	}

	public String getExceptionName() {
		return exceptionName;
	}

	public String getExceptionMessage() {
		return exceptionMessage;
	}

	public ERequestState getState() {
		return state;
	}

	public Optional<T> getData() {
		return Optional.ofNullable(data);
	}

	private static <T> T checkNotNull(T reference, String errorMessage) {
		if (reference == null) {
			throw new IllegalArgumentException(errorMessage);
		}
		return reference;
	}

	private static String checkNotNullOrEmpty(String reference, String errorMessage) {
		if (reference == null || reference.isEmpty()) {
			throw new IllegalArgumentException(errorMessage);
		}
		return reference;
	}
}
