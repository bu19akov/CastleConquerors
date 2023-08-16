package main;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import exceptions.ClientNetworkException;
import messagesbase.ResponseEnvelope;
import messagesbase.UniquePlayerIdentifier;
import messagesbase.messagesfromserver.ERequestState;
import messagesbase.messagesfromclient.PlayerRegistration;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientNetwork {
    private WebClient baseWebClient;
    private String gameID;
    private static final Logger logger = LoggerFactory.getLogger(ClientNetwork.class);

    public ClientNetwork(String serverBaseUrl, String gameID) {
        this.baseWebClient = WebClient.builder().baseUrl(serverBaseUrl + "/games")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_VALUE).build();
        this.gameID = gameID;
    }

    public UniquePlayerIdentifier sendPlayerRegistration(PlayerRegistration playerReg) throws ClientNetworkException {
        Mono<ResponseEnvelope> webAccess = baseWebClient.method(HttpMethod.POST).uri("/" + this.gameID + "/players")
                .body(BodyInserters.fromValue(playerReg)) // specify the data which is sent to the server
                .retrieve().bodyToMono(ResponseEnvelope.class); // specify the object returned by the server
        ResponseEnvelope<UniquePlayerIdentifier> resultReg = webAccess.block();
        if (resultReg.getState() == ERequestState.Error) {
            System.err.println("Client error, errormessage: " + resultReg.getExceptionMessage());
        } else {
            return resultReg.getData().get();
        }
        logger.error("Registration error!");
        throw new ClientNetworkException("Registration error!");
    }
}