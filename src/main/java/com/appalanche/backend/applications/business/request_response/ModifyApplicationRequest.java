package com.appalanche.backend.applications.business.request_response;

import jakarta.validation.constraints.PastOrPresent;
import org.hibernate.validator.constraints.Range;
import org.springframework.lang.NonNull;

import java.time.Instant;

public record ModifyApplicationRequest(
        String requisitionId,
        String title,
        String company,

        @Range(min = 1, max = 10, message = "Interest rating must be between 1 and 10, inclusive")
        Integer interest,

        String statusCode,
        String experienceLevelCode,

        @PastOrPresent(message = "Application cannot be from the future")
        Instant appliedDate,

        @PastOrPresent(message = "Response cannot be from the future")
        Instant responseDate) {

    @NonNull
    @Override
    public String toString() {
        return String.format("ModifyApplicationRequest[requisitionId='%s', title='%s', company='%s', " +
                        "interest='%d', statusCode='%s', experienceLevelCode='%s', appliedDate='%s', responseDate='%s']",
                requisitionId, title, company, interest, statusCode, experienceLevelCode, appliedDate, responseDate);
    }
}
