package database;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

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

    public static void createPlayerAccount(Player player, String password) {
        // Hash the password
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        Document document = new Document("playerUsername", player.getUsername())
                .append("password", hashedPassword);
        try {
        	credentialsCollection.insertOne(document);
            logger.info("Player's account with username {} created successfully", player.getUsername());
        } catch (MongoException e) {
            logger.error("Failed to create new Player Account with username {}", player.getUsername(), e);
        }
    }

    public static void deletePlayerAccount(Player player, String password) {
        Document account = new Document("playerUsername", player.getUsername())
                .append("password", password);

        try {
            Document accountFromDatabase = credentialsCollection.find(account).first();

            if (accountFromDatabase == null) {
                throw new RuntimeException("Failed to delete account: no account with such user credentials");
            }

            credentialsCollection.deleteOne(accountFromDatabase);
        } catch (MongoException e) {
            logger.error("Failed to delete Player Account with username {}", player.getUsername(), e);
        } catch (RuntimeException e) {
            logger.error("Failed to create new Player Account with username {}", player.getUsername(), e);
        }
    }

    public static void close() {
        mongoClient.close();
        logger.info("Connection to Mongo is closed");
    }
}
