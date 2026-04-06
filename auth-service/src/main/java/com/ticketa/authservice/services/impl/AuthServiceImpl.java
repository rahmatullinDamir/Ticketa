package com.ticketa.authservice.services.impl;


import com.ticketa.authservice.models.User;
import com.ticketa.authservice.models.dto.LoginRequest;
import com.ticketa.authservice.models.dto.RefreshRequest;
import com.ticketa.authservice.models.dto.RegisterRequest;
import com.ticketa.authservice.models.dto.TokenResponse;
import com.ticketa.authservice.models.enums.Role;
import com.ticketa.authservice.repositories.UserRepository;
import com.ticketa.authservice.services.AuthService;
import com.ticketa.authservice.services.JwtService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final StringRedisTemplate redisTemplate;
    private static final String LUA_GET_AND_DELETE =
            "local val = redis.call('get', KEYS[1]) " +
                    "if val then redis.call('del', KEYS[1]) end " +
                    "return val";

    @Transactional
    public TokenResponse register(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.email())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User with this email already exists");
        }

        User user = User.builder()
                .email(registerRequest.email())
                .password(passwordEncoder.encode(registerRequest.password()))
                .role(Role.valueOf(registerRequest.role().toUpperCase()))
                .build();

        userRepository.save(user);

        return createTokenResponse(user);
    }

    public TokenResponse login(LoginRequest loginRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password())
        );

        User user = userRepository.findByEmail(loginRequest.email()).orElseThrow();

        return createTokenResponse(user);
    }

    public TokenResponse refresh(RefreshRequest refreshRequest) {
        String redisKey = "refresh:tokens:" + refreshRequest.refreshToken();

        DefaultRedisScript<String> script = new DefaultRedisScript<>(LUA_GET_AND_DELETE, String.class);
        String email = redisTemplate.execute(script, Collections.singletonList(redisKey));

        if (email == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or reused refresh token");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        return createTokenResponse(user);
    }


    private TokenResponse createTokenResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = UUID.randomUUID().toString();

        redisTemplate.opsForValue().set(
                "refresh:tokens:" + refreshToken,
                user.getEmail(),
                Duration.ofDays(7)
        );

        return new TokenResponse(accessToken, refreshToken);
    }


}
