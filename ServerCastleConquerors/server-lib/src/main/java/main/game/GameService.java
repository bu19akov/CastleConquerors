package main.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.UUID;

@Service
public class GameService {
    private static final int MAX_GAMES = 99; 
    private static final int MAX_TURNS = 420;
    private static final int MAX_TURN_TIME = 10_000; // 10 seconds
    private static final Logger logger = LoggerFactory.getLogger(GameService.class);

    protected static final ConcurrentMap<UniqueGameIdentifier, GameInfo> games = new ConcurrentHashMap<>();
    public static final Map<FieldTransition, Integer> FIELD_TRANSITION_COST;
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

    public UniquePlayerIdentifier registerPlayerToGame(UniqueGameIdentifier gameID, PlayerRegistration playerReg) throws GameNotFoundException {
        checkIfGameExist(gameID, "register a player");
        UniquePlayerIdentifier playerID = UniquePlayerIdentifier.of(playerReg.getPlayerUsername());
        if (games.get(gameID).containsPlayerWithID(playerID.getUniquePlayerID())) {
        	return playerID;
        }
        checkIfGameIsFull(gameID);
        PlayerState newPlayer;
        if (playerID.getUniquePlayerID() == "AI_Easy") {
        	newPlayer = new PlayerAIEasy(
                    playerReg.getPlayerUsername(), 
                    EPlayerGameState.MustWait,
                    playerID,
                    false
                );
        } else if (playerID.getUniquePlayerID() == "AI_Hard") {
        	newPlayer = new PlayerAIHard(
                    playerReg.getPlayerUsername(), 
                    EPlayerGameState.MustWait,
                    playerID,
                    false
                );
        }
        else {
        	newPlayer = new PlayerState(
	            playerReg.getPlayerUsername(), 
	            EPlayerGameState.MustWait,
	            playerID,
	            false
	        );
        }

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

    public static void checkIfGameExist(UniqueGameIdentifier gameID, String message) throws GameNotFoundException{
        if (!games.containsKey(gameID)) {
            logger.error("Attempted to {} to a non-existent game: {}", message, gameID.getUniqueGameID());
            throw new GameNotFoundException(gameID.getUniqueGameID());
        }
    }

    public static ConcurrentMap<UniqueGameIdentifier, GameInfo> getGames() {
        return games;
    }

    public void startGame(UniqueGameIdentifier gameID) {
        GameInfo game = games.get(gameID);
        Iterator<PlayerState> playerIterator = game.getPlayers().iterator();

        setInitialPlayersForGame(game, playerIterator);
        if (game.containsPlayerWithID("AI_Easy")) {
        	PlayerAIEasy aiPlayer = (PlayerAIEasy) getAIEasyPlayer(game);
            aiPlayer.setStartParameters(game);
        } else if (game.containsPlayerWithID("AI_Hard")) {
        	PlayerAIHard aiPlayer = (PlayerAIHard) getAIHardPlayer(game);
            aiPlayer.setStartParameters(game);
        }
        games.get(gameID).setTurnStartTime(System.currentTimeMillis());
    }
    
    public boolean doesGameContainsAIEasy(UniqueGameIdentifier gameID) {
    	GameInfo game = games.get(gameID);
    	PlayerState aiPlayer = new PlayerState();
    	for (PlayerState player : game.getPlayers()) {
    		if (player.getPlayerUsername() == "AI_Easy") {
    			aiPlayer = player;
    		}
    	}
    	if (aiPlayer.getPlayerUsername() == "AI_Easy") {
    		return true;
    	}
    	return false;
    }
    
    public boolean doesGameContainsAIHard(UniqueGameIdentifier gameID) {
    	GameInfo game = games.get(gameID);
    	PlayerState aiPlayer = new PlayerState();
    	for (PlayerState player : game.getPlayers()) {
    		if (player.getPlayerUsername() == "AI_Hard") {
    			aiPlayer = player;
    		}
    	}
    	if (aiPlayer.getPlayerUsername() == "AI_Hard") {
    		return true;
    	}
    	return false;
    }
    
    public void makeAIEasyMove(UniqueGameIdentifier gameID) {
        GameInfo game = games.get(gameID);
        PlayerAIEasy aiPlayer = (PlayerAIEasy) getAIEasyPlayer(game);

        if (aiPlayer == null) {
            System.out.println("AI player not found");
            return;
        }
        
        aiPlayer.makeMove();
    }
    
    public void makeAIHardMove(UniqueGameIdentifier gameID) {
        GameInfo game = games.get(gameID);
        PlayerAIHard aiPlayer = (PlayerAIHard) getAIHardPlayer(game);

        if (aiPlayer == null) {
            System.out.println("AI player not found");
            return;
        }
        
        aiPlayer.makeMove();
    }

    private PlayerState getAIEasyPlayer(GameInfo game) {
        for (PlayerState player : game.getPlayers()) {
            if ("AI_Easy".equals(player.getPlayerUsername())) {
                return player;
            }
        }
        return null;
    }
    
    private PlayerState getAIHardPlayer(GameInfo game) {
        for (PlayerState player : game.getPlayers()) {
            if ("AI_Hard".equals(player.getPlayerUsername())) {
                return player;
            }
        }
        return null;
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
    
    public static void processMove(UniqueGameIdentifier gameID, PlayerMove playerMove) throws IllegalArgumentException {
    	checkIfGameExist(gameID, "send move");
        GameInfo game = games.get(gameID);
        
        if (game.getCurrentPlayer() == null) {
            throw new IllegalArgumentException("Wait for the second player to connect!");
        }
        
        // Validate if it's the player's turn
        if(!game.getCurrentPlayer().getUniquePlayerID().equals(playerMove.getUniquePlayerID())) {
            logger.error("It's not the turn of player {}", playerMove.getUniquePlayerID());
            throw new IllegalArgumentException("Not your turn!");
        }
        
        FullMap fullMap = game.getFullMap();
        PlayerState currentPlayer = game.getPlayerWithID(playerMove.getUniquePlayerID()); // game.getCurrentPlayer();
        
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
            logger.error("Player {} attempted to move outside the map boundaries!", playerMove.getUniquePlayerID()); // TODO throw exception
            endGame(gameID, "moved outside of the map boundaries!", false);
            return;
        }
        
        Optional<FullMapNode> nextNodeOpt = fullMap.get(newX, newY);
        
        if (!nextNodeOpt.isPresent()) {
        	throw new IllegalArgumentException("Next node is not present!");
        }
        
        FullMapNode nextNode = nextNodeOpt.get();
        
        if (nextNode.getTerrain() == ETerrain.Water) {
            endGame(gameID, "moved into the water!", false);
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
            // remove player field occupation if it is not fort or treasure
            if (currentNode.getOwnedByPlayer() == game.getPlayerNumberByPlayerID(currentPlayer)) {
            	if (currentNode.getPlayerPositionState() == EPlayerPositionState.BothPlayerPosition) {
            		currentNode.setPlayerPositionState(EPlayerPositionState.EnemyPlayerPosition);
            	} else {
            		currentNode.setPlayerPositionState(EPlayerPositionState.NoPlayerPresent);
            	}
                if (currentNode.getFortState() != EFortState.MyFortPresent && currentNode.getTreasureState() != ETreasureState.MyTreasureIsPresent) {
            		currentNode.setOwnedByPlayer(0); // TODO check BothPlayer
            	}
            }
            
            else if (currentNode.getOwnedByPlayer() == 0 && currentNode.getPlayerPositionState() == EPlayerPositionState.BothPlayerPosition) {
            	currentNode.setPlayerPositionState(EPlayerPositionState.MyPlayerPosition);
            	if (game.getPlayerNumberByPlayerID(currentPlayer) == 1) {
            		currentNode.setOwnedByPlayer(2);
            	} else {
            		currentNode.setOwnedByPlayer(1);
            	}
            } else if (currentNode.getOwnedByPlayer() != game.getPlayerNumberByPlayerID(currentPlayer) && (currentNode.getTreasureState() == ETreasureState.MyTreasureIsPresent || currentNode.getFortState() == EFortState.MyFortPresent)) {
            	if (currentNode.getPlayerPositionState() == EPlayerPositionState.BothPlayerPosition) {
            		currentNode.setPlayerPositionState(EPlayerPositionState.MyPlayerPosition);
            	} else {
            		currentNode.setPlayerPositionState(EPlayerPositionState.NoPlayerPresent);
            	}
        	}

            // Place player on the new node
            Optional<FullMapNode> newNodeOpt = fullMap.get(newX, newY);
            
            if (!newNodeOpt.isPresent()) {
            	throw new IllegalArgumentException("New node is not present!");
            }
            
            FullMapNode newNode = newNodeOpt.get();
            
            
            // Reveal neighbor nodes (radius 1), if your new node is mountain
            if (newNode.getTerrain() == ETerrain.Mountain) {
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

                            Optional<FullMapNode> neighborNodeOpt = fullMap.get(neighborX, neighborY);
                            
                            if (!neighborNodeOpt.isPresent()) {
                            	throw new IllegalArgumentException("Neighbor node is not present!");
                            }
                            
                            FullMapNode neighborNode = neighborNodeOpt.get();
                            
                            if (neighborNode.getOwnedByPlayer() == 0 || 
                                neighborNode.getOwnedByPlayer() == game.getPlayerNumberByPlayerID(currentPlayer)) {
                                // neighborNode.setOwnedByPlayer(game.getPlayerNumberByPlayerID(currentPlayer));
                                if (neighborNode.getTreasureState() == ETreasureState.MyTreasureIsPresent) {
                                	currentPlayer.setRevealedTreasureToTrue();
                                }
                            }
                            if (currentPlayer.getCollectedTreasure() && neighborNode.getFortState() == EFortState.MyFortPresent && neighborNode.getOwnedByPlayer() != game.getPlayerNumberByPlayerID(currentPlayer)) {
                            	currentPlayer.setRevealedEnemyFortToTrue();
                            }
                        }
                    }
                }
            }

