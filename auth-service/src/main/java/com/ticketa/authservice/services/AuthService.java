package com.ticketa.authservice.services;

import com.ticketa.authservice.models.dto.LoginRequest;
import com.ticketa.authservice.models.dto.RefreshRequest;
import com.ticketa.authservice.models.dto.RegisterRequest;
import com.ticketa.authservice.models.dto.TokenResponse;

public interface AuthService {
    TokenResponse register(RegisterRequest registerRequest);
    TokenResponse login(LoginRequest loginRequest);
    TokenResponse refresh(RefreshRequest refreshRequest);

}
