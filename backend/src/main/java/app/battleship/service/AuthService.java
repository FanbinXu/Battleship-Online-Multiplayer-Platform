package app.battleship.service;

import app.battleship.model.AuthResponse;
import app.battleship.model.LoginRequest;
import app.battleship.model.RegisterRequest;
import app.battleship.model.User;
import app.battleship.persist.UserRepository;
import app.battleship.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }
    
    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already exists");
        }
        
        String passwordHash = passwordEncoder.encode(request.password());
        User user = new User(request.email(), passwordHash);
        return userRepository.save(user);
    }
    
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        
        return new AuthResponse(user.getId(), user.getEmail());
    }
    
    public String generateToken(String userId, String email) {
        return jwtUtil.generateToken(userId, email);
    }
}