            // NEW
            if (newNode.getPlayerPositionState() == EPlayerPositionState.MyPlayerPosition && newNode.getFortState() != EFortState.MyFortPresent && newNode.getTreasureState() != ETreasureState.MyTreasureIsPresent) {
            	newNode.setOwnedByPlayer(0);
            	newNode.setPlayerPositionState(EPlayerPositionState.BothPlayerPosition);
            } else if (newNode.getFortState() == EFortState.MyFortPresent || newNode.getTreasureState() == ETreasureState.MyTreasureIsPresent){
            	if (newNode.getOwnedByPlayer() != game.getPlayerNumberByPlayerID(currentPlayer)) {
	            	if (newNode.getPlayerPositionState() == EPlayerPositionState.MyPlayerPosition) {
	            		newNode.setPlayerPositionState(EPlayerPositionState.BothPlayerPosition);
	            	} else {
	            		newNode.setPlayerPositionState(EPlayerPositionState.EnemyPlayerPosition); // TRY TO FIX enemy treasure is shown for my player, and for origin player it disappears
	            	}
            	} else if (newNode.getOwnedByPlayer() == game.getPlayerNumberByPlayerID(currentPlayer)) {
            		if (newNode.getPlayerPositionState() == EPlayerPositionState.EnemyPlayerPosition) {
	            		newNode.setPlayerPositionState(EPlayerPositionState.BothPlayerPosition);
	            	} else {
	            		newNode.setPlayerPositionState(EPlayerPositionState.MyPlayerPosition);
	            	}
            	}
            } else {
            	newNode.setOwnedByPlayer(game.getPlayerNumberByPlayerID(currentPlayer));
            	newNode.setPlayerPositionState(EPlayerPositionState.MyPlayerPosition);
            }
            
