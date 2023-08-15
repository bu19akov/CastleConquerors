package exceptions;

public class GameNotFoundException extends GenericExampleException {
    
    public GameNotFoundException(String gameID) {
        super("GameNotFoundException", "Game with ID " + gameID + " was not found!");
    }
}
