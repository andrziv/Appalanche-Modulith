package com.appalanche.backend.logos.persistence;

import jakarta.persistence.*;

import java.time.Instant;

@Table(name = "company_logos", uniqueConstraints = {@UniqueConstraint(columnNames = {"brand", "top_level_domain"})})
@Entity
public class CompanyLogo {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(nullable = false, updatable = false)
    private Long id;

    @Column(nullable = false, updatable = false)
    private String brand;

    @Column(nullable = false, updatable = false, name = "top_level_domain")
    private String topLevelDomain;

    @Column(columnDefinition = "bytea", nullable = false, name = "image_bytes")
    private byte[] imageBytes;

    @Column(nullable = false)
    private Integer size;

    @Column(nullable = false, name = "content_type")
    private String contentType;

    @Column(nullable = false, name = "last_updated")
    private Instant lastUpdated;

    protected CompanyLogo() {
    }

    public CompanyLogo(String brand, String topLevelDomain, byte[] imageBytes, Integer size, String contentType, Instant lastUpdated) {
        this.brand = brand;
        this.topLevelDomain = topLevelDomain;
        this.imageBytes = imageBytes;
        this.size = size;
        this.contentType = contentType;
        this.lastUpdated = lastUpdated;
    }

    public Long getId() {
        return id;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getTopLevelDomain() {
        return topLevelDomain;
    }

    public void setTopLevelDomain(String topLevelDomain) {
        this.topLevelDomain = topLevelDomain;
    }

    public byte[] getImageBytes() {
        return imageBytes;
    }

    public void setImageBytes(byte[] imageBytes) {
        this.imageBytes = imageBytes;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
