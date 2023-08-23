package game;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import messagesbase.UniqueGameIdentifier;
import messagesbase.UniquePlayerIdentifier;
import messagesbase.messagesfromclient.EMove;
import messagesbase.messagesfromclient.PlayerMove;
import messagesbase.messagesfromserver.EFortState;
import messagesbase.messagesfromserver.EPlayerGameState;
import messagesbase.messagesfromserver.EPlayerPositionState;
import messagesbase.messagesfromserver.ETerrain;
import messagesbase.messagesfromserver.FullMap;
import messagesbase.messagesfromserver.FullMapNode;
import messagesbase.messagesfromserver.PlayerState;

public class PlayerAIEasy extends PlayerState {
	private AiMemory aiMemory = null;
	private HalfMapType aiHalf;
	private GameInfo game;
	
	public PlayerAIEasy(String playerUsername, 
						EPlayerGameState state, 
						UniquePlayerIdentifier playerIdentifier, 
						boolean collectedTreasure) {
		super(playerUsername, state, playerIdentifier, collectedTreasure);
		if (playerUsername == "AI_Easy") {
			this.aiMemory = new AiMemory();
		}
	}
	
	public void setStartParameters(GameInfo game) {
		this.game = game;
		FullMapNode aiFortNode = getAiFortNode(game.getFullMap(), game.getGameID());
        System.out.println("AI FORT NODE: " + aiFortNode.toString());
        HalfMapType aiHalf = determineHalfMap(game.getFullMap(), aiFortNode);
        setAiHalf(aiHalf);
        if (this.getState() == EPlayerGameState.MustAct) {
        	makeMove();
        }
	}
	
