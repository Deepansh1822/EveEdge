package in.sfp.main.controllers;

import in.sfp.main.model.User;
import in.sfp.main.repo.UserRepository;
import in.sfp.main.security.JwtUtil;
import in.sfp.main.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @org.springframework.beans.factory.annotation.Value("${admin.registration.key}")
    private String adminRegistrationKey;

    @PostMapping("/admin-signup")
    @ResponseBody
    public ResponseEntity<?> registerAdmin(@RequestBody Map<String, String> data) {
        String username = data.get("username");
        String email = data.get("email");
        String password = data.get("password");
        String mobileNumber = data.get("mobileNumber");
        String organization = data.get("organization");
        String securityKey = data.get("securityKey");

        Map<String, Object> response = new HashMap<>();

        if (!adminRegistrationKey.equals(securityKey)) {
            response.put("success", false);
            response.put("message", "Invalid security key");
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body(response);
        }

        if (userRepository.existsByUsername(username)) {
            response.put("success", false);
            response.put("message", "Username is already taken");
            return ResponseEntity.badRequest().body(response);
        }

        if (userRepository.existsByEmail(email)) {
            response.put("success", false);
            response.put("message", "Email is already in use");
            return ResponseEntity.badRequest().body(response);
        }

        User user = new User(username, passwordEncoder.encode(password), email);
        user.setMobileNumber(mobileNumber);
        user.setOrganization(organization);

        Set<String> roles = new HashSet<>();
        roles.add("ADMIN");
        roles.add("USER");
        user.setRoles(roles);

        userRepository.save(user);

        response.put("success", true);
        response.put("message", "Admin registered successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    @ResponseBody
    public ResponseEntity<?> authenticateUser(@RequestBody Map<String, String> data,
            jakarta.servlet.http.HttpServletResponse response) {
        String username = data.get("username");
        String password = data.get("password");
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            String jwt = jwtUtil.generateToken(userDetails);

            // Set JWT as HttpOnly Cookie for Page Access
            jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("jwtToken", jwt);
            cookie.setHttpOnly(true);
            cookie.setSecure(false); // Set to true in production with HTTPS
            cookie.setPath("/");
            cookie.setMaxAge(24 * 60 * 60); // 24 hours
            response.addCookie(cookie);

            Map<String, Object> authResponse = new HashMap<>();
            authResponse.put("success", true);
            authResponse.put("token", jwt);
            authResponse.put("username", userDetails.getUsername());
            authResponse.put("email", userDetails.getEmail());
            authResponse.put("mobileNumber", userDetails.getMobileNumber());
            authResponse.put("organization", userDetails.getOrganization());
            authResponse.put("roles", userDetails.getAuthorities());

            return ResponseEntity.ok(authResponse);
        } catch (Exception e) {
            Map<String, Object> authResponse = new HashMap<>();
            authResponse.put("success", false);
            authResponse.put("message", "Invalid username or password");
            return ResponseEntity.badRequest().body(authResponse);
        }
    }

    @PostMapping("/signup")
    @ResponseBody
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> data) {
        String username = data.get("username");
        String email = data.get("email");
        String password = data.get("password");
        String mobileNumber = data.get("mobileNumber");
        String organization = data.get("organization");
        Map<String, Object> response = new HashMap<>();

        if (userRepository.existsByUsername(username)) {
            response.put("success", false);
            response.put("message", "Username is already taken");
            return ResponseEntity.badRequest().body(response);
        }

        if (userRepository.existsByEmail(email)) {
            response.put("success", false);
            response.put("message", "Email is already in use");
            return ResponseEntity.badRequest().body(response);
        }

        // Create new user account
        User user = new User(username, passwordEncoder.encode(password), email);
        user.setMobileNumber(mobileNumber);
        user.setOrganization(organization);

        Set<String> roles = new HashSet<>();
        roles.add("USER");
        user.setRoles(roles);

        userRepository.save(user);

        response.put("success", true);
        response.put("message", "User registered successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @ResponseBody
    public ResponseEntity<?> logoutUser(jakarta.servlet.http.HttpServletResponse response) {
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("jwtToken", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0); // Expire immediately
        response.addCookie(cookie);

        Map<String, Object> data = new HashMap<>();
        data.put("success", true);
        data.put("message", "Logged out successfully");
        return ResponseEntity.ok(data);
    }
}
