package main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import database.DatabaseRepository;
import database.Player;

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
}
