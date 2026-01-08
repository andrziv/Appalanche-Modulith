package com.appalanche.backend.profiles.persistence.dao;

import jakarta.persistence.*;

@Table(name = "job_sites")
@Entity
public class JobSite {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(nullable = false, updatable = false)
    private Long id;

    @Column(nullable = false, updatable = false, unique = true)
    private String url;

    @Column(nullable = false, updatable = false)
    private String name;

    protected JobSite() {
    }

    public JobSite(String url, String name) {
        this.url = url;
        this.name = name;
    }

    public Long getId() {
        return id;
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