	private FullMapNode getAiFortNode(FullMap fullMap, UniqueGameIdentifier gameID) {
        for (int x = 0; x <= fullMap.getMaxX(); x++) {
            for (int y = 0; y <= fullMap.getMaxY(); y++) {
                Optional<FullMapNode> nodeOpt = fullMap.get(x, y);
                if (nodeOpt.isPresent()) {
                	FullMapNode node = nodeOpt.get();
	                if (node.getFortState() == EFortState.MyFortPresent && node.getOwnedByPlayer() == game.getPlayerNumberByPlayerID(new UniquePlayerIdentifier(this.getUniquePlayerID()))) {
	                    return node;
	                }
                }
            }
        }
        return null;
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
	
	public void makeMove() {
		if (!isGameFinished()) {
            if (this.getState() == EPlayerGameState.MustAct) {
            	if (this.getCollectedTreasure()) {
            		System.out.println("\n\nTREASURE IS COLLECTED\n\n");
            	}
                EMove aiNextMove = this.getCollectedTreasure() ? aiSearchForEnemyFort() : aiSearchForTreasure();
                GameService.processMove(game.getGameID(), new PlayerMove(this.getPlayerUsername(), aiNextMove));
            }
        }
	}
	
	private EMove aiSearchForTreasure() {
        AiMemory memory = this.getAiMemory();

        if (!memory.plannedMoves.isEmpty()) {
            return memory.plannedMoves.poll();
        }

        FullMapNode aiCurrentNode = getCurrentNode();
        memory.visitedTiles.add(aiCurrentNode);
        System.out.println("aiCurrentNode was returned: " + aiCurrentNode.toString());
        FullMapNode targetNode = findNearestUnvisitedGrassTile(aiCurrentNode, memory.visitedTiles);
        
        if (targetNode == null) {
        	System.out.println("targetNode == null");
            return getRandomMove();
        }
        
        System.out.println("targetNode was returned: " + targetNode.toString());
        
        List<EMove> movesToTarget = determinePathToTarget(aiCurrentNode, targetNode);
        System.out.println("list of movesToTarget was initialized: " + movesToTarget.toString());
        memory.plannedMoves.addAll(movesToTarget);

        return memory.plannedMoves.poll();
    }
	
	private EMove aiSearchForEnemyFort() {
        HalfMapType opponentHalfMapType = getOpponentHalfMap(this.getAiHalf());
        AiMemory memory = this.getAiMemory();

        if (!memory.plannedMoves.isEmpty()) {
            return memory.plannedMoves.poll();
        }

        FullMapNode aiCurrentNode = getCurrentNode();
        memory.visitedTiles.add(aiCurrentNode);
        
        // Check if the AI is still in its own half.
        if (!isInHalfMap(aiCurrentNode.getX(), aiCurrentNode.getY(), opponentHalfMapType)) {
            FullMapNode nearestNodeInOpponentHalf = findNearestGrassTileInOpponentHalf(aiCurrentNode, opponentHalfMapType);
            System.out.println("NEAREST NODE IN OPONENTS HALF: " + nearestNodeInOpponentHalf.toString());
            
            // If a valid node in opponent's half is found, determine the path to it.
            if (nearestNodeInOpponentHalf != null) {
                List<EMove> movesToTarget = determinePathToTarget(aiCurrentNode, nearestNodeInOpponentHalf);
                System.out.println("MOVES TO TARGET: " + movesToTarget.toString());
                memory.plannedMoves.addAll(movesToTarget);
                return memory.plannedMoves.poll();
            }
        }

        // Continue the existing logic of searching for the enemy fort once the AI is in the opponent's half.
        FullMapNode targetNode = findNearestUnvisitedGrassTileInOpponentHalf(aiCurrentNode, memory.visitedTiles, opponentHalfMapType);
        
        if (targetNode == null) {
            return getRandomMove();
        }
        
        List<EMove> movesToTarget = determinePathToTarget(aiCurrentNode, targetNode);
        memory.plannedMoves.addAll(movesToTarget);

        return memory.plannedMoves.poll();
    }
	
	private FullMapNode getCurrentNode() {
        for (FullMapNode node : game.getFullMap()) {
            if ((node.getPlayerPositionState() == EPlayerPositionState.EnemyPlayerPosition && node.getOwnedByPlayer() !=  game.getPlayerNumberByPlayerID(new UniquePlayerIdentifier(this.getPlayerUsername()))) || 
            		node.getPlayerPositionState() == EPlayerPositionState.BothPlayerPosition || 
            		(node.getPlayerPositionState() == EPlayerPositionState.MyPlayerPosition && node.getOwnedByPlayer() == game.getPlayerNumberByPlayerID(new UniquePlayerIdentifier(this.getPlayerUsername())))) {
                return node;
            }
        }
        return null;
    }
	
	private FullMapNode findNearestUnvisitedGrassTile(FullMapNode start, Set<FullMapNode> visited) {
        Queue<FullMapNode> queue = new LinkedList<>();
        Set<FullMapNode> seen = new HashSet<>(visited);
        queue.add(start);

        while (!queue.isEmpty()) {
            FullMapNode current = queue.poll();
            
            for (FullMapNode neighbor : getValidNeighbors(current, this.getAiHalf(), true)) {
                if (!seen.contains(neighbor) && neighbor.getTerrain() == ETerrain.Grass) { // test for ) && neighbor.getTerrain() == ETerrain.Grass
                	return neighbor;
                } else {
                	queue.add(neighbor);
                }
            }
        }
        return null;
    }
	
	private EMove getRandomMove() {
        Random rand = new Random();
        EMove[] moves = EMove.values();
        return moves[rand.nextInt(moves.length)];
    }
	
	private List<EMove> determinePathToTarget(FullMapNode start, FullMapNode target) {
        Map<FullMapNode, FullMapNode> cameFrom = new HashMap<>();
        Queue<FullMapNode> queue = new LinkedList<>();
        queue.add(start);

        while (!queue.isEmpty()) {
            FullMapNode current = queue.poll();
            for (FullMapNode neighbor : getValidNeighbors(current, this.getAiHalf(), false)) {
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
            Integer cost = GameService.FIELD_TRANSITION_COST.getOrDefault(transition, 1);
            for (int i = 0; i < cost; i++) {
                path.add(move);
            }

            current = prev;
        }
        Collections.reverse(path);
        return path;
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
	
	private boolean isInHalfMap(int x, int y, HalfMapType halfMapType) {
		int maxX = game.getFullMap().getMaxX();
		int maxY = game.getFullMap().getMaxY();
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
	
	private FullMapNode findNearestGrassTileInOpponentHalf(FullMapNode start, HalfMapType opponentHalfMapType) {
        Queue<FullMapNode> queue = new LinkedList<>();
        Set<FullMapNode> seen = new HashSet<>();
        queue.add(start);

        while (!queue.isEmpty()) {
            FullMapNode current = queue.poll();
            
            // Get all valid neighbors regardless of which half they are in.
            for (FullMapNode neighbor : getValidNeighbors(current, opponentHalfMapType, false)) {
                if (!seen.contains(neighbor)) {
                    seen.add(neighbor);
                    queue.add(neighbor);

                    // Check if the node is in the opponent's half and is a grass tile.
                    if (isInHalfMap(neighbor.getX(), neighbor.getY(), opponentHalfMapType) && 
                        neighbor.getTerrain() == ETerrain.Grass) {
                        return neighbor;
                    }
                }
            }
        }
        return null;
    }
	
	private FullMapNode findNearestUnvisitedGrassTileInOpponentHalf(FullMapNode start, Set<FullMapNode> visited, HalfMapType opponentHalfMapType) {
        Queue<FullMapNode> queue = new LinkedList<>();
        Set<FullMapNode> seen = new HashSet<>(visited);
        queue.add(start);

        while (!queue.isEmpty()) {
            FullMapNode current = queue.poll();
            
            for (FullMapNode neighbor : getValidNeighbors(current, opponentHalfMapType, true)) {
                if (!seen.contains(neighbor) && neighbor.getTerrain() == ETerrain.Grass) {
                    return neighbor;
                } else {
                    queue.add(neighbor);
                }
            }
        }
        return null;
    }
	
	private List<FullMapNode> getValidNeighbors(FullMapNode node, HalfMapType halfMapType, boolean restrictToHalfMap) {
        List<FullMapNode> neighbors = new ArrayList<>();
        FullMap fullMap = game.getFullMap();

        int[][] directions = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};

        for (int[] direction : directions) {
            int newX = node.getX() + direction[0];
            int newY = node.getY() + direction[1];

            if (isValidCoordinate(newX, newY)) {
                if (!restrictToHalfMap || isInHalfMap(newX, newY, halfMapType)) {
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
	
	private EMove getMoveFromNodes(FullMapNode from, FullMapNode to) {
        if (from.getX() < to.getX()) return EMove.Right;
        if (from.getX() > to.getX()) return EMove.Left;
        if (from.getY() < to.getY()) return EMove.Down;
        return EMove.Up;
    }
	
	private boolean isValidCoordinate(int x, int y) {
        return x >= 0 && x <= game.getFullMap().getMaxX() && y >= 0 && y <= game.getFullMap().getMaxY();
    }
	
	private boolean isGameFinished() {
        return this.getState() == EPlayerGameState.Won || this.getState() == EPlayerGameState.Lost;
    }
	
	public HalfMapType getAiHalf() {
        return aiHalf;
    }

    public void setAiHalf(HalfMapType aiHalf) {
        this.aiHalf = aiHalf;
    }
	
	public AiMemory getAiMemory() {
		return aiMemory;
	}

}
