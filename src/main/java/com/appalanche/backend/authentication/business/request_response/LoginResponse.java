package com.appalanche.backend.authentication.business.request_response;

import java.util.UUID;

public record LoginResponse(UUID accountId, String email) {
}
