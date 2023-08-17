package main;

import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class Endpoints {
	
	@GetMapping("/")
    public String getHomePage() {
        return "homePage";
    }
	
	@GetMapping("/login")
    public String loginPage() {
        return "login";
    }
	
	@GetMapping("/game") 
	public String getGamePage(Model model, HttpSession session) {
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
