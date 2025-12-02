package com.appalanche.backend.applications.persistence.dao;

import jakarta.persistence.*;

import java.io.Serializable;

@Table(name = "application_experience_levels")
@Entity
public class JobApplicationExperience implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(nullable = false)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private String description;

    protected JobApplicationExperience() {
    }

    public JobApplicationExperience(String code, String label, String description) {
        this.code = code;
        this.label = label;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}
