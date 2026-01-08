package com.appalanche.backend.profiles.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "appalanche.known-hosts")
public class KnownJobSiteHostProperties {
    private List<KnownHostsConfig> hosts;

    public List<KnownHostsConfig> getHosts() {
        return hosts;
    }

    public void setHosts(List<KnownHostsConfig> hosts) {
        this.hosts = hosts;
    }

    public static class KnownHostsConfig {
        private String hostname;
        private String displayName;

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
    }
}
