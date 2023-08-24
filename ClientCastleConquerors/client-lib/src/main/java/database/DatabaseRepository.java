package database;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;

import statistic.GameStatistics;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mindrot.jbcrypt.BCrypt;

public class DatabaseRepository {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseRepository.class);

    private static final MongoClient mongoClient;
    private static final MongoDatabase database;
    private static final MongoCollection<Document> credentialsCollection;

    static {
    	String connectionString = "mongodb+srv://vovabulgakov00:LAVWAXOSJrqB9GEI@castleconquerorscluster.varxwoo.mongodb.net/?retryWrites=true&w=majority";
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .build();

        mongoClient = MongoClients.create(settings);
        database = mongoClient.getDatabase("CastleConquerorsData");
        credentialsCollection = database.getCollection("UserCredentials");
    }

    public static boolean verifyLogin(String playerUsername, String password) {
        Document query = new Document("playerUsername", playerUsername);
        Document document = credentialsCollection.find(query).first();

        if (document != null) {
            String storedHashedPassword = document.getString("password");
            
            // Check that an unencrypted password matches one that has been hashed
            if (BCrypt.checkpw(password, storedHashedPassword)) {
                return true;
            }
        }
        return false;
    }

    public static Player findAccountByUsername(String playerUsername) {
        Document query = new Document("playerUsername", playerUsername);
        Document document = credentialsCollection.find(query).first();
        if (document != null) {
            String usernameFound = document.getString("playerUsername");
            return new Player(usernameFound);
        }
        return null;
    }

    public static void createPlayerAccount(Player player, String password, String email) {
        // Hash the password
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        Document document = new Document("playerUsername", player.getUsername())
        		.append("email", email)
        		.append("password", hashedPassword)
        		.append("wonGames", 0)
        		.append("lostGames", 0);
        try {
        	credentialsCollection.insertOne(document);
            logger.info("Player's account with username {} created successfully", player.getUsername());
        } catch (MongoException e) {
            logger.error("Failed to create new Player Account with username {}", player.getUsername(), e);
        }
    }
    
    public static void updatePlayerGameStatistics(String playerUsername, String outcome) {
        Document query = new Document("playerUsername", playerUsername);
        
        Document updateDocument;
        if ("Won".equalsIgnoreCase(outcome)) {
            updateDocument = new Document("$inc", new Document("wonGames", 1));
        } else if ("Lost".equalsIgnoreCase(outcome)) {
            updateDocument = new Document("$inc", new Document("lostGames", 1));
        } else {
            logger.error("Invalid game outcome provided: {}", outcome);
            return;
        }
        
        try {
            credentialsCollection.updateOne(query, updateDocument);
            logger.info("Updated game stats for {}. {} games incremented.", playerUsername, outcome);
        } catch (MongoException e) {
            logger.error("Failed to update game stats for {}", playerUsername, e);
        }
    }
    
    public static GameStatistics getGameStatisticsByPlayerUsername(String playerUsername) {
        Document query = new Document("playerUsername", playerUsername);
        Document document = credentialsCollection.find(query).first();

        if (document != null) {
            int wonGames = document.getInteger("wonGames");
            int lostGames = document.getInteger("lostGames");
            return new GameStatistics(playerUsername, wonGames, lostGames);
        }

        logger.error("Failed to retrieve game statistics for {}", playerUsername);
        return null;
    }
    
    public static List<GameStatistics> getTop5PlayersByWonGames() {
        List<GameStatistics> topPlayers = new ArrayList<>();

        credentialsCollection.find()
            .sort(Sorts.descending("wonGames"))
            .limit(5)
            .projection(Projections.fields(Projections.include("playerUsername", "wonGames"), Projections.excludeId()))
            .forEach(document -> {
                String username = document.getString("playerUsername");
                int wonGames = document.getInteger("wonGames");
                topPlayers.add(new GameStatistics(username, wonGames, 0)); // Set lostGames to 0 as we're not retrieving it in this query
            });

        return topPlayers;
    }

    public static void close() {
        mongoClient.close();
        logger.info("Connection to Mongo is closed");
    }
}
