package main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import database.DatabaseRepository;
import database.Player;
import messagesbase.messagesfromserver.FullMap;
import messagesbase.messagesfromserver.FullMapNode;

@Service
public class EndpointsService {

    private static final Logger logger = LoggerFactory.getLogger(EndpointsService.class);

    public boolean verifyLogin(String username, String password) {
        return DatabaseRepository.verifyLogin(username, password);
    }

    public void createPlayerAccount(String username, String password) {
        if (DatabaseRepository.findAccountByUsername(username) != null) {
            throw new IllegalArgumentException("Username already exists");
        }
        Player player = new Player(username);
        logger.info("A new player account was created with username {}", username);
        DatabaseRepository.createPlayerAccount(player, password);
    }
    
    public FullMapNode[][] getOrderedArray(FullMap map) {
        FullMapNode[][] mapNodes = new FullMapNode[map.getMaxY() + 1][map.getMaxX() + 1];
                for (FullMapNode node : map.getMapNodes()) {
                    mapNodes[node.getY()][node.getX()] = node;
                }

        return mapNodes;
    }
}
