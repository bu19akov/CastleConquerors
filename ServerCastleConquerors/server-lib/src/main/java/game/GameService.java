package game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import exceptions.GameFullException;
import exceptions.GameNotFoundException;
import messagesbase.UniqueGameIdentifier;
import messagesbase.UniquePlayerIdentifier;
import messagesbase.messagesfromserver.EFortState;
import messagesbase.messagesfromserver.EPlayerGameState;
import messagesbase.messagesfromserver.EPlayerPositionState;
import messagesbase.messagesfromserver.ETerrain;
import messagesbase.messagesfromserver.ETreasureState;
import messagesbase.messagesfromserver.FullMap;
import messagesbase.messagesfromserver.FullMapNode;
import messagesbase.messagesfromclient.EMove;
import messagesbase.messagesfromclient.PlayerMove;
import messagesbase.messagesfromclient.PlayerRegistration;
import messagesbase.messagesfromserver.PlayerState;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;

@Service
public class GameService {
    private static final int MAX_GAMES = 99;
    private static final long EXPIRATION_TIME = 600_000; // 10 minutes
    private static final int MAX_TURNS = 320;
    private static final int MAX_TURN_TIME = 10_000; // 10 seconds
    private static final Logger logger = LoggerFactory.getLogger(GameService.class);

    protected static final ConcurrentMap<UniqueGameIdentifier, GameInfo> games = new ConcurrentHashMap<>();
    private static final Map<FieldTransition, Integer> FIELD_TRANSITION_COST;
    static {
        Map<FieldTransition, Integer> costMap = new HashMap<>();
        costMap.put(new FieldTransition(ETerrain.Grass, ETerrain.Grass), 2);
        costMap.put(new FieldTransition(ETerrain.Grass, ETerrain.Mountain), 3);
        costMap.put(new FieldTransition(ETerrain.Mountain, ETerrain.Grass), 3);
        costMap.put(new FieldTransition(ETerrain.Mountain, ETerrain.Mountain), 4);

        FIELD_TRANSITION_COST = Collections.unmodifiableMap(costMap);
    }


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

        UniquePlayerIdentifier playerID = UniquePlayerIdentifier.of(playerReg.getPlayerUsername());

        if (games.get(gameID).containsPlayerWithID(playerID.getUniquePlayerID())) {
        	return playerID;
        }
        
        PlayerState newPlayer = new PlayerState(
            playerReg.getPlayerUsername(), 
            EPlayerGameState.MustWait,
            playerID,
            false
        );

        games.get(gameID).registerPlayer(newPlayer);

