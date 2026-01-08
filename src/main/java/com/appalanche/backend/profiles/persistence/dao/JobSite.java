package com.appalanche.backend.profiles.persistence.dao;

import jakarta.persistence.*;

import java.util.UUID;

@Table(name = "job_sites")
@Entity
public class JobSite {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(nullable = false, updatable = false)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false, name = "site_id")
    private UUID siteId;

    @Column(nullable = false, updatable = false, unique = true)
    private String url;

    @Column(nullable = false, updatable = false)
    private String name;

    protected JobSite() {
    }

    public JobSite(UUID siteId, String url, String name) {
        this.siteId = siteId;
        this.url = url;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public UUID getSiteId() {
        return siteId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
