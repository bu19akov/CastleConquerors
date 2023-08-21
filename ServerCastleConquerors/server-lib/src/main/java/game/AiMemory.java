package game;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import messagesbase.messagesfromclient.EMove;
import messagesbase.messagesfromserver.FullMapNode;

public class AiMemory {
    Set<FullMapNode> visitedTiles = new HashSet<>();
    Queue<EMove> plannedMoves = new LinkedList<>();
}

