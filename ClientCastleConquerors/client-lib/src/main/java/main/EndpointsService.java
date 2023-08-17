package main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import database.DatabaseRepository;

@Service
public class EndpointsService {

    private static final Logger logger = LoggerFactory.getLogger(EndpointsService.class);

    public static boolean verifyLogin(String username, String password) {
        return DatabaseRepository.verifyLogin(username, password);
    }

//    public void createBankAccount(String username, String password, double initialBalance) {
//        if (DatabaseRepository.findAccountByUsername(username) != null) {
//            throw new IllegalArgumentException("Username already exists");
//        }
//        BankAccount account = new BankAccount(username, initialBalance);
//        logger.info("A new bank account was created with username {}. Initial balance: {}", username, initialBalance);
//        DatabaseRepository.saveAccount(account, password);
//    }
}
