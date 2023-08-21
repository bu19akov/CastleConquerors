package main;

import javax.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import messagesbase.*;
import messagesbase.messagesfromclient.PlayerMove;
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
    
    @PostMapping(value = "ai/easy/", 
            consumes = MediaType.APPLICATION_XML_VALUE, 
            produces = MediaType.APPLICATION_XML_VALUE)
	public @ResponseBody ResponseEnvelope<UniqueGameIdentifier> registerPlayerVSEasyAI(
	       @Validated @RequestBody PlayerRegistration playerRegistration) {
    	UniqueGameIdentifier gameID = gameService.createGame();
		if (GameService.getGames().containsKey(gameID) && GameService.getGames().get(gameID).containsPlayerWithID(playerRegistration.getPlayerUsername())) {
			return new ResponseEnvelope<>("IllegalArgumentException", "Player is already registered");
		}
		gameService.registerPlayerToGame(gameID, new PlayerRegistration("AI_Easy"));
		gameService.registerPlayerToGame(gameID, playerRegistration);
		gameService.startGameWithAIEasy(gameID);
	
	   return new ResponseEnvelope<>(gameID);
	}

    @PostMapping(value = "/{gameID}/players", 
                 consumes = MediaType.APPLICATION_XML_VALUE, 
                 produces = MediaType.APPLICATION_XML_VALUE)
    public @ResponseBody ResponseEnvelope<UniquePlayerIdentifier> registerPlayer(
            @Validated @PathVariable UniqueGameIdentifier gameID,
            @Validated @RequestBody PlayerRegistration playerRegistration) {
    	if (GameService.getGames().containsKey(gameID) && GameService.getGames().get(gameID).containsPlayerWithID(playerRegistration.getPlayerUsername())) {
        	return new ResponseEnvelope<>("IllegalArgumentException", "Player is already registered");
        }
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
    
    @PostMapping(value = "/{gameID}/moves", 
            consumes = MediaType.APPLICATION_XML_VALUE, 
            produces = MediaType.APPLICATION_XML_VALUE)
	public @ResponseBody ResponseEnvelope<?> sendMove(
	   @Validated @PathVariable UniqueGameIdentifier gameID,
	   @Validated @RequestBody PlayerMove playerMove) {
	
    	try {
    		gameService.processMove(gameID, playerMove);
    	} catch (IllegalArgumentException e) {
    		return new ResponseEnvelope<>("IllegalArgumentException", e.getMessage());
    	}
	    return new ResponseEnvelope<>("Move processed successfully");
	}
    
    @ExceptionHandler({ GenericExampleException.class })
	public @ResponseBody ResponseEnvelope<?> handleException(GenericExampleException ex, HttpServletResponse response) {
		ResponseEnvelope<?> result = new ResponseEnvelope<>(ex.getErrorName(), ex.getMessage());
		response.setStatus(HttpServletResponse.SC_OK);
		return result;
	}
}
