package exceptions;

public class GameFullException extends GenericExampleException {
    
    private static final long serialVersionUID = 1L;

	public GameFullException(String gameId) {
        super("GameFullException", "Game with ID " + gameId + " already has two players!");
    }
}

