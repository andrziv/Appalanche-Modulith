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
    private String jobPostingLink;
    private String description;
    private Instant appliedDate;
    private Instant responseDate;

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

    public JobApplicationModel withJobPostingLink(String jobPostingLink) {
        this.jobPostingLink = jobPostingLink;
        return this;
    }

    public JobApplicationModel withDescription(String description) {
        this.description = description;
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

    public String getJobPostingLink() {
        return jobPostingLink;
    }

    public String getDescription() {
        return description;
    }

    public Instant getAppliedDate() {
        return appliedDate;
    }

    public Instant getResponseDate() {
        return responseDate;
    }
}
