package com.appalanche.backend.profiles.business;

import jakarta.annotation.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Map;

public class UrlHelper {
    public static String cleanupUrl(String urlString) {
        if (urlString == null || urlString.isBlank()) {
            return "Unknown Site";
        }

        var urlChunks = urlString.split("://");
        var recombobulatedUrl = urlString;

        if (urlChunks.length > 2) {
            recombobulatedUrl = urlChunks[urlChunks.length - 2] + "://" + urlChunks[urlChunks.length - 1];
        }

        try {
            var protocol = new URI(recombobulatedUrl).getScheme();
            if (protocol == null) {
                return "https://" + recombobulatedUrl;
            } else if (!protocol.equals("https")) {
                return recombobulatedUrl.replace(protocol, "https");
            }

            return recombobulatedUrl;
        } catch (URISyntaxException e) {
            return "Unknown Site";
        }
    }

    public static String extractSiteName(String urlString, @Nullable Map<String, String> hostMappings) {
        if (urlString == null || urlString.isBlank()) {
            return "Unknown Site";
        }

        try {
            if (!urlString.startsWith("http")) {
                urlString = "https://" + urlString;
            }

            var uri = new URI(urlString);

            String host = uri.getHost();
            if (host == null) {
                return "Unknown Site";
            }

            if (host.startsWith("www.")) {
                host = host.substring(4);
            }

            int lastDot = host.lastIndexOf('.');
            if (lastDot > 0) {
                String withoutTld = host.substring(0, lastDot);

                int secondLastDot = withoutTld.lastIndexOf('.');
                if (secondLastDot > -1) {
                    withoutTld = withoutTld.substring(secondLastDot + 1);
                }

                if (hostMappings != null && hostMappings.containsKey(withoutTld.toLowerCase())) {
                    return hostMappings.get(withoutTld.toLowerCase());
                }

                return capitalize(withoutTld);
            }

            return capitalize(host);

        } catch (URISyntaxException e) {
            return "Unknown Site";
        }
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        return str.substring(0, 1).toUpperCase(Locale.ROOT) + str.substring(1);
    }
}
