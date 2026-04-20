package com.example.scoremaster.app.controller;

import com.example.scoremaster.app.security.CustomUserDetails;
import com.example.scoremaster.app.security.JwtUtil;
import jakarta.servlet.http.Cookie;

import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;

    @GetMapping("/test")
    public String getUsers() {
        return "Auth Test";
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(principal.getName());
        String role = userDetails.getAuthorities().iterator().next().getAuthority();
        return ResponseEntity
                .ok(new AuthResponse(userDetails.getUsername(), userDetails.getUser().getUsername(), role));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        // Create an empty cookie with the exact same name and path
        Cookie jwtCookie = new Cookie("jwt", null);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(false); // Make sure this matches the login configuration
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(0); // 0 max-age tells the browser to instantly delete the cookie

        response.addCookie(jwtCookie);
        return org.springframework.http.ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest authRequest, HttpServletResponse response) throws Exception {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getId(), authRequest.getPassword()));
        } catch (Exception e) {
            throw new Exception("Incorrect ID or password", e);
        }

        final CustomUserDetails userDetails = (CustomUserDetails) userDetailsService
                .loadUserByUsername(authRequest.getId());
        final String jwt = jwtUtil.generateToken(userDetails);

        // Attach JWT as HttpOnly Cookie
        Cookie jwtCookie = new Cookie("jwt", jwt);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(false); // Set to true in production if using HTTPS!
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(10 * 60 * 60); // Expires in 10 hours

        response.addCookie(jwtCookie);

        // Grab the user role to send to React for RBAC
        String role = userDetails.getAuthorities().iterator().next().getAuthority();

        return new AuthResponse(userDetails.getUsername(), userDetails.getUser().getUsername(), role);
    }

    @Data
    public static class AuthRequest {
        private String id;
        private String password;
    }

    @Data
    public static class AuthResponse {
        private final String id;
        private final String username;
        private final String role;
    }
}
