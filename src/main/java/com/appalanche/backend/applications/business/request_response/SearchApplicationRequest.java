package com.appalanche.backend.applications.business.request_response;

import com.appalanche.backend.applications.business.validation.ValidTimezone;

import java.time.LocalDate;
import java.util.List;

public record SearchApplicationRequest(
        String search,
        List<String> statusCodes,
        List<String> experienceLevelCodes,
        List<String> interestCriteria,

        LocalDate appliedAfter,
        LocalDate appliedBefore,
        LocalDate responseAfter,
        LocalDate responseBefore,

        @ValidTimezone
        String timezone) {
}
