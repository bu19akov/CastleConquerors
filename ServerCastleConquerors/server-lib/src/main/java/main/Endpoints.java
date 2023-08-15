package main;

import javax.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import messagesbase.*;
import messagesbase.messagesfromclient.PlayerRegistration;
import messagesbase.messagesfromserver.GameState;
import exceptions.GenericExampleException;
import game.GameService;
import game.GameInfo;

@RestController
@RequestMapping("/games")
public class Endpoints {

    private final GameService gameService = new GameService();

    @GetMapping(produces = MediaType.APPLICATION_XML_VALUE)
    public @ResponseBody UniqueGameIdentifier createNewGame() {
        return gameService.createGame();
    }

    @PostMapping(value = "/{gameID}/players", 
                 consumes = MediaType.APPLICATION_XML_VALUE, 
                 produces = MediaType.APPLICATION_XML_VALUE)
    public @ResponseBody ResponseEnvelope<UniquePlayerIdentifier> registerPlayer(
            @Validated @PathVariable UniqueGameIdentifier gameID,
            @Validated @RequestBody PlayerRegistration playerRegistration) {

        UniquePlayerIdentifier newPlayerID = gameService.registerPlayerToGame(gameID, playerRegistration);
        if (isGameFull(gameID)) {
            GameService.startGame(gameID);
        }

        return new ResponseEnvelope<>(newPlayerID);
    }

    @GetMapping(value = "/{gameID}/states/{playerID}", 
                produces = MediaType.APPLICATION_XML_VALUE)
    public @ResponseBody ResponseEnvelope<GameState> getGameState(
            @Validated @PathVariable UniqueGameIdentifier gameID,
            @Validated @PathVariable UniquePlayerIdentifier playerID) {

        GameService.checkIfGameExist(gameID, "send a request");
        GameService.checkIfPlayerExist(gameID, playerID.getUniquePlayerID());

        GameInfo game = GameService.getGames().get(gameID);
        GameState gameState = new GameState(game.getFilteredMapForPlayer(playerID), 
                                            game.getPlayers(), 
                                            "testID");
        return new ResponseEnvelope<>(gameState);
    }

    private boolean isGameFull(UniqueGameIdentifier gameID) {
        return GameService.getGames().get(gameID).getPlayers().size() == 2;
    }
    
    @ExceptionHandler({ GenericExampleException.class })
	public @ResponseBody ResponseEnvelope<?> handleException(GenericExampleException ex, HttpServletResponse response) {
		ResponseEnvelope<?> result = new ResponseEnvelope<>(ex.getErrorName(), ex.getMessage());
		response.setStatus(HttpServletResponse.SC_OK);
		return result;
	}
}
