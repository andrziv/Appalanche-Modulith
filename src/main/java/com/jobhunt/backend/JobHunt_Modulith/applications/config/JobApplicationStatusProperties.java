package com.jobhunt.backend.JobHunt_Modulith.applications.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "jobhunt.status")
public class JobApplicationStatusProperties {
    private List<StatusConfig> statuses;

    public List<StatusConfig> getStatuses() {
        return statuses;
    }

    public void setStatuses(List<StatusConfig> statuses) {
        this.statuses = statuses;
    }

    public static class StatusConfig {
        private String code;
        private String label;
        private int maxRounds;
        private String colour;
        private String textColour;

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

        public int getMaxRounds() {
            return maxRounds;
        }

        public void setMaxRounds(int maxRounds) {
            this.maxRounds = maxRounds;
        }

        public String getColour() {
            return colour;
        }

        public void setColour(String colour) {
            this.colour = colour;
        }

        public String getTextColour() {
            return textColour;
        }

        public void setTextColour(String textColour) {
            this.textColour = textColour;
        }
    }
}