        logger.info("Player {} registered to game {}", playerID.getUniquePlayerID(), gameID.getUniqueGameID());
        return playerID;
    }

    private void checkIfGameIsFull(UniqueGameIdentifier gameID) {
        if (games.get(gameID).getPlayers().size() >= 2) {
            logger.error("Attempted to register a player to a full game: {}", gameID.getUniqueGameID());
            throw new GameFullException(gameID.getUniqueGameID());
        }
    }

    public static void checkIfGameExist(UniqueGameIdentifier gameID, String message) {
        if (!games.containsKey(gameID)) {
            logger.error("Attempted to {} to a non-existent game: {}", message, gameID.getUniqueGameID());
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
    
    public void processMove(UniqueGameIdentifier gameID, PlayerMove playerMove) {
        GameInfo game = games.get(gameID);
        
        // Validate if it's the player's turn
        if(!game.getCurrentPlayer().getUniquePlayerID().equals(playerMove.getUniquePlayerID())) {
            logger.error("It's not the turn of player {}", playerMove.getUniquePlayerID());
            throw new IllegalArgumentException("Not your turn!");
        }
        
        FullMap fullMap = game.getFullMap();
        PlayerState currentPlayer = game.getCurrentPlayer();
        
        FullMapNode currentNode = getCurrentPosition(fullMap, currentPlayer, game);
        int currentX = currentNode.getX();
        int currentY = currentNode.getY();
        
        // Calculate new coordinates based on the move
        int newX = currentX;
        int newY = currentY;

        switch (playerMove.getMove()) {
	        case Up:
	            newY = currentY - 1;
	            break;
	        case Down:
	            newY = currentY + 1;
	            break;
	        case Right:
	            newX = currentX + 1;
	            break;
	        case Left:
	            newX = currentX - 1;
	            break;
	    }
        
        if (newX < 0 || newX > fullMap.getMaxX() || newY < 0 || newY > fullMap.getMaxY()) {
            logger.error("Player {} attempted to move outside the map boundaries!", playerMove.getUniquePlayerID());
            endGame(gameID, "Player moved outside of the map boundaries!", false);
            return;
        }
        
        FullMapNode nextNode = getFullMapNodeByXY(fullMap, newX, newY);
        
        if (nextNode.getTerrain() == ETerrain.Water) {
            endGame(gameID, "Player moved into the water", false);
            return; // Exit the method since the game is ended
        }
        
        int movesRequired = getTransitionCost(currentNode.getTerrain(), nextNode.getTerrain());
        Map<EMove, Integer> playerMoveCounter = game.getMoveCounter().computeIfAbsent(currentPlayer, k -> new HashMap<>());
        
        if (!playerMoveCounter.containsKey(playerMove.getMove())) {
        	playerMoveCounter.clear();
            playerMoveCounter.put(playerMove.getMove(), movesRequired);
        }
        
        int remainingMoves = playerMoveCounter.get(playerMove.getMove());
        remainingMoves--;

        // Update FullMap with the new player position
        if (remainingMoves == 0 && (newX != currentX || newY != currentY)) {
            // Remove player from the current node
            currentNode.setPlayerPositionState(EPlayerPositionState.NoPlayerPresent);
            if (currentNode.getFortState() != EFortState.MyFortPresent && currentNode.getTreasureState() != ETreasureState.MyTreasureIsPresent) {
            	currentNode.setOwnedByPlayer(0); // TODO check BothPlayer
            }

            // Place player on the new node
            FullMapNode newNode = getFullMapNodeByXY(fullMap, newX, newY);
            newNode.setPlayerPositionState(EPlayerPositionState.MyPlayerPosition);
            
            if (newNode.getTreasureState() == ETreasureState.MyTreasureIsPresent && 
                    newNode.getOwnedByPlayer() == game.getPlayerNumberByPlayerID(currentPlayer)) {
                        currentPlayer.setCollectedTreasureToTrue();
                }
            
            // Reveal neighbor nodes (radius 1), if your new node is mountain
            if (newNode.getTerrain() == ETerrain.Mountain) {
            	System.out.println("PLAYER HAS ENTERED A MOUNTAIN");
                // Loop through each of the 8 neighboring nodes
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        // Skip the center node (the player's current position)
                        if (dx == 0 && dy == 0) {
                            continue;
                        }
                        
                        int neighborX = newX + dx;
                        int neighborY = newY + dy;

                        // Ensure the neighboring node is within the map boundaries
                        if (neighborX >= 0 && neighborX <= fullMap.getMaxX() &&
                            neighborY >= 0 && neighborY <= fullMap.getMaxY()) {

                            FullMapNode neighborNode = getFullMapNodeByXY(fullMap, neighborX, neighborY);
                            
                            if (neighborNode.getOwnedByPlayer() == 0 || 
                                neighborNode.getOwnedByPlayer() == game.getPlayerNumberByPlayerID(currentPlayer)) {
                                neighborNode.setOwnedByPlayer(game.getPlayerNumberByPlayerID(currentPlayer));
                                if (neighborNode.getTreasureState() == ETreasureState.MyTreasureIsPresent) {
                                	System.out.println("FOUND TREASURE ON: " + neighborX + " " + neighborY);
                                	currentPlayer.setRevealedTreasureToTrue();
                                }
                            }
                            if (currentPlayer.getCollectedTreasure() && neighborNode.getFortState() == EFortState.MyFortPresent && neighborNode.getOwnedByPlayer() != game.getPlayerNumberByPlayerID(currentPlayer)) {
                            	System.out.println("OPPONENTS CASTLE IS REVEALED");
                            	currentPlayer.setRevealedEnemyFortToTrue();
                            }
                        }
                    }
                }
            }
            
            if (newNode.getFortState() == EFortState.MyFortPresent && currentPlayer.getCollectedTreasure() && newNode.getOwnedByPlayer() != game.getPlayerNumberByPlayerID(currentPlayer)) {
                // Player wins!
                endGame(gameID, "Player " + currentPlayer.getUniquePlayerID() + " has captured the enemy fort with the treasure!", true);
                return;
            }
            
            newNode.setOwnedByPlayer(game.getPlayerNumberByPlayerID(currentPlayer));  // TODO check BothPlayer
            
            playerMoveCounter.remove(playerMove.getMove());
        } else {
            playerMoveCounter.put(playerMove.getMove(), remainingMoves);
        }

        // Update the game state
        nextTurn(gameID);
    }
    
    private int getTransitionCost(ETerrain from, ETerrain to) {
        return FIELD_TRANSITION_COST.get(new FieldTransition(from, to));
    }

    private FullMapNode getCurrentPosition(FullMap fullMap, PlayerState player, GameInfo game) {
        for (FullMapNode node : fullMap) {
            if ((node.getPlayerPositionState() == EPlayerPositionState.MyPlayerPosition && 
            	node.getOwnedByPlayer() == game.getPlayerNumberByPlayerID(player)) || node.getPlayerPositionState() == EPlayerPositionState.BothPlayerPosition) { // TRY TO FIX NO PLAYER POSITION FOUND
                return node;
            }
        }
        throw new IllegalStateException("Player position not found!");
    }

    // TODO !!!!!!! TO BE REMOVED (THERE IS ANALOG IN FULLMAP CLASS)
    private FullMapNode getFullMapNodeByXY(FullMap fullMap, int x, int y) {
        for (FullMapNode node : fullMap) {
            if (node.getX() == x && node.getY() == y) {
                return node;
            }
        }
        throw new IllegalArgumentException("No map node found for the given X and Y coordinates");
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
    public static void nextTurn(UniqueGameIdentifier gameID) {
    	GameInfo game = games.get(gameID);
        game.getCurrentPlayer().setPlayerGameState(EPlayerGameState.MustWait);
        game.setStateID(UUID.randomUUID().toString());
        logger.info("Current turn ended for player {} in game {}", game.getCurrentPlayer().getUniquePlayerID(), gameID.getUniqueGameID());

        for (PlayerState player : game.getPlayers()) {
            if (player != game.getCurrentPlayer()) {
                game.setCurrentPlayer(player);
                game.getCurrentPlayer().setPlayerGameState(EPlayerGameState.MustAct);
                logger.info("New turn started for player {} in game {}", game.getCurrentPlayer().getUniquePlayerID(), gameID.getUniqueGameID());
                break;
            }
        }
        // Check if maximum turns reached
        if (games.get(gameID).getTurnCount() >= MAX_TURNS) {
            endGame(gameID, "Maximum turns reached", false);
        } else {
            games.get(gameID).setTurnStartTime(System.currentTimeMillis());
        }
    }
    
    private static void endGame(UniqueGameIdentifier gameID, String reason, boolean currentPlayerWins) {
        logger.info("Game {} was ended! Reason: {}", gameID.getUniqueGameID(), reason);

        GameInfo game = games.get(gameID);
        PlayerState currentPlayer = game.getCurrentPlayer();
        
        if (currentPlayer != null) {
            if (currentPlayerWins) {
                currentPlayer.setPlayerGameState(EPlayerGameState.Won);
                logger.info("Player {} won the game: {}", currentPlayer.getUniquePlayerID(), gameID);
                
                // Set second player as loser
                game.getPlayers().stream()
                    .filter(player -> !player.getUniquePlayerID().equals(currentPlayer.getUniquePlayerID()))
                    .forEach(player -> {
                        player.setPlayerGameState(EPlayerGameState.Lost);
                        logger.info("Player {} lost the game: {}", player.getUniquePlayerID(), gameID);
                    });
            } else {
                currentPlayer.setPlayerGameState(EPlayerGameState.Lost);
                logger.info("Player {} lost the game: {}", currentPlayer.getUniquePlayerID(), gameID);
                
                // Set second player as winner
                game.getPlayers().stream()
                    .filter(player -> !player.getUniquePlayerID().equals(currentPlayer.getUniquePlayerID()))
                    .forEach(player -> {
                        player.setPlayerGameState(EPlayerGameState.Won);
                        logger.info("Player {} won the game: {}", player.getUniquePlayerID(), gameID);
                    });
            }
        }

        // Remove game from active games list
        //games.remove(gameID);
    }

}
