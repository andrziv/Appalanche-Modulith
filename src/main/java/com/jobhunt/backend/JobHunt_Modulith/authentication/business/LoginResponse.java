package com.jobhunt.backend.JobHunt_Modulith.authentication.business;

public record LoginResponse(String token, long expiresIn) {
}
