package exceptions;

public class PlayerIsAlreadyRegisteredException extends GenericExampleException {
    
    private static final long serialVersionUID = 1L;

	public PlayerIsAlreadyRegisteredException(String playerID, String gameID) {
        super("PlayerIsAlreadyRegisteredException", "Player with ID " + playerID + " is already registered to game with ID " + gameID);
    }
}

