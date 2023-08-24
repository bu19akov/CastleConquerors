package main.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Random;

import messagesbase.UniquePlayerIdentifier;
import messagesbase.messagesfromclient.EMove;
import messagesbase.messagesfromclient.PlayerMove;
import messagesbase.messagesfromserver.EFortState;
import messagesbase.messagesfromserver.EPlayerGameState;
import messagesbase.messagesfromserver.EPlayerPositionState;
import messagesbase.messagesfromserver.ETerrain;
import messagesbase.messagesfromserver.ETreasureState;
import messagesbase.messagesfromserver.FullMap;
import messagesbase.messagesfromserver.FullMapNode;
import messagesbase.messagesfromserver.PlayerState;

public class PlayerAIHard extends PlayerState {
	private AiMemory aiMemory = null;
	private GameInfo game;
	
	public PlayerAIHard(String playerUsername, 
						EPlayerGameState state, 
						UniquePlayerIdentifier playerIdentifier, 
						boolean collectedTreasure) {
		super(playerUsername, state, playerIdentifier, collectedTreasure);
		if (playerUsername == "AI_Hard") {
			this.aiMemory = new AiMemory();
		}
	}
	
	public void setStartParameters(GameInfo game) {
		this.game = game;
        if (this.getState() == EPlayerGameState.MustAct) {
        	makeMove();
        }
	}
	
	public void makeMove() {
		if (!isGameFinished()) {
            if (this.getState() == EPlayerGameState.MustAct) {
                EMove aiNextMove = this.getCollectedTreasure() ? aiSearchForEnemyFort() : aiSearchForTreasure();
                GameService.processMove(game.getGameID(), new PlayerMove(this.getPlayerUsername(), aiNextMove));
            }
        }
	}
	
	private boolean isGameFinished() {
        return this.getState() == EPlayerGameState.Won || this.getState() == EPlayerGameState.Lost;
    }
	
	private EMove aiSearchForTreasure() {
        AiMemory memory = this.getAiMemory();

        if (!memory.plannedMoves.isEmpty()) {
            return memory.plannedMoves.poll();
        }

        FullMapNode aiCurrentNode = getCurrentNode();
        memory.visitedTiles.add(aiCurrentNode);
        FullMapNode targetNode = null;
        for (FullMapNode node : game.getFullMap()) {
        	if (node.getTreasureState() == ETreasureState.MyTreasureIsPresent &&
        			node.getOwnedByPlayer() == game.getPlayerNumberByPlayerID(new UniquePlayerIdentifier(this.getPlayerUsername()))) {
        		targetNode = node;
        	}
        }
        
        if (targetNode == null) {
            return getRandomMove();
        }
        
        
        List<EMove> movesToTarget = determinePathToTarget(aiCurrentNode, targetNode);
        memory.plannedMoves.addAll(movesToTarget);

        return memory.plannedMoves.poll();
    }
	
	private EMove aiSearchForEnemyFort() {
		AiMemory memory = this.getAiMemory();

        if (!memory.plannedMoves.isEmpty()) {
            return memory.plannedMoves.poll();
        }

        FullMapNode aiCurrentNode = getCurrentNode();
        memory.visitedTiles.add(aiCurrentNode);
        FullMapNode targetNode = null;
        for (FullMapNode node : game.getFullMap()) {
        	if (node.getFortState() == EFortState.MyFortPresent &&
        			node.getOwnedByPlayer() != game.getPlayerNumberByPlayerID(new UniquePlayerIdentifier(this.getPlayerUsername()))) {
        		targetNode = node;
        	}
        }
        
        if (targetNode == null) {
            return getRandomMove();
        }
        
        
        List<EMove> movesToTarget = determinePathToTarget(aiCurrentNode, targetNode);
        memory.plannedMoves.addAll(movesToTarget);

        return memory.plannedMoves.poll();
    }
	
	private List<EMove> determinePathToTarget(FullMapNode start, FullMapNode target) {
        Map<FullMapNode, FullMapNode> cameFrom = new HashMap<>();
        Queue<FullMapNode> queue = new LinkedList<>();
        queue.add(start);

        while (!queue.isEmpty()) {
            FullMapNode current = queue.poll();
            for (FullMapNode neighbor : getValidNeighbors(current)) {
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
	
	private List<FullMapNode> getValidNeighbors(FullMapNode node) {
        List<FullMapNode> neighbors = new ArrayList<>();
        FullMap fullMap = game.getFullMap();

        int[][] directions = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};

        for (int[] direction : directions) {
            int newX = node.getX() + direction[0];
            int newY = node.getY() + direction[1];

            if (isValidCoordinate(newX, newY)) {
                Optional<FullMapNode> neighborOpt = fullMap.get(newX, newY);
                if (neighborOpt.isPresent()) {
                    FullMapNode neighbor = neighborOpt.get();
                    if (neighbor.getTerrain() != ETerrain.Water) {
                        neighbors.add(neighbor);
                    }
                }
            }
        }
        return neighbors;
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
	
	private boolean isValidCoordinate(int x, int y) {
        return x >= 0 && x <= game.getFullMap().getMaxX() && y >= 0 && y <= game.getFullMap().getMaxY();
    }

	public AiMemory getAiMemory() {
		return aiMemory;
	}
}