            if (newNode.getTreasureState() == ETreasureState.MyTreasureIsPresent && 
                    newNode.getOwnedByPlayer() == game.getPlayerNumberByPlayerID(currentPlayer)) {
                        currentPlayer.setCollectedTreasureToTrue();
                }
            
            if (newNode.getFortState() == EFortState.MyFortPresent && currentPlayer.getCollectedTreasure() && newNode.getOwnedByPlayer() != game.getPlayerNumberByPlayerID(currentPlayer)) {
                // Player wins!
                endGame(gameID, "Player " + currentPlayer.getUniquePlayerID() + " has captured the enemy fort with the treasure!", true);
                return;
            }
            
            playerMoveCounter.remove(playerMove.getMove());
        } else {
            playerMoveCounter.put(playerMove.getMove(), remainingMoves);
        }

        // Update the game state
        nextTurn(gameID);
    }
    
    private static int getTransitionCost(ETerrain from, ETerrain to) {
        return FIELD_TRANSITION_COST.get(new FieldTransition(from, to));
    }

    private static FullMapNode getCurrentPosition(FullMap fullMap, PlayerState player, GameInfo game) {
        for (FullMapNode node : fullMap) {
            if ((node.getPlayerPositionState() == EPlayerPositionState.MyPlayerPosition && 
            	node.getOwnedByPlayer() == game.getPlayerNumberByPlayerID(player)) || node.getPlayerPositionState() == EPlayerPositionState.BothPlayerPosition) { // TRY TO FIX NO PLAYER POSITION FOUND
                return node;
            }
            if (node.getPlayerPositionState() == EPlayerPositionState.EnemyPlayerPosition && (node.getTreasureState() == ETreasureState.MyTreasureIsPresent || node.getFortState() == EFortState.MyFortPresent) && node.getOwnedByPlayer() != game.getPlayerNumberByPlayerID(player)) {
            	return node; // TRY TO FIX enemy treasure is shown for my player, and for origin player it disappears
            }
        }
        throw new IllegalStateException("Player position not found!");
    }

    @Scheduled(fixedRate = 1000)
    public void checkTurnTime() {
        games.values().forEach(game -> {
            // Check if turn time has been exceeded
            if (game.getTurnStartTime() != 0 && System.currentTimeMillis() - game.getTurnStartTime() > MAX_TURN_TIME) {
                logger.info("Turn time exceeded for game with ID: {}", game.getGameID().getUniqueGameID());
                endGame(game.getGameID(), "exceeded your maximum turn time!", false);
            }
        });
    }
	
    public static void nextTurn(UniqueGameIdentifier gameID) {
    	GameInfo game = games.get(gameID);
        game.getCurrentPlayer().setPlayerGameState(EPlayerGameState.MustWait);
        game.setStateID(UUID.randomUUID().toString());

        for (PlayerState player : game.getPlayers()) {
            if (player != game.getCurrentPlayer()) {
                game.setCurrentPlayer(player);
                game.getCurrentPlayer().setPlayerGameState(EPlayerGameState.MustAct);
                break;
            }
        }
        // Check if maximum turns reached
        if (games.get(gameID).getTurnCount() >= MAX_TURNS) {
            endGame(gameID, "Maximum turns reached", false);
        } else {
        	games.get(gameID).setTurnStartTime(System.currentTimeMillis());
        }
        game.incrementTurnCount();
    }
    
    private static void endGame(UniqueGameIdentifier gameID, String reason, boolean currentPlayerWins) throws IllegalArgumentException{
        logger.info("Game {} was ended! Reason: {}", gameID.getUniqueGameID(), reason);

        GameInfo game = games.get(gameID);
        PlayerState currentPlayer = game.getCurrentPlayer();
        
        if (currentPlayer != null) {
            if (currentPlayerWins) {
                currentPlayer.setPlayerGameState(EPlayerGameState.Won);
                logger.info("Player {} won the game: {}", currentPlayer.getUniquePlayerID(), gameID.getUniqueGameID());
                
                // Set second player as loser
                game.getPlayers().stream()
                    .filter(player -> !player.getUniquePlayerID().equals(currentPlayer.getUniquePlayerID()))
                    .forEach(player -> {
                        player.setPlayerGameState(EPlayerGameState.Lost);
                        logger.info("Player {} lost the game: {}", player.getUniquePlayerID(), gameID.getUniqueGameID());
                    });
            } else {
                currentPlayer.setPlayerGameState(EPlayerGameState.Lost);
                logger.info("Player {} lost the game: {}", currentPlayer.getUniquePlayerID(), gameID.getUniqueGameID());
                
                // Set second player as winner
                game.getPlayers().stream()
                    .filter(player -> !player.getUniquePlayerID().equals(currentPlayer.getUniquePlayerID()))
                    .forEach(player -> {
                        player.setPlayerGameState(EPlayerGameState.Won);
                        logger.info("Player {} won the game: {}", player.getUniquePlayerID(), gameID.getUniqueGameID());
                    });
                
                if (reason == "exceeded your maximum turn time!") {
                	currentPlayer.setTurnTimeExceededToTrue();
                } else {
                	throw new IllegalArgumentException("You " + reason);
                }
            }
        }

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(() -> {
            games.remove(gameID);
            logger.info("{} was deleted from the game list", gameID.getUniqueGameID());
            scheduler.shutdown(); // shut down the scheduler to free resources
        }, 2, TimeUnit.SECONDS);
    }

}
