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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
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
        UniquePlayerIdentifier playerID = UniquePlayerIdentifier.of(playerReg.getPlayerUsername());
        if (games.get(gameID).containsPlayerWithID(playerID.getUniquePlayerID())) {
        	return playerID;
        }
        checkIfGameIsFull(gameID);
        
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
    
    public void startGameWithAIEasy(UniqueGameIdentifier gameID) {
        startGame(gameID);
        GameInfo game = games.get(gameID);
        PlayerState aiPlayer = getAIPlayer(game);
        
        if (aiPlayer != null) {
            FullMapNode aiFortNode = getAiFortNode(game.getFullMap(), gameID, aiPlayer);
            System.out.println("AI FORT NODE: " + aiFortNode.toString());
            HalfMapType aiHalf = determineHalfMap(game.getFullMap(), aiFortNode);
            aiPlayer.setAiHalf(aiHalf); // Assuming you add a setAiHalf method to PlayerState
        }
    }

    private HalfMapType determineHalfMap(FullMap fullMap, FullMapNode aiFortNode) {
        int maxX = fullMap.getMaxX();
        int maxY = fullMap.getMaxY();

        if (maxX == 19 && maxY == 4) {
            if (aiFortNode.getX() <= 9) {
                return HalfMapType.LEFT_HALF;
            } else {
                return HalfMapType.RIGHT_HALF;
            }
        } else if (maxX == 9 && maxY == 9) {
            if (aiFortNode.getY() <= 4) {
                return HalfMapType.UPPER_HALF;
            } else {
                return HalfMapType.LOWER_HALF;
            }
        }

        throw new IllegalStateException("Unexpected map dimensions.");
    }

    private FullMapNode getAiFortNode(FullMap fullMap, UniqueGameIdentifier gameID, PlayerState aiPlayer) {
        for (int x = 0; x <= fullMap.getMaxX(); x++) {
            for (int y = 0; y <= fullMap.getMaxY(); y++) {
                FullMapNode node = getFullMapNodeByXY(fullMap, x, y);
                GameInfo game = games.get(gameID);
                if (node.getFortState() == EFortState.MyFortPresent && node.getOwnedByPlayer() == game.getPlayerNumberByPlayerID(aiPlayer)) {
                    return node;
                }
            }
        }
        return null;
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
    
    public void makeAIEasyMove(UniqueGameIdentifier gameID) {
        GameInfo game = games.get(gameID);
        PlayerState aiPlayer = getAIPlayer(game);

        if (aiPlayer == null) {
            System.out.println("AI player not found");
            return;
        }

        if (!isGameFinished(aiPlayer)) {
            if (aiPlayer.getState() == EPlayerGameState.MustAct) {
            	if (aiPlayer.getCollectedTreasure()) {
            		System.out.println("\n\nTREASURE IS COLLECTED\n\n");
            	}
                EMove aiNextMove = aiPlayer.getCollectedTreasure() ? aiSearchForEnemyFort(game, aiPlayer) : aiSearchForTreasure(game.getFullMap(), aiPlayer, gameID);
            	System.out.println("AI is going to find next move");
            	System.out.println("Next move: " + aiNextMove.toString());
                processMove(gameID, new PlayerMove(aiPlayer.getPlayerUsername(), aiNextMove));
                System.out.println("AI Easy has made a move");
            } else {
                System.out.println("AI Easy needs to wait");
            }
        } else {
            System.out.println("AI Easy has won / lost");
        }
    }

    private PlayerState getAIPlayer(GameInfo game) {
        for (PlayerState player : game.getPlayers()) {
            if ("AI_Easy".equals(player.getPlayerUsername())) {
                return player;
            }
        }
        return null;
    }

    private boolean isGameFinished(PlayerState aiPlayer) {
        return aiPlayer.getState() == EPlayerGameState.Won || aiPlayer.getState() == EPlayerGameState.Lost;
    }

    private EMove aiSearchForTreasure(FullMap fullMap, PlayerState aiPlayer, UniqueGameIdentifier gameID) {
        AiMemory memory = aiPlayer.getAiMemory();

        if (!memory.plannedMoves.isEmpty()) {
            return memory.plannedMoves.poll();
        }

        FullMapNode aiCurrentNode = getCurrentNode(fullMap, aiPlayer, gameID);
        memory.visitedTiles.add(aiCurrentNode);
        System.out.println("aiCurrentNode was returned: " + aiCurrentNode.toString());
        FullMapNode targetNode = findNearestUnvisitedGrassTile(fullMap, aiCurrentNode, memory.visitedTiles, aiPlayer);
        
        if (targetNode == null) {
        	System.out.println("targetNode == null");
            return getRandomMove();
        }
        
        System.out.println("targetNode was returned: " + targetNode.toString());
        
        List<EMove> movesToTarget = determinePathToTarget(fullMap, aiCurrentNode, targetNode, aiPlayer);
        System.out.println("list of movesToTarget was initialized: " + movesToTarget.toString());
        memory.plannedMoves.addAll(movesToTarget);

        return memory.plannedMoves.poll();
    }
    
    private EMove aiSearchForEnemyFort(GameInfo game, PlayerState aiPlayer) {
        HalfMapType opponentHalfMapType = getOpponentHalfMap(aiPlayer.getAiHalf());
        AiMemory memory = aiPlayer.getAiMemory();

        if (!memory.plannedMoves.isEmpty()) {
            return memory.plannedMoves.poll();
        }

        FullMapNode aiCurrentNode = getCurrentNode(game.getFullMap(), aiPlayer, game.getGameID());
        memory.visitedTiles.add(aiCurrentNode);
        
        // Check if the AI is still in its own half.
        if (!isInHalfMap(aiCurrentNode.getX(), aiCurrentNode.getY(), opponentHalfMapType, game.getFullMap().getMaxX(), game.getFullMap().getMaxY())) {
            FullMapNode nearestNodeInOpponentHalf = findNearestGrassTileInOpponentHalf(game.getFullMap(), aiCurrentNode, opponentHalfMapType);
            System.out.println("NEAREST NODE IN OPONENTS HALF: " + nearestNodeInOpponentHalf.toString());
            
            // If a valid node in opponent's half is found, determine the path to it.
            if (nearestNodeInOpponentHalf != null) {
                List<EMove> movesToTarget = determinePathToTarget(game.getFullMap(), aiCurrentNode, nearestNodeInOpponentHalf, aiPlayer);
                System.out.println("MOVES TO TARGET: " + movesToTarget.toString());
                memory.plannedMoves.addAll(movesToTarget);
                return memory.plannedMoves.poll();
            }
        }

        // Continue the existing logic of searching for the enemy fort once the AI is in the opponent's half.
        FullMapNode targetNode = findNearestUnvisitedGrassTileInOpponentHalf(game.getFullMap(), aiCurrentNode, memory.visitedTiles, opponentHalfMapType);
        
        if (targetNode == null) {
            return getRandomMove();
        }
        
        List<EMove> movesToTarget = determinePathToTarget(game.getFullMap(), aiCurrentNode, targetNode, aiPlayer);
        memory.plannedMoves.addAll(movesToTarget);

        return memory.plannedMoves.poll();
    }
    
    private FullMapNode findNearestGrassTileInOpponentHalf(FullMap fullMap, FullMapNode start, HalfMapType opponentHalfMapType) {
        Queue<FullMapNode> queue = new LinkedList<>();
        Set<FullMapNode> seen = new HashSet<>();
        queue.add(start);

        while (!queue.isEmpty()) {
            FullMapNode current = queue.poll();
            
            // Get all valid neighbors regardless of which half they are in.
            for (FullMapNode neighbor : getValidNeighbors(fullMap, current, opponentHalfMapType, false)) {
                if (!seen.contains(neighbor)) {
                    seen.add(neighbor);
                    queue.add(neighbor);

                    // Check if the node is in the opponent's half and is a grass tile.
                    if (isInHalfMap(neighbor.getX(), neighbor.getY(), opponentHalfMapType, fullMap.getMaxX(), fullMap.getMaxY()) && 
                        neighbor.getTerrain() == ETerrain.Grass) {
                        return neighbor;
                    }
                }
            }
        }
        return null;
    }
    
    private FullMapNode findNearestUnvisitedGrassTileInOpponentHalf(FullMap fullMap, FullMapNode start, Set<FullMapNode> visited, HalfMapType opponentHalfMapType) {
        Queue<FullMapNode> queue = new LinkedList<>();
        Set<FullMapNode> seen = new HashSet<>(visited);
        queue.add(start);

        while (!queue.isEmpty()) {
            FullMapNode current = queue.poll();
            
            for (FullMapNode neighbor : getValidNeighbors(fullMap, current, opponentHalfMapType, true)) {
                if (!seen.contains(neighbor) && neighbor.getTerrain() == ETerrain.Grass) {
                    return neighbor;
                } else {
                    queue.add(neighbor);
                }
            }
        }
        return null;
    }

    private HalfMapType getOpponentHalfMap(HalfMapType aiHalfMapType) {
        switch (aiHalfMapType) {
            case UPPER_HALF:
                return HalfMapType.LOWER_HALF;
            case LOWER_HALF:
                return HalfMapType.UPPER_HALF;
            case LEFT_HALF:
                return HalfMapType.RIGHT_HALF;
            case RIGHT_HALF:
                return HalfMapType.LEFT_HALF;
        }
        throw new IllegalStateException("Unexpected AI half map type.");
    }

    private FullMapNode getCurrentNode(FullMap fullMap, PlayerState aiPlayer, UniqueGameIdentifier gameID) {
        for (FullMapNode node : fullMap) {
            if (node.getPlayerPositionState() == EPlayerPositionState.BothPlayerPosition || (node.getPlayerPositionState() == EPlayerPositionState.MyPlayerPosition) && node.getOwnedByPlayer() == games.get(gameID).getPlayerNumberByPlayerID(new UniquePlayerIdentifier(aiPlayer.getPlayerUsername()))) {
                return node;
            }
        }
        return null;
    }

    private FullMapNode findNearestUnvisitedGrassTile(FullMap fullMap, FullMapNode start, Set<FullMapNode> visited, PlayerState aiPlayer) {
        Queue<FullMapNode> queue = new LinkedList<>();
        Set<FullMapNode> seen = new HashSet<>(visited);
        queue.add(start);

        while (!queue.isEmpty()) {
            FullMapNode current = queue.poll();
            
            for (FullMapNode neighbor : getValidNeighbors(fullMap, current, aiPlayer.getAiHalf(), true)) {
                if (!seen.contains(neighbor) && neighbor.getTerrain() == ETerrain.Grass) { // test for ) && neighbor.getTerrain() == ETerrain.Grass
                	return neighbor;
                } else {
                	queue.add(neighbor);
                }
            }
        }
        return null;
    }

    private List<FullMapNode> getValidNeighbors(FullMap fullMap, FullMapNode node, HalfMapType halfMapType, boolean restrictToHalfMap) {
        List<FullMapNode> neighbors = new ArrayList<>();

        int[][] directions = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};

        for (int[] direction : directions) {
            int newX = node.getX() + direction[0];
            int newY = node.getY() + direction[1];

            if (isValidCoordinate(newX, newY, fullMap.getMaxX(), fullMap.getMaxY())) {
                if (!restrictToHalfMap || isInHalfMap(newX, newY, halfMapType, fullMap.getMaxX(), fullMap.getMaxY())) {
                    Optional<FullMapNode> neighborOpt = fullMap.get(newX, newY);
                    if (neighborOpt.isPresent()) {
                        FullMapNode neighbor = neighborOpt.get();
                        if (neighbor.getTerrain() != ETerrain.Water) {
                            neighbors.add(neighbor);
                        }
                    }
                }
            }
        }
        return neighbors;
    }

    private boolean isInHalfMap(int x, int y, HalfMapType halfMapType, int maxX, int maxY) {
        switch (halfMapType) {
            case UPPER_HALF:
                return y <= maxY / 2;
            case LOWER_HALF:
                return y > maxY / 2;
            case LEFT_HALF:
                return x <= maxX / 2;
            case RIGHT_HALF:
                return x > maxX / 2;
        }
        return false; // Default to not in any half (though this shouldn't be reached)
    }


    private boolean isValidCoordinate(int x, int y, int maxX, int maxY) {
        return x >= 0 && x <= maxX && y >= 0 && y <= maxY;
    }

    private List<EMove> determinePathToTarget(FullMap fullMap, FullMapNode start, FullMapNode target, PlayerState aiPlayer) {
        Map<FullMapNode, FullMapNode> cameFrom = new HashMap<>();
        Queue<FullMapNode> queue = new LinkedList<>();
        queue.add(start);

        while (!queue.isEmpty()) {
            FullMapNode current = queue.poll();
            for (FullMapNode neighbor : getValidNeighbors(fullMap, current, aiPlayer.getAiHalf(), false)) {
                if (!cameFrom.containsKey(neighbor)) {
                    cameFrom.put(neighbor, current);
                    queue.add(neighbor);
                }
            }
        }

        List<EMove> path = new LinkedList<>();
        FullMapNode current = target;
        while (current != null && !current.equals(start)) {
            FullMapNode prev = cameFrom.get(current);
            EMove move = getMoveFromNodes(prev, current);

            // Retrieve the cost for the field transition and add the move that many times
            FieldTransition transition = new FieldTransition(prev.getTerrain(), current.getTerrain());
            Integer cost = FIELD_TRANSITION_COST.getOrDefault(transition, 1);
            for (int i = 0; i < cost; i++) {
                path.add(move);
            }

            current = prev;
        }
        Collections.reverse(path);
        return path;
    }

    private EMove getMoveFromNodes(FullMapNode from, FullMapNode to) {
        if (from.getX() < to.getX()) return EMove.Right;
        if (from.getX() > to.getX()) return EMove.Left;
        if (from.getY() < to.getY()) return EMove.Down;
        return EMove.Up;
    }

    private EMove getRandomMove() {
        Random rand = new Random();
        EMove[] moves = EMove.values();
        return moves[rand.nextInt(moves.length)];
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
    
    public void processMove(UniqueGameIdentifier gameID, PlayerMove playerMove) throws IllegalArgumentException {
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
            endGame(gameID, "Player " + currentPlayer.getPlayerUsername() + " moved into the water", false);
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
        	if (currentNode.getOwnedByPlayer() != game.getPlayerNumberByPlayerID(currentPlayer) && (currentNode.getTreasureState() == ETreasureState.MyTreasureIsPresent || currentNode.getFortState() == EFortState.MyFortPresent)) {
        		currentNode.setPlayerPositionState(EPlayerPositionState.NoPlayerPresent);
        	}
            // remove player field occupation if it is not fort or treasure
            if (currentNode.getOwnedByPlayer() == game.getPlayerNumberByPlayerID(currentPlayer)) {
            	// Remove player from the current node
                currentNode.setPlayerPositionState(EPlayerPositionState.NoPlayerPresent);
                if (currentNode.getFortState() != EFortState.MyFortPresent && currentNode.getTreasureState() != ETreasureState.MyTreasureIsPresent) {
            		currentNode.setOwnedByPlayer(0); // TODO check BothPlayer
            	}
            }
            
            if (currentNode.getOwnedByPlayer() == 0 && currentNode.getPlayerPositionState() == EPlayerPositionState.BothPlayerPosition) {
            	currentNode.setPlayerPositionState(EPlayerPositionState.MyPlayerPosition);
            	if (game.getPlayerNumberByPlayerID(currentPlayer) == 1) {
            		currentNode.setOwnedByPlayer(2);
            	} else {
            		currentNode.setOwnedByPlayer(1);
            	}
            }

            // Place player on the new node
            FullMapNode newNode = getFullMapNodeByXY(fullMap, newX, newY);
            
            
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

            // NEW
            if (newNode.getPlayerPositionState() == EPlayerPositionState.MyPlayerPosition && newNode.getFortState() != EFortState.MyFortPresent && newNode.getTreasureState() != ETreasureState.MyTreasureIsPresent) {
            	newNode.setOwnedByPlayer(0);
            	newNode.setPlayerPositionState(EPlayerPositionState.BothPlayerPosition);
            } else if (newNode.getFortState() == EFortState.MyFortPresent && newNode.getOwnedByPlayer() != game.getPlayerNumberByPlayerID(currentPlayer)){
            	newNode.setPlayerPositionState(EPlayerPositionState.EnemyPlayerPosition);
            } else if (newNode.getTreasureState() == ETreasureState.MyTreasureIsPresent && newNode.getOwnedByPlayer() != game.getPlayerNumberByPlayerID(currentPlayer)) {
            	newNode.setPlayerPositionState(EPlayerPositionState.EnemyPlayerPosition); // TRY TO FIX enemy treasure is shown for my player, and for origin player it disappears
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
    
    private int getTransitionCost(ETerrain from, ETerrain to) {
        return FIELD_TRANSITION_COST.get(new FieldTransition(from, to));
    }

    private FullMapNode getCurrentPosition(FullMap fullMap, PlayerState player, GameInfo game) {
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
        game.incrementTurnCount();
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
