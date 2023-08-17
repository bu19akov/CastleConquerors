package main;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import messagesbase.ResponseEnvelope;
import messagesbase.UniqueGameIdentifier;
import messagesbase.UniquePlayerIdentifier;
import messagesbase.messagesfromclient.PlayerRegistration;

@Controller
@RequestMapping("/")
public class Endpoints {
	
	@Autowired
    private EndpointsService endpointsService;
	
	@Autowired
	private RestTemplate restTemplate;
	
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
    public String createBankAccount(@RequestParam String username, @RequestParam String password, Model model, HttpSession session) {
        try {
        	endpointsService.createPlayerAccount(username, password);
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
        return "redirect:/login";
    }
	
	@GetMapping("/menu") 
	public String getMenuPage(Model model, HttpSession session) {
		if (!isLoggedIn(session)) return "redirect:/login";
    	String loggedInUser = getLoggedInUser(session);
        model.addAttribute("loggedInUser", loggedInUser);
		return "menu";
	}
	
	@GetMapping("/game") 
	public String getGameID(Model model, HttpSession session) {
	    if (!isLoggedIn(session)) return "redirect:/login";
	    
	    String loggedInUser = getLoggedInUser(session);
	    model.addAttribute("loggedInUser", loggedInUser);

	    String url = "http://localhost:18235/games/";

	    UniqueGameIdentifier gameID = null;

	    try {
	        gameID = restTemplate.getForObject(url, UniqueGameIdentifier.class);
	    } catch (Exception e) {
	        System.out.println("Can not get Game ID");
	    }

	    if (gameID != null && !gameID.getUniqueGameID().isBlank()) {
	        System.out.println("GAME ID: " + gameID.getUniqueGameID());
	        return "redirect:/game/" + gameID.getUniqueGameID();
	    } else {
	    	System.out.println("Game ID can not be empty!");
	    	return "";
	    }
	}


	@GetMapping("/game/{gameID}") 
	public String getGamePage(@Validated @PathVariable String gameID, 
	                          Model model, 
	                          HttpSession session) {
		System.out.println("REDIRECTING TO NEW PAGE...");
	    if (!isLoggedIn(session)) return "redirect:/login";
	    
	    String loggedInUser = getLoggedInUser(session);
	    model.addAttribute("loggedInUser", loggedInUser);

	    String url = "http://localhost:18235/games/" + gameID + "/players";
	    PlayerRegistration playerReg = new PlayerRegistration(loggedInUser);
	    ResponseEnvelope<UniquePlayerIdentifier> playerID = null;
	    
	    try {
	        ResponseEntity<ResponseEnvelope<UniquePlayerIdentifier>> responseEntity = restTemplate.exchange(
	            url, 
	            HttpMethod.POST, 
	            new HttpEntity<>(playerReg), 
	            new ParameterizedTypeReference<ResponseEnvelope<UniquePlayerIdentifier>>() {}
	        );
	        playerID = responseEntity.getBody();
	    } catch (Exception e) {
	        System.out.println("Wrong player Registration!");
	    }

	    System.out.println(playerID.getData().get().getUniquePlayerID());
	    return "game";
	}



	private boolean isLoggedIn(HttpSession session) {
        return getLoggedInUser(session) != null;
    }
	
	private String getLoggedInUser(HttpSession session) {
        return (String) session.getAttribute("loggedInUser");
    }
}
