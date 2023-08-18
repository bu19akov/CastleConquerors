package main;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import messagesbase.messagesfromclient.PlayerRegistration;
import messagesbase.messagesfromserver.FullMap;
import messagesbase.messagesfromserver.FullMapNode;

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
	
	// TODO Get for /game/gameID


	@GetMapping("/game/{gameID}") 
    public String getGamePage(@Validated @PathVariable String gameID, Model model, HttpSession session) throws Exception {
        if (!isLoggedIn(session)) return "redirect:/login";

        String loggedInUser = getLoggedInUser(session);
        model.addAttribute("loggedInUser", loggedInUser);

        PlayerRegistration playerReg = new PlayerRegistration(loggedInUser);
        try {
            String uniquePlayerID = clientNetwork.sendPlayerRegistration(gameID, playerReg);  // Use ClientNetwork
            FullMap fullMap = clientNetwork.retrieveMapState(gameID, uniquePlayerID);
            
            FullMapNode[][] orderedMap = endpointsService.getOrderedArray(fullMap);
            model.addAttribute("map", orderedMap);

            return "map_example";
        } catch (Exception e) {
        	System.out.println(e.getMessage());
            // Handle error accordingly
        }
        return "game";
    }

	private boolean isLoggedIn(HttpSession session) {
        return getLoggedInUser(session) != null;
    }
	
	private String getLoggedInUser(HttpSession session) {
        return (String) session.getAttribute("loggedInUser");
    }
}
