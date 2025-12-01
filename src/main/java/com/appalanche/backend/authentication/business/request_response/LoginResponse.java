package com.appalanche.backend.authentication.business.request_response;

public record LoginResponse(String token, long expiresIn) {
}
