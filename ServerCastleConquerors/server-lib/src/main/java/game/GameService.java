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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.UUID;

@Service
public class GameService {
    private static final int MAX_GAMES = 99;
    private static final long EXPIRATION_TIME = 600_000; // 10 minutes
    private static final int MAX_TURNS = 320;
    private static final int MAX_TURN_TIME = 5_000; // 5 seconds
    private static final Logger logger = LoggerFactory.getLogger(GameService.class);

    protected static final ConcurrentMap<UniqueGameIdentifier, GameInfo> games = new ConcurrentHashMap<>();

    private final PriorityQueue<GameInfo> gamesQueue = new PriorityQueue<>(
        Comparator.comparing(GameInfo::getCreationTime)
    );

    public UniqueGameIdentifier createGame() {
        UniqueGameIdentifier gameID = generateUniqueGameId();

        GameInfo newGame = new GameInfo(gameID, UUID.randomUUID().toString(), System.currentTimeMillis());
        games.put(gameID, newGame);
        gamesQueue.add(newGame);

        logGameCreationAndCleanupOldGames(gameID);
        return gameID;
    }

    private UniqueGameIdentifier generateUniqueGameId() {
        UniqueGameIdentifier gameID;
        do {
            gameID = UniqueGameIdentifier.random();
        } while (games.containsKey(gameID));
        return gameID;
    }

    private void logGameCreationAndCleanupOldGames(UniqueGameIdentifier gameID) {
        logger.info("Game {} was created.", gameID.getUniqueGameID());

        while (games.size() > MAX_GAMES) {
            GameInfo oldestGame = gamesQueue.poll();
            if (oldestGame != null) {
                games.remove(oldestGame.getGameID());
                logger.info("Game {} removed due to maximum game limit", oldestGame.getGameID());
            }
        }
    }

    public UniquePlayerIdentifier registerPlayerToGame(UniqueGameIdentifier gameID, PlayerRegistration playerReg) {
        checkIfGameExist(gameID, "register a player");
        checkIfGameIsFull(gameID);

        UniquePlayerIdentifier playerID = UniquePlayerIdentifier.random();
        PlayerState newPlayer = new PlayerState(
            playerReg.getPlayerUsername(), 
            EPlayerGameState.MustWait,
            playerID,
            false
        );

        games.get(gameID).registerPlayer(newPlayer);

        logger.info("Player {} registered to game {}", playerID, gameID);
        return playerID;
    }

    private void checkIfGameIsFull(UniqueGameIdentifier gameID) {
        if (games.get(gameID).getPlayers().size() >= 2) {
            logger.error("Attempted to register a player to a full game: {}", gameID);
            throw new GameFullException(gameID.getUniqueGameID());
        }
    }

    public static void checkIfGameExist(UniqueGameIdentifier gameID, String message) {
        if (!games.containsKey(gameID)) {
            logger.error("Attempted to {} to a non-existent game: {}", message, gameID);
            throw new GameNotFoundException(gameID.getUniqueGameID());
        }
    }

    public static ConcurrentMap<UniqueGameIdentifier, GameInfo> getGames() {
        return games;
    }

    public static void startGame(UniqueGameIdentifier gameID) {
        GameInfo game = games.get(gameID);
        Iterator<PlayerState> playerIterator = game.getPlayers().iterator();

        setInitialPlayersForGame(game, playerIterator);
        games.get(gameID).setTurnStartTime(System.currentTimeMillis());
    }

    private static void setInitialPlayersForGame(GameInfo game, Iterator<PlayerState> playerIterator) {
        game.setCurrentPlayer(playerIterator.next());
        game.getCurrentPlayer().setPlayerGameState(EPlayerGameState.MustAct);

        PlayerState otherPlayer = playerIterator.next();
        otherPlayer.setPlayerGameState(EPlayerGameState.MustWait);

        logger.info("Game {} started. Player {} set as the current player", game.getGameID(), game.getCurrentPlayer().getUniquePlayerID());
        logger.info("Player {} set to wait in game {}", otherPlayer.getUniquePlayerID(), game.getGameID());
    }

    public static void checkIfPlayerExist(UniqueGameIdentifier gameId, String playerId) {
        if (!getGames().get(gameId).containsPlayerWithID(playerId)) {
            logger.error("No player {} found in game {}", playerId, gameId);
            throw new IllegalArgumentException(playerId);
        }
    }
    
//  public PriorityQueue<GameInfo> getGamesQueue() {
//	return this.gamesQueue;
//}
//
//    @Scheduled(fixedRate = 30000) // Check every 30 seconds
//  public void removeExpiredGames() {
//  	long now = getCurrentTimeMillis();
//      while (!gamesQueue.isEmpty()) {
//          GameInfo oldestGame = gamesQueue.peek();
//          if (now - oldestGame.getCreationTime() > EXPIRATION_TIME) {
//              gamesQueue.poll();
//              endGame(oldestGame.getGameID(), "The time has expired!");
//          } else {
//              break;
//          }
//      }
//  }
//  
//  protected long getCurrentTimeMillis() {
//      return System.currentTimeMillis();
//  }
//  
//  @Scheduled(fixedRate = 1000) // Check every second
//  public void checkTurnTime() {
//      games.values().forEach(game -> {
//          // Check if turn time has been exceeded
//          if (game.getTurnStartTime() != 0 && System.currentTimeMillis() - game.getTurnStartTime() > MAX_TURN_TIME) {
//              endGame(game.getGameID(), "Turn time exceeded");
//          }
//      });
//  }
//    
//    public static void nextTurn(String gameId) {
//    	GameInfo game = games.get(gameId);
//        game.getCurrentPlayer().setPlayerGameState(EMyPlayerGameState.MustWait);
//        game.setStateId(UUID.randomUUID().toString());
//        logger.info("Current turn ended for player {} in game {}", game.getCurrentPlayer().getPlayerId(), gameId);
//
//        for (Player player : game.getPlayers()) {
//            if (player != game.getCurrentPlayer()) {
//                game.setCurrentPlayer(player);
//                game.getCurrentPlayer().setPlayerGameState(EMyPlayerGameState.MustAct);
//                logger.info("New turn started for player {} in game {}", game.getCurrentPlayer().getPlayerId(), gameId);
//                break;
//            }
//        }
//        // Check if maximum turns reached
//        if (games.get(gameId).getTurnCount() >= MAX_TURNS) {
//            endGame(gameId, "Maximum turns reached");
//        } else {
//            games.get(gameId).setTurnStartTime(System.currentTimeMillis());
//        }
//    }
//    
//    private static void endGame(String gameId, String reason) {
//        logger.info("Game {} was ended! Reason: {}", gameId, reason);
//
//        GameInfo game = games.get(gameId);
//        Player currentPlayer = game.getCurrentPlayer();
//        
//        // If the current player is found, they lose the game
//        if (currentPlayer != null) {
//            currentPlayer.setPlayerGameState(EMyPlayerGameState.Lost);
//            logger.info("Player {} lost the game: {}", currentPlayer.getPlayerId(), gameId);
//            
//            // Set second player as winner
//            game.getPlayers().stream()
//                .filter(player -> !player.getPlayerId().equals(currentPlayer.getPlayerId()))
//                .forEach(player -> {
//                    player.setPlayerGameState(EMyPlayerGameState.Won);
//                    logger.info("Player {} won the game: {}", player.getPlayerId(), gameId);
//                });
//        }
//
//        // Remove game from active games list
//        games.remove(gameId);
//    }
}
