package com.jobhunt.backend.JobHunt_Modulith.authentication.business.request_response;

public record LoginResponse(String token, long expiresIn) {
}
