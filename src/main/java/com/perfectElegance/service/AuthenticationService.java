package com.perfectElegance.service;

import com.perfectElegance.Dto.MailBody;
import com.perfectElegance.exceptions.UserBlockedException;
import com.perfectElegance.exceptions.UserNotVerifiedException;
import com.perfectElegance.modal.ForgotPassword;
import com.perfectElegance.repository.ForgotPasswordRepository;
import com.perfectElegance.utils.RegisterRequest;
import com.perfectElegance.exceptions.EmailAlreadyExistsException;
import com.perfectElegance.exceptions.InvalidPasswordException;
import com.perfectElegance.utils.AuthenticationResponse;
import com.perfectElegance.utils.LoginResponse;
import com.perfectElegance.modal.Role;
import com.perfectElegance.modal.User;
import com.perfectElegance.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Objects;
import java.util.Optional;

@Service
public class AuthenticationService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private OTPService otpService;

    @Autowired
    private  ModelMapper modelMapper;

    @Autowired
    private EmailService emailService;


    @Autowired
    private ForgotPasswordRepository forgotPasswordRepository;




    public AuthenticationResponse register(RegisterRequest request) {
        // Check if the email already exists
        Optional<User> existingUser = userRepository.findByEmail(request.getEmail());
        if (existingUser.isPresent()) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        // Map RegisterRequest to User
        User user = modelMapper.map(request, User.class);
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        user.setPassword(encodedPassword);
        user.setRole(Role.USER);

        // Save the new user
        user = userRepository.save(user);

        // Generate and send OTP
        Integer otp = otpService.generateOTP();
        String email = user.getEmail();
        MailBody mailBody = MailBody.builder()
                .to(email)
                .text("This is OTP for your Registration request: " + otp)
                .subject("OTP for Registration Request")
                .build();
        Date expirationTime = new Date(System.currentTimeMillis() + 300000);


        ForgotPassword fb = ForgotPassword.builder()
                .otp(otp)
                .expirationTime(expirationTime)
                .user(user)
                .build();

        emailService.sendSimpleMessage(mailBody);
        forgotPasswordRepository.save(fb);

        return new AuthenticationResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(),expirationTime);
    }



    public LoginResponse authenticate(User request) {

        // Fetch the user by email before authentication
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(() ->
                new InvalidPasswordException("Invalid email or password"));

        // Check if the user is blocked
        if (user.isBlocked()) {
            System.out.println("hey i am working ");
            throw new UserBlockedException("User account is blocked");
        }

        // Check if the user is verified
        if (!user.isVerified()) {
            throw new UserNotVerifiedException("User is not verified");
        }

        try {
            // Authenticate the user after the checks
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (AuthenticationException e) {
            // Handle invalid email or password
            throw new InvalidPasswordException("Invalid email or password");
        }

        // Generate the token and update user status
        String token = jwtService.generateToken(user);
        boolean loggedIn = true;
        user.setOnline(true);
        userRepository.save(user);

        // Return the login response
        return new LoginResponse(
                user.getId(), token, user.getName(), user.getEmail(),
                user.getPhoneNumber(), user.getGender(), user.getRole(), loggedIn, user.isBlocked()
        );
    }




}
