package asia.canopy.tree.controller;

import asia.canopy.tree.config.CustomUserDetails;
import asia.canopy.tree.domain.User;
import asia.canopy.tree.dto.PasswordDto;
import asia.canopy.tree.dto.ProfileSetupDto;
import asia.canopy.tree.dto.RegistrationDto;
import asia.canopy.tree.repository.UserRepository;
import asia.canopy.tree.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
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

    @Autowired
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

    // 1단계: 이메일 회원가입
    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("registrationDto") RegistrationDto registrationDto,
                               BindingResult result,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "register";
        }

        try {
            userService.registerUser(registrationDto.getEmail());
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

            session.setAttribute("verifiedEmail", true);
            redirectAttributes.addFlashAttribute("success", "Email verified successfully! Please set your password.");
            return "redirect:/set-password";
        } else {
            redirectAttributes.addFlashAttribute("error", "Invalid or expired verification token");
            return "redirect:/login";
        }
    }

    @GetMapping("/set-password")
    public String showSetPasswordPage(Model model, HttpSession session) {
        Boolean verifiedEmail = (Boolean) session.getAttribute("verifiedEmail");
        if (verifiedEmail == null || !verifiedEmail) {
            return "redirect:/login";
        }

        model.addAttribute("passwordDto", new PasswordDto());
        return "set-password";
    }

    @PostMapping("/set-password")
    public String setPassword(@Valid @ModelAttribute("passwordDto") PasswordDto passwordDto,
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

    // 2단계: 프로필 설정 페이지
    @GetMapping("/profile-setup")
    public String showProfileSetupPage(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userRepository.findByEmail(userDetails.getEmail()).orElseThrow();

        if (user.isProfileCompleted()) {
            return "redirect:/dashboard";
        }

        model.addAttribute("profileSetupDto", new ProfileSetupDto());
        return "profile-setup";
    }

    // 2단계: 프로필 설정 처리
    @PostMapping("/profile-setup")
    public String setupProfile(@Valid @ModelAttribute("profileSetupDto") ProfileSetupDto profileSetupDto,
                               BindingResult result,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "profile-setup";
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        try {
            userService.completeProfile(
                    userDetails.getEmail(),
                    profileSetupDto.getNickname(),
                    profileSetupDto.getAvatar()
            );
            redirectAttributes.addFlashAttribute("success", "Profile setup completed!");
            return "redirect:/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/profile-setup";
        }
    }
}