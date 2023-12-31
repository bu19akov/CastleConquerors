package main;

import javax.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import messagesbase.*;
import messagesbase.messagesfromclient.PlayerMove;
import messagesbase.messagesfromclient.PlayerRegistration;
import messagesbase.messagesfromserver.EPlayerGameState;
import messagesbase.messagesfromserver.GameState;
import exceptions.GameNotFoundException;
import exceptions.GenericExampleException;
import main.game.GameInfo;
import main.game.GameService;

@RestController
@RequestMapping("/games")
public class Endpoints {

    private final GameService gameService = new GameService();

    @GetMapping(produces = MediaType.APPLICATION_XML_VALUE)
    public @ResponseBody UniqueGameIdentifier createNewGame() {
        return gameService.createGame();
    }
    
    @PostMapping(value = "/ai/easy", 
            consumes = MediaType.APPLICATION_XML_VALUE, 
            produces = MediaType.APPLICATION_XML_VALUE)
	public @ResponseBody UniqueGameIdentifier registerPlayerVSEasyAI(
	       @Validated @RequestBody PlayerRegistration playerRegistration) {
    	UniqueGameIdentifier gameID = gameService.createGame();

		gameService.registerPlayerToGame(gameID, new PlayerRegistration("AI_Easy"));
	
	   return gameID;
	}
    
    @PostMapping(value = "/ai/hard", 
            consumes = MediaType.APPLICATION_XML_VALUE, 
            produces = MediaType.APPLICATION_XML_VALUE)
	public @ResponseBody UniqueGameIdentifier registerPlayerVSHardAI(
	       @Validated @RequestBody PlayerRegistration playerRegistration) {
    	UniqueGameIdentifier gameID = gameService.createGame();

		gameService.registerPlayerToGame(gameID, new PlayerRegistration("AI_Hard"));
	
	   return gameID;
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
    	UniquePlayerIdentifier newPlayerID = new UniquePlayerIdentifier();
    	try {
    		newPlayerID = gameService.registerPlayerToGame(gameID, playerRegistration);
    	} catch (GameNotFoundException e) {
    		return new ResponseEnvelope<>("GameNotFoundException", "Invalid GameID");
    	}
        if (isGameFull(gameID)) {
            gameService.startGame(gameID);
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
    		GameService.processMove(gameID, playerMove);
    	} catch (IllegalArgumentException e) {
    		return new ResponseEnvelope<>("IllegalArgumentException", e.getMessage());
    	}
    	if (GameService.getGames().get(gameID).getCurrentPlayer().getState() != EPlayerGameState.Lost && 
    			GameService.getGames().get(gameID).getCurrentPlayer().getState() != EPlayerGameState.Won) {
	    	try {
		    	if (gameService.doesGameContainsAIEasy(gameID)) {
		    		gameService.makeAIEasyMove(gameID);
		    	}
	    	} catch (Exception e) {
	    		return new ResponseEnvelope<>("Exception", e.getMessage());
	    	}
	    	try {
		    	if (gameService.doesGameContainsAIHard(gameID)) {
		    		gameService.makeAIHardMove(gameID);
		    	}
	    	} catch (Exception e) {
	    		return new ResponseEnvelope<>("Exception", e.getMessage());
	    	}
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
