package com.appalanche.backend.applications.business.request_response;

import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.List;

public record SearchApplicationRequest(
        String search,
        List<String> statusCodes,
        List<String> experienceLevelCodes,
        List<String> interestCriteria,

        @DateTimeFormat(pattern = "yyyy-MM-dd")
        Date appliedAfter,

        @DateTimeFormat(pattern = "yyyy-MM-dd")
        Date appliedBefore,

        @DateTimeFormat(pattern = "yyyy-MM-dd")
        Date responseAfter,

        @DateTimeFormat(pattern = "yyyy-MM-dd")
        Date responseBefore) {
}
