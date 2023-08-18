package exceptions;

public class GameNotFoundException extends GenericExampleException {
    
    private static final long serialVersionUID = 1L;

	public GameNotFoundException(String gameID) {
        super("GameNotFoundException", "Game with ID " + gameID + " was not found!");
    }
}
