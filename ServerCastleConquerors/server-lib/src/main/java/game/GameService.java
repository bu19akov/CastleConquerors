package game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import exceptions.GameFullException;
import exceptions.GameNotFoundException;
import messagesbase.UniqueGameIdentifier;
import messagesbase.UniquePlayerIdentifier;
import messagesbase.messagesfromserver.EPlayerGameState;
import messagesbase.messagesfromclient.PlayerRegistration;
import messagesbase.messagesfromserver.PlayerState;

import org.springframework.scheduling.annotation.Scheduled;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.UUID;

@Service
public class GameService {
    private static final int MAX_GAMES = 99;
    protected static final long EXPIRATION_TIME = 1000 * 60 * 10; // 10 minutes
    private static final int MAX_TURNS = 320;
    private static final int MAX_TURN_TIME = 5000;  // 5 seconds
    private static final Logger logger = LoggerFactory.getLogger(GameService.class);
    protected static ConcurrentMap<UniqueGameIdentifier, GameInfo> games = new ConcurrentHashMap<>();
    
    private PriorityQueue<GameInfo> gamesQueue = new PriorityQueue<>(
        Comparator.comparing(GameInfo::getCreationTime)
    );

    public UniqueGameIdentifier createGame() {
    	UniqueGameIdentifier gameID;
        do {
            gameID = UniqueGameIdentifier.random();
        } while (games.containsKey(gameID));

        GameInfo newGame = new GameInfo(gameID, UUID.randomUUID().toString(), System.currentTimeMillis());

        games.put(gameID, newGame);
        gamesQueue.add(newGame);
        
        logger.info("Game {} was created.", gameID.getUniqueGameID());
        
        while (games.size() > MAX_GAMES) {
            GameInfo oldestGame = gamesQueue.poll();
            if (oldestGame != null) {
                games.remove(oldestGame.getGameID());
                logger.info("Game {} removed due to maximum game limit", oldestGame.getGameID());
            }
        }

        return gameID;
    }
    
    public UniquePlayerIdentifier registerPlayerToGame(String gameID, PlayerRegistration playerReg) {
        checkIfGameExist(gameID, "register a player");

        if (games.get(gameID).getPlayers().size() >= 2) {
        	logger.error("Attempted to register a player to a full game: {}", gameID);
            throw new GameFullException(gameID);
        }
        
        UniquePlayerIdentifier playerID = UniquePlayerIdentifier.random();
		PlayerState newPlayer = new PlayerState(playerReg.getPlayerUsername(), 
									  EPlayerGameState.MustWait,
									  playerID,
									  false);
		games.get(gameID).registerPlayer(newPlayer);
		logger.info("Player {} registered to game {}", playerID, gameID);
		return playerID;
    }
    
    public static void checkIfGameExist(String gameID, String message) {
    	if (!games.containsKey(gameID)) {
        	logger.error("Attempted to {} to a non-existent game: {}", message, gameID);
        	throw new GameNotFoundException(gameID);
        }
    }
    
    public static ConcurrentMap<UniqueGameIdentifier, GameInfo> getGames() {
    	return games;
    }
    
    public static void startGame(UniqueGameIdentifier gameID) {
    	GameInfo game = games.get(gameID);
        Iterator<PlayerState> playerIterator = game.getPlayers().iterator();
        game.setCurrentPlayer(playerIterator.next());  // Set the first player as the current player
        game.getCurrentPlayer().setPlayerGameState(EPlayerGameState.MustAct);
        logger.info("Game {} started. Player {} set as the current player", gameID, game.getCurrentPlayer().getUniquePlayerID());

        // Set the second player to wait
        PlayerState otherPlayer = playerIterator.next();
        otherPlayer.setPlayerGameState(EPlayerGameState.MustWait);
        logger.info("Player {} set to wait in game {}", otherPlayer.getUniquePlayerID(), gameID);
        games.get(gameID).setTurnStartTime(System.currentTimeMillis());
    }
}
