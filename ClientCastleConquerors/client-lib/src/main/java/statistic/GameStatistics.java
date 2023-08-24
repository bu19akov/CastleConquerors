package statistic;

public class GameStatistics {
    private String playerUsername;
    private int wonGames;
    private int lostGames;

    public GameStatistics(String playerUsername, int wonGames, int lostGames) {
        this.playerUsername = playerUsername;
        this.wonGames = wonGames;
        this.lostGames = lostGames;
    }

	public String getPlayerUsername() {
		return playerUsername;
	}

	public void setPlayerUsername(String playerUsername) {
		this.playerUsername = playerUsername;
	}

	public int getWonGames() {
		return wonGames;
	}

	public void setWonGames(int wonGames) {
		this.wonGames = wonGames;
	}

	public int getLostGames() {
		return lostGames;
	}

	public void setLostGames(int lostGames) {
		this.lostGames = lostGames;
	}

    
}
