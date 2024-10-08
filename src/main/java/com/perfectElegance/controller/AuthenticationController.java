package com.perfectElegance.controller;

import com.perfectElegance.exceptions.UserNotVerifiedException;
import com.perfectElegance.modal.ForgotPassword;
import com.perfectElegance.repository.ForgotPasswordRepository;
import com.perfectElegance.repository.UserRepository;
import com.perfectElegance.utils.RegisterRequest;
import com.perfectElegance.exceptions.EmailAlreadyExistsException;
import com.perfectElegance.exceptions.InvalidPasswordException;
import com.perfectElegance.utils.AuthenticationResponse;
import com.perfectElegance.utils.LoginResponse;
import com.perfectElegance.modal.User;
import com.perfectElegance.service.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    @Autowired
    private AuthenticationService authenticationService;



    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest request) {
        try {
            AuthenticationResponse response = authenticationService.register(request);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (EmailAlreadyExistsException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }


    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User request) {

            LoginResponse response = authenticationService.authenticate(request);
            System.out.println(response + " Response");
            return new ResponseEntity<>(response, HttpStatus.OK);

    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String,String>> logout(HttpServletRequest request, Authentication authentication) {
        String token = request.getHeader("Authorization");
        System.out.println("Received token: " + token);

        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            System.out.println("Token after removing 'Bearer ': " + token);

//            authenticationService.logout(token); // Invalidate token and update user status
        } else {
            System.out.println("Token is missing or doesn't start with 'Bearer '");
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "Logged out successfully");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }



}






