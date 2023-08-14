package game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
}
