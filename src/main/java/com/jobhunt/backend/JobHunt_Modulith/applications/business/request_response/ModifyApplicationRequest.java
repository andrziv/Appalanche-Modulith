package com.jobhunt.backend.JobHunt_Modulith.applications.business.request_response;

import org.springframework.lang.NonNull;

import java.util.Date;

public record ModifyApplicationRequest(
        String requisitionId,
        String title,
        String company,
        Integer interest,
        String statusCode,
        Date appliedDate,
        Date responseDate) {

    @NonNull
    @Override
    public String toString() {
        return String.format("ModifyApplicationRequest[requisitionId='%s', title='%s', company='%s', " +
                        "interest='%d', statusCode='%s', appliedDate='%s', responseDate='%s']",
                requisitionId, title, company, interest, statusCode, appliedDate, responseDate);
    }
}
