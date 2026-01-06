package com.appalanche.backend.applications.business.request_response;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.Range;
import org.hibernate.validator.constraints.URL;
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

        @URL(protocol = "https", message = "URL must be a valid HTTPS link")
        @Pattern(regexp = "[^<>\"\\s]*", message = "URL cannot contain spaces, quotes, or angle brackets (< >)")
        String jobPostingLink,

        @Size(max = 15000, message = "Description cannot exceed 15,000 characters")
        String description,

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
