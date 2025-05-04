package asia.canopy.tree.controller;

import asia.canopy.tree.domain.User;
import asia.canopy.tree.dto.RegistrationDto;
import asia.canopy.tree.repository.UserRepository;
import asia.canopy.tree.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired  // 추가
    private UserRepository userRepository;

    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        model.addAttribute("registrationDto", new RegistrationDto());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("registrationDto") RegistrationDto registrationDto,
                               BindingResult result,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "register";
        }

        try {
            userService.registerUser(registrationDto.getEmail(), registrationDto.getName());
            redirectAttributes.addFlashAttribute("success", "Registration successful! Please check your email for verification.");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        }
    }

    @GetMapping("/verify")
    public String verifyEmail(@RequestParam String token, RedirectAttributes redirectAttributes, HttpSession session) {
        boolean verified = userService.verifyEmail(token);

        if (verified) {
            // 이메일을 세션에 저장
            Optional<User> user = userRepository.findByVerificationToken(token);
            if (user.isPresent()) {
                session.setAttribute("email", user.get().getEmail());
            }

            session.setAttribute("verifiedEmail", "true");
            redirectAttributes.addFlashAttribute("success", "Email verified successfully! Please set your password.");
            return "redirect:/set-password";
        } else {
            redirectAttributes.addFlashAttribute("error", "Invalid or expired verification token");
            return "redirect:/login";
        }
    }

    @GetMapping("/set-password")
    public String showSetPasswordPage(Model model, HttpSession session) {
        if (session.getAttribute("verifiedEmail") == null) {
            return "redirect:/login";
        }

        model.addAttribute("passwordDto", new PasswordDto());
        return "set-password";
    }

    @PostMapping("/set-password")
    public String setPassword(@Valid @ModelAttribute("passwordDto") asia.canopy.tree.dto.PasswordDto passwordDto,
                              BindingResult result,
                              RedirectAttributes redirectAttributes,
                              HttpSession session) {
        if (result.hasErrors()) {
            return "set-password";
        }

        if (session.getAttribute("verifiedEmail") == null) {
            return "redirect:/login";
        }

        String email = (String) session.getAttribute("email");

        try {
            userService.setPassword(email, passwordDto.getPassword());
            session.removeAttribute("verifiedEmail");
            session.removeAttribute("email");

            redirectAttributes.addFlashAttribute("success", "Password set successfully! You can now login.");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/set-password";
        }
    }

    private class PasswordDto {
    }
}