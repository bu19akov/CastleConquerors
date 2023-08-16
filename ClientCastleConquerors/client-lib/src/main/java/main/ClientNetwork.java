package main;

import exceptions.ClientNetworkException;
import messagesbase.ResponseEnvelope;
import messagesbase.UniqueGameIdentifier;
import messagesbase.UniquePlayerIdentifier;
import messagesbase.messagesfromclient.PlayerRegistration;
import messagesbase.messagesfromserver.ERequestState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class ClientNetwork {
    private static final Logger logger = LoggerFactory.getLogger(ClientNetwork.class);
    private WebClient baseWebClient;
    private String gameID;

    public ClientNetwork(String serverBaseUrl) {
        this.baseWebClient = WebClient.builder().baseUrl(serverBaseUrl + "/games")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_VALUE).build();
    }

    public String retrieveUniqueGameIdentifier() throws ClientNetworkException {
        try {
            UniqueGameIdentifier uniqueGameIdentifier = baseWebClient.get()
                    .uri("/")
                    .retrieve()
                    .bodyToMono(UniqueGameIdentifier.class)
                    .block();
            this.gameID = uniqueGameIdentifier.getUniqueGameID();
            return this.gameID;
        } catch (Exception e) {
            logger.error("Error while fetching UniqueGameIdentifier", e);
            throw new ClientNetworkException("Error retrieving game identifier");
        }

//        return
    }

    public String sendPlayerRegistration(PlayerRegistration playerReg) throws ClientNetworkException {
        Mono<ResponseEnvelope> webAccess = baseWebClient.method(HttpMethod.POST).uri("/" + this.gameID + "/players")
                .body(BodyInserters.fromValue(playerReg)) // specify the data which is sent to the server
                .retrieve().bodyToMono(ResponseEnvelope.class); // specify the object returned by the server
        ResponseEnvelope<UniquePlayerIdentifier> resultReg = webAccess.block();
        if (resultReg.getState() == ERequestState.Error) {
            System.err.println("Client error, errormessage: " + resultReg.getExceptionMessage());
        } else {
            return resultReg.getData().get().getUniquePlayerID();
        }
        logger.error("Registration error!");
        throw new ClientNetworkException("Registration error!");
    }


}