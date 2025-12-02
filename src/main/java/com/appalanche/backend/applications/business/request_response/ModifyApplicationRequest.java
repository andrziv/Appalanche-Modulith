package com.appalanche.backend.applications.business.request_response;

import org.hibernate.validator.constraints.Range;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.NonNull;

import java.util.Date;

public record ModifyApplicationRequest(
        String requisitionId,
        String title,
        String company,

        @Range(min = 1, max = 10, message = "Interest rating must be between 1 and 10, inclusive")
        Integer interest,

        String statusCode,
        String experienceLevelCode,

        @DateTimeFormat(pattern = "yyyy-MM-dd")
        Date appliedDate,

        @DateTimeFormat(pattern = "yyyy-MM-dd")
        Date responseDate) {

    @NonNull
    @Override
    public String toString() {
        return String.format("ModifyApplicationRequest[requisitionId='%s', title='%s', company='%s', " +
                        "interest='%d', statusCode='%s', experienceLevelCode='%s', appliedDate='%s', responseDate='%s']",
                requisitionId, title, company, interest, statusCode, experienceLevelCode, appliedDate, responseDate);
    }
}
