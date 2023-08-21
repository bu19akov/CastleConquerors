package main;

import exceptions.ClientNetworkException;
import messagesbase.ResponseEnvelope;
import messagesbase.UniqueGameIdentifier;
import messagesbase.UniquePlayerIdentifier;
import messagesbase.messagesfromclient.EMove;
import messagesbase.messagesfromclient.PlayerMove;
import messagesbase.messagesfromclient.PlayerRegistration;
import messagesbase.messagesfromserver.ERequestState;
import messagesbase.messagesfromserver.FullMap;
import messagesbase.messagesfromserver.GameState;
import messagesbase.messagesfromserver.PlayerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Set;

@Component
public class ClientNetwork {
    private static final Logger logger = LoggerFactory.getLogger(ClientNetwork.class);
    private WebClient baseWebClient;

    @Autowired
    public ClientNetwork(@Value("${server.backend.url}") String serverBaseUrl) {
        this.baseWebClient = WebClient.builder().baseUrl(serverBaseUrl + "/games")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_VALUE).build();
    }

    private static void checkGameState(ResponseEnvelope<GameState> gameState) throws ClientNetworkException {
        if (gameState.getState() == ERequestState.Error) {
            System.err.println("Client error, errormessage: " + gameState.getExceptionMessage());
            throw new ClientNetworkException("Error retrieving map state");
        }
    }

    public String retrieveUniqueGameIdentifier() throws ClientNetworkException {
        try {
            UniqueGameIdentifier uniqueGameIdentifier = baseWebClient.get()
                    .uri("/")
                    .retrieve()
                    .bodyToMono(UniqueGameIdentifier.class)
                    .block();
            return uniqueGameIdentifier.getUniqueGameID();
        } catch (Exception e) {
            logger.error("Error while fetching UniqueGameIdentifier", e);
            throw new ClientNetworkException("Error retrieving game identifier");
        }
    }
    
    public String connectPlayerToAIGameEasy(PlayerRegistration playerReg) throws ClientNetworkException {
        try {
            Mono<UniqueGameIdentifier> webAccess = baseWebClient.method(HttpMethod.POST).uri("/ai/easy")
                    .body(BodyInserters.fromValue(playerReg)) // specify the data which is sent to the server
                    .retrieve().bodyToMono(UniqueGameIdentifier.class); // specify the object returned by the server
           UniqueGameIdentifier resultReg = webAccess.block();
           return resultReg.getUniqueGameID();
        } catch (Exception e) {
            logger.error("Error while creating Game with AI Easy", e);
            throw new ClientNetworkException("Error while creating Game with AI Easy");
        }
    }

    public String sendPlayerRegistration(String gameID, PlayerRegistration playerReg) throws ClientNetworkException {  // Get unique player ID
        Mono<ResponseEnvelope> webAccess = baseWebClient.method(HttpMethod.POST).uri("/" + gameID + "/players")
                .body(BodyInserters.fromValue(playerReg)) // specify the data which is sent to the server
                .retrieve().bodyToMono(ResponseEnvelope.class); // specify the object returned by the server
        ResponseEnvelope<UniquePlayerIdentifier> resultReg = webAccess.block();
        if (resultReg.getState() == ERequestState.Error) {
        	logger.error("Registration error!");
            throw new ClientNetworkException("Registration error!");
        } else {
            return resultReg.getData().get().getUniquePlayerID();
        }
    }

    public FullMap retrieveMapState(String gameID, String playerID) throws ClientNetworkException {
        Mono<ResponseEnvelope> webAccess = baseWebClient.method(HttpMethod.GET)
                .uri("/" + gameID + "/states/" + playerID)
                .retrieve().bodyToMono(ResponseEnvelope.class); // specify the object returned by the server
        ResponseEnvelope<GameState> gameState = webAccess.block();

        checkGameState(gameState);
        return gameState.getData().get().getMap();
    }

    public PlayerState getPlayerState(String gameID, String playerID) throws ClientNetworkException {
        // h-p(s)://<domain>:<port>/games/<GameID>/states/<PlayerID>
        Mono<ResponseEnvelope> webAccess = baseWebClient.method(HttpMethod.GET)
                .uri("/" + gameID + "/states/" + playerID)
                .retrieve().bodyToMono(ResponseEnvelope.class);
        ResponseEnvelope<GameState> gameState = webAccess.block();
        checkGameState(gameState);
        Set<PlayerState> playerStates = gameState.getData().get().getPlayers();

        for (PlayerState playerState : playerStates) {
            if (playerState.getUniquePlayerID().equals(playerID)) {
                return playerState;
            }
            else if (!playerState.getUniquePlayerID().equals(playerID) && playerState.getCollectedTreasure()) {
                // TODO: 2023-08-16 notify about enemy collecting treasure
            }
        }

        throw new ClientNetworkException("Client not found");
    }

    public void sendPlayerMove(String gameID, String playerID, EMove move) throws ClientNetworkException {
        PlayerMove playerMove = PlayerMove.of(playerID, move);
        Mono<ResponseEnvelope> webAccess = baseWebClient.method(HttpMethod.POST)
                .uri("/" + gameID + "/moves/")
                .body(BodyInserters.fromValue(playerMove))
                .retrieve().bodyToMono(ResponseEnvelope.class);
        ResponseEnvelope<?> requestState = webAccess.block();

        if (requestState.getState() == ERequestState.Error) {
            System.err.println("Client error, errormessage: " + requestState.getExceptionMessage());

            logger.error("Your move was not registered");
            throw new ClientNetworkException("Movement error!");
        }
    }
}