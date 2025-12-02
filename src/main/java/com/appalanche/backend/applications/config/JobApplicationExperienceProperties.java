package com.appalanche.backend.applications.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "appalanche.experience")
public class JobApplicationExperienceProperties {
    private List<ExperienceConfig> levels;

    public List<ExperienceConfig> getLevels() {
        return levels;
    }

    public void setLevels(List<ExperienceConfig> levels) {
        this.levels = levels;
    }

    public static class ExperienceConfig {
        private String code;
        private String label;
        private String description;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
