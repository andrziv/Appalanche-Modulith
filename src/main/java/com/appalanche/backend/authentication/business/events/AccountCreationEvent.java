package com.appalanche.backend.authentication.business.events;

import java.util.UUID;

public record AccountCreationEvent(
        UUID accountId,
        String firstName,
        String lastName
) {
}
