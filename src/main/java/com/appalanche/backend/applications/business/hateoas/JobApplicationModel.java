package com.appalanche.backend.applications.business.hateoas;

import com.appalanche.backend.applications.persistence.dao.JobApplicationExperience;
import com.appalanche.backend.applications.persistence.dao.JobApplicationStatus;
import org.springframework.hateoas.RepresentationModel;

import java.time.Instant;
import java.util.UUID;

public class JobApplicationModel extends RepresentationModel<JobApplicationModel> {
    private Long id;
    private UUID applicationId;
    private String requisitionId;
    private String title;
    private String company;
    private Integer interest;
    private JobApplicationStatus status;
    private JobApplicationExperience experience;
    private Instant appliedDate;
    private Instant responseDate;
    private Instant createdAt;

    public JobApplicationModel withId(Long id) {
        this.id = id;
        return this;
    }

    public JobApplicationModel withApplicationId(UUID applicationId) {
        this.applicationId = applicationId;
        return this;
    }

    public JobApplicationModel withRequisitionId(String requisitionId) {
        this.requisitionId = requisitionId;
        return this;
    }

    public JobApplicationModel withTitle(String title) {
        this.title = title;
        return this;
    }

    public JobApplicationModel withCompany(String company) {
        this.company = company;
        return this;
    }

    public JobApplicationModel withInterest(Integer interest) {
        this.interest = interest;
        return this;
    }

    public JobApplicationModel withStatus(JobApplicationStatus status) {
        this.status = status;
        return this;
    }

    public JobApplicationModel withExperience(JobApplicationExperience experience) {
        this.experience = experience;
        return this;
    }

    public JobApplicationModel withAppliedDate(Instant appliedDate) {
        this.appliedDate = appliedDate;
        return this;
    }

    public JobApplicationModel withResponseDate(Instant responseDate) {
        this.responseDate = responseDate;
        return this;
    }

    public JobApplicationModel withCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Long getId() {
        return id;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public String getRequisitionId() {
        return requisitionId;
    }

    public String getTitle() {
        return title;
    }

    public String getCompany() {
        return company;
    }

    public Integer getInterest() {
        return interest;
    }

    public JobApplicationStatus getStatus() {
        return status;
    }

    public JobApplicationExperience getExperience() {
        return experience;
    }

    public Instant getAppliedDate() {
        return appliedDate;
    }

    public Instant getResponseDate() {
        return responseDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
