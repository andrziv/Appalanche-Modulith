package com.appalanche.backend.applications.persistence.dao;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

@Table(name = "applications")
@Entity
public class JobApplication implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(nullable = false)
    private Long id;

    @Column(nullable = false, name = "requisition_id")
    private String requisitionId;

    @Column(nullable = false, name = "owner_account_id")
    private UUID ownerAccountId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String company;

    @Column(nullable = false)
    private Integer interest;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "status_id", nullable = false)
    private JobApplicationStatus status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "experience_id", nullable = false)
    private JobApplicationExperience experience;

    @Column(name = "date_applied")
    private Date appliedDate;

    @Column(name = "date_response")
    private Date responseDate;

    @CreationTimestamp
    @Column(updatable = false, name = "created_at")
    private Date createdAt;

    protected JobApplication() {
    }

    public JobApplication(String requisitionId, UUID ownerAccountId, String title, String company, Integer interest,
                          JobApplicationStatus status, JobApplicationExperience experience,
                          Date appliedDate, Date responseDate) {
        this.requisitionId = requisitionId;
        this.ownerAccountId = ownerAccountId;
        this.title = title;
        this.company = company;
        this.interest = interest;
        this.status = status;
        this.appliedDate = appliedDate;
        this.responseDate = responseDate;
        this.experience = experience;
    }

    @Override
    public String toString() {
        return String.format("JobApplication[id=%d, requisitionId='%s', ownerAccountId='%s', title='%s', " +
                        "company='%s', interest='%d', status='%s', experience='%s']",
                id, requisitionId, ownerAccountId, title, company, interest, status.getLabel(), experience.getLabel());
    }

    public Long getId() {
        return id;
    }

    public String getRequisitionId() {
        return requisitionId;
    }

    public void setRequisitionId(String requisitionId) {
        this.requisitionId = requisitionId;
    }

    public UUID getOwnerAccountId() {
        return ownerAccountId;
    }

    public void setOwnerAccountId(UUID ownerAccountId) {
        this.ownerAccountId = ownerAccountId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public Integer getInterest() {
        return interest;
    }

    public void setInterest(Integer interest) {
        this.interest = interest;
    }

    public JobApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(JobApplicationStatus status) {
        this.status = status;
    }

    public JobApplicationExperience getExperience() {
        return experience;
    }

    public void setExperience(JobApplicationExperience experience) {
        this.experience = experience;
    }

    public Date getAppliedDate() {
        return appliedDate;
    }

    public void setAppliedDate(Date appliedDate) {
        this.appliedDate = appliedDate;
    }

    public Date getResponseDate() {
        return responseDate;
    }

    public void setResponseDate(Date responseDate) {
        this.responseDate = responseDate;
    }

    public Date getCreatedAt() {
        return createdAt;
    }
}