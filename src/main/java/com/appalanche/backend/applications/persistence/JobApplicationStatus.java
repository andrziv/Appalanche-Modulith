package com.appalanche.backend.applications.persistence;

import jakarta.persistence.*;

import java.io.Serializable;

@Table(name = "application_statuses", uniqueConstraints = {@UniqueConstraint(columnNames = {"label", "round"})})
@Entity
public class JobApplicationStatus implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(nullable = false)
    private Long id;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private Integer round;

    private String colour;
    private String textColour;

    protected JobApplicationStatus() {
    }

    public JobApplicationStatus(String code, String label, Integer round, String colour, String textColour) {
        this.code = code;
        this.label = label;
        this.round = round;
        this.colour = colour;
        this.textColour = textColour;
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

    public Integer getRound() {
        return round;
    }

    public String getColour() {
        return colour;
    }

    public String getTextColour() {
        return textColour;
    }
}
