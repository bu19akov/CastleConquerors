package main;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/")
public class Endpoints {
	
	@Autowired
    private EndpointsService endpointsService;

	
	@GetMapping("/")
    public String getHomePage() {
        return "homePage";
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
	
	@GetMapping("/menu") 
	public String getMenuPage(Model model, HttpSession session) {
//		if (!isLoggedIn(session)) return "redirect:/login";
//    	String loggedInUser = getLoggedInUser(session);
//        model.addAttribute("loggedInUser", loggedInUser);
		return "gamePage";
	}
	
	private boolean isLoggedIn(HttpSession session) {
        return getLoggedInUser(session) != null;
    }
	
	private String getLoggedInUser(HttpSession session) {
        return (String) session.getAttribute("loggedInUser");
    }
}
