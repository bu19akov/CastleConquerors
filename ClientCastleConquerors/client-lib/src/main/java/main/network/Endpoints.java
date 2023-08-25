package main.network;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import database.DatabaseRepository;
import exceptions.ClientNetworkException;
import messagesbase.messagesfromclient.EMove;
import messagesbase.messagesfromclient.PlayerRegistration;
import messagesbase.messagesfromserver.FullMap;
import messagesbase.messagesfromserver.FullMapNode;
import messagesbase.messagesfromserver.PlayerState;
import statistic.GameEndRequest;

@Controller
@RequestMapping("/")
public class Endpoints {
	
	@Autowired
    private EndpointsService endpointsService;
	
	@Autowired
    private ClientNetwork clientNetwork;
	
	@GetMapping("/")
    public String getHomePage() {
        return "home";
    }
	
	@GetMapping("/login")
    public String loginPage() {
        return "login";
    }
	
	@PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, Model model, HttpSession session) {
		try {
            if (endpointsService.verifyLogin(username, password)) {
                session.setAttribute("loggedInUser", username);
                return "redirect:/menu";
            } else {
                model.addAttribute("error", "Invalid username or password");
                return "login";
            }
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "login";
        }
    }
	
	@PostMapping("/create-account")
    public String createUserAccount(@RequestParam String username, @RequestParam String password, @RequestParam String email, Model model, HttpSession session) {
        try {
        	endpointsService.createPlayerAccount(username, password, email);
            session.setAttribute("loggedInUser", username);
            return "redirect:/menu";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "login";
        }
    }
	
	@GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
	
	@GetMapping("/menu") 
	public String getMenuPage(Model model, HttpSession session) {
	    if (!isLoggedIn(session)) return "redirect:/login";

	    String loggedInUser = getLoggedInUser(session);
	    model.addAttribute("loggedInUser", loggedInUser);

	    // Get the error message from the session if it exists
	    if (session.getAttribute("error") != null) {
	        model.addAttribute("errorMessage", "Invalid GameID");
	        session.removeAttribute("error");
	    }

	    return "menu";
	}
	
	@GetMapping("/game") 
    public String getGameID(Model model, HttpSession session) throws Exception {
        if (!isLoggedIn(session)) return "redirect:/login";

        String loggedInUser = getLoggedInUser(session);
        model.addAttribute("loggedInUser", loggedInUser);

        try {
            String gameId = clientNetwork.retrieveUniqueGameIdentifier();  // Use ClientNetwork
            System.out.println("GAME ID: " + gameId);
            return "redirect:/game/" + gameId;
        } catch (Exception e) {
            System.out.println("Can not get Game ID");
            return "";  // Consider returning an error page or error message
        }
    }
	
	@GetMapping("/game/ai/easy")
    public String startGameWithAIEasy(Model model, HttpSession session) throws Exception {
        if (!isLoggedIn(session)) return "redirect:/login";

        String loggedInUser = getLoggedInUser(session);
        model.addAttribute("loggedInUser", loggedInUser);

        PlayerRegistration playerReg = new PlayerRegistration(loggedInUser);
        try {
            String gameId = clientNetwork.connectPlayerToAIGameEasy(playerReg);  // Use ClientNetwork
            System.out.println("GAME ID: " + gameId);
            return "redirect:/game/" + gameId;
        } catch (Exception e) {
            System.out.println("Can not get Game ID");
            return "";  // Consider returning an error page or error message
        }
    }
	
	@GetMapping("/game/ai/hard")
    public String startGameWithAIHard(Model model, HttpSession session) throws Exception {
        if (!isLoggedIn(session)) return "redirect:/login";

        String loggedInUser = getLoggedInUser(session);
        model.addAttribute("loggedInUser", loggedInUser);

        PlayerRegistration playerReg = new PlayerRegistration(loggedInUser);
        try {
            String gameId = clientNetwork.connectPlayerToAIGameHard(playerReg);  // Use ClientNetwork
            System.out.println("GAME ID: " + gameId);
            return "redirect:/game/" + gameId;
        } catch (Exception e) {
            System.out.println("Can not get Game ID");
            return "";  // Consider returning an error page or error message
        }
    }


	@GetMapping("/game/{gameID}") 
    public String getGamePage(@Validated @PathVariable String gameID, Model model, HttpSession session) throws Exception {
        if (!isLoggedIn(session)) return "redirect:/login";

        String loggedInUser = getLoggedInUser(session);
        model.addAttribute("loggedInUser", loggedInUser);

        PlayerRegistration playerReg = new PlayerRegistration(loggedInUser);
        try {
            clientNetwork.sendPlayerRegistration(gameID, playerReg);  
        } catch (ClientNetworkException e) {
        	System.out.println(e.getMessage());
            session.setAttribute("error", "Network error when registering player!");
            return "redirect:/menu";
        }
        try {
            FullMap fullMap = clientNetwork.retrieveMapState(gameID, loggedInUser);
            
            FullMapNode[][] orderedMap = endpointsService.getOrderedArray(fullMap);
            model.addAttribute("map", orderedMap);
            
            PlayerState playerState = clientNetwork.getPlayerState(gameID, loggedInUser);
            model.addAttribute("playerState", playerState);
            
            model.addAttribute("gameID", gameID);

            return "map_example";
        } catch (Exception e) {
        	System.out.println(e.getMessage());
            // Handle error accordingly
        }
        return "game";
    }
	
	@GetMapping("/game/{gameID}/mapdata")
	@ResponseBody
	public FullMapNode[][] getMapData(@Validated @PathVariable String gameID, HttpSession session) {
	    try {
	        // Check if the user is logged in
	        if (!isLoggedIn(session)) {
	            throw new Exception("User not logged in");
	        }

	        String loggedInUser = getLoggedInUser(session);
	        
	        // Fetch and return map data
	        FullMap fullMap = clientNetwork.retrieveMapState(gameID, loggedInUser);
	        return endpointsService.getOrderedArray(fullMap);
	    } catch (Exception e) {
	        return null;  // TODO Consider a better error-handling mechanism here
	    }
	}

	@GetMapping("/game/{gameID}/playerdata")
	@ResponseBody
	public PlayerState getPlayerData(@Validated @PathVariable String gameID, HttpSession session) {
	    try {
	        // Check if the user is logged in
	        if (!isLoggedIn(session)) {
	            throw new Exception("User not logged in");
	        }

	        String loggedInUser = getLoggedInUser(session);
	        
	        // Fetch and return player state data
	        return clientNetwork.getPlayerState(gameID, loggedInUser);
	    } catch (Exception e) {
	        return null;  // Consider a better error-handling mechanism here
	    }
	}

    @GetMapping("/game/{gameID}/opponentdata")
    @ResponseBody
    public PlayerState getOpponentData(@Validated @PathVariable String gameID, HttpSession session) {
        try {
            // Check if the user is logged in
            if (!isLoggedIn(session)) {
                throw new Exception("User not logged in");
            }
            String loggedInUser = getLoggedInUser(session);

            // Fetch and return player state data
            return clientNetwork.getOpponentState(gameID, loggedInUser);
        } catch (Exception e) {
            return null;  // Consider a better error-handling mechanism here
        }
    }
	
	@PostMapping("/game/{gameID}/move")
	public ResponseEntity<String> sendMove(@PathVariable String gameID,
	                                       @RequestParam String move,
	                                       HttpSession session) {
	    try {
	        // Ensure user is logged in
	        if (!isLoggedIn(session)) {
	            return new ResponseEntity<>("User not logged in", HttpStatus.FORBIDDEN);
	        }

	        // Retrieve logged-in user and their player ID
	        String loggedInUser = getLoggedInUser(session);

	        // Send the move command
	        clientNetwork.sendPlayerMove(gameID, loggedInUser, EMove.valueOf(move));

	        return new ResponseEntity<>("Move accepted", HttpStatus.OK);
	    } catch (ClientNetworkException e) {
	    	// TODO logger
	        return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	}
	
	@PostMapping("/game/{gameID}/end")
	@ResponseBody
	public ResponseEntity<String> gameEnd(@PathVariable String gameID,
	                                      @RequestBody GameEndRequest gameEndRequest,
	                                      HttpSession session) {
	    try {
	        if (!isLoggedIn(session)) {
	            return new ResponseEntity<>("User not logged in", HttpStatus.FORBIDDEN);
	        }

	        // Update the database with the game result
	        DatabaseRepository.updatePlayerGameStatistics(gameEndRequest.getPlayerID(), gameEndRequest.getMessage());

	        return new ResponseEntity<>("Game end data received successfully", HttpStatus.OK);
	    } catch (Exception e) {
	        return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	}



	private boolean isLoggedIn(HttpSession session) {
        return getLoggedInUser(session) != null;
    }
	
	private String getLoggedInUser(HttpSession session) {
        return (String) session.getAttribute("loggedInUser");
    }
}
