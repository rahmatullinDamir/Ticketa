package com.ticketa.apigateway.jwt;

import io.jsonwebtoken.Claims;

public interface JwtUtil {
    Claims validateAndExtract(String token);
}
