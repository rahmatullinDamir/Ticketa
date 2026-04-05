package com.ticketa.authservice.services;

import com.ticketa.authservice.models.User;

public interface JwtService {
    String generateAccessToken(User user);
}
