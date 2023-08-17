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
        Document document = new Document("playerUsername", playerUsername).append("password", password);
        return credentialsCollection.find(document).first() != null;
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
        Document document = new Document("playerUsername", player.getUsername())
                .append("password", password);
        try {
        	credentialsCollection.insertOne(document);
            logger.info("Player's acount with username {} created successfully", player.getUsername());
        } catch (MongoException e) {
            logger.error("Failed to create new Player Account with username {}", player.getUsername(), e);
        }
    }

    public static void close() {
        mongoClient.close();
        logger.info("Connection to Mongo is closed");
    }
}
