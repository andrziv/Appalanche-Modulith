package com.appalanche.backend.profiles.persistence.dao;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Table(name = "account_profiles")
@Entity
public class AccountProfile implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(nullable = false, updatable = false)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false, name = "account_id")
    private UUID accountId;

    @Column(nullable = false, name = "first_name")
    private String firstName;

    @Column(nullable = false, name = "last_name")
    private String lastName;

    @Column(name = "linkedin_profile")
    private String linkedInProfile;

    @Column(name = "github_profile")
    private String gitHubProfile;

    @Column(name = "portfolio_site")
    private String portfolioSite;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "user_profile_sites", joinColumns = @JoinColumn(name = "account_id"), inverseJoinColumns = @JoinColumn(name = "site_id"))
    private final List<JobSite> jobSites = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false, name = "created_at")
    private Instant createdAt;

    protected AccountProfile() {
    }

    public AccountProfile(UUID accountId, String firstName, String lastName,
                          String linkedInProfile, String gitHubProfile, String portfolioSite) {
        this.accountId = accountId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.linkedInProfile = linkedInProfile;
        this.gitHubProfile = gitHubProfile;
        this.portfolioSite = portfolioSite;
    }

    @Override
    public String toString() {
        return String.format("AccountProfile[id=%d, accountId='%s', firstname='%s', lastName='%s']",
                id, accountId.toString(), firstName, lastName);
    }

    public Long getId() {
        return id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getLinkedInProfile() {
        return linkedInProfile;
    }

    public void setLinkedInProfile(String linkedInProfile) {
        this.linkedInProfile = linkedInProfile;
    }

    public String getGitHubProfile() {
        return gitHubProfile;
    }

    public void setGitHubProfile(String gitHubProfile) {
        this.gitHubProfile = gitHubProfile;
    }

    public String getPortfolioSite() {
        return portfolioSite;
    }

    public void setPortfolioSite(String portfolioSite) {
        this.portfolioSite = portfolioSite;
    }

    public List<JobSite> getJobSites() {
        return new ArrayList<>(jobSites);
    }

    public void addJobSite(JobSite site) {
        if (!jobSites.contains(site)) {
            jobSites.add(site);
        }
    }

    public void removeJobSite(JobSite site) {
        jobSites.remove(site);
    }
}
