package exceptions;

public class ClientNetworkException extends Exception {
    private static final long serialVersionUID = 1L;

    public ClientNetworkException(String message) {
        super(message);
    }
}