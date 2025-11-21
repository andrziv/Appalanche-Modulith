package com.jobhunt.backend.JobHunt_Modulith.applications.business.request_response;

import jakarta.validation.constraints.NotBlank;
import org.springframework.lang.NonNull;

import java.util.Date;

public record AddApplicationRequest(
        @NotBlank(message = "Requisition ID cannot be blank")
        String requisitionId,

        @NotBlank(message = "Job title cannot be blank")
        String title,

        @NotBlank(message = "Company name cannot be blank")
        String company,

        @NotBlank(message = "Interest rating cannot be blank")
        int interest,

        @NotBlank(message = "Status code cannot be blank")
        String statusCode,

        Date appliedDate,

        Date responseDate) {

    @NonNull
    @Override
    public String toString() {
        return String.format("AddApplicationRequest[requisitionId='%s', title='%s', company='%s', " +
                        "interest='%d', statusCode='%s', appliedDate='%s', responseDate='%s']",
                requisitionId, title, company, interest, statusCode, appliedDate, responseDate);
    }
}
