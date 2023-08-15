package exceptions;

public class GameFullException extends GenericExampleException {
    
    public GameFullException(String gameId) {
        super("GameFullException", "Game with ID " + gameId + " already has two players!");
    }
}

