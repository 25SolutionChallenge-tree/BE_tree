package asia.canopy.tree.AuthTest;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@Profile("test")
public class TestController {

    @GetMapping("/")
    @ResponseBody
    public String home() {
        return "home";
    }

    @GetMapping("/dashboard")
    @ResponseBody
    public String dashboard() {
        return "dashboard";
    }
}
