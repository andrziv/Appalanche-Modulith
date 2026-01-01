package com.appalanche.backend.logos.business;

import com.appalanche.backend.logos.persistence.CompanyLogo;
import com.appalanche.backend.logos.persistence.CompanyLogoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import static java.time.Instant.now;

@Service
@Transactional(readOnly = true)
public class CompanyLogoService {
    private static final Logger logger = LoggerFactory.getLogger(CompanyLogoService.class);

    private final CompanyLogoRepository logoRepository;
    private final RestClient restClient;

    @Value("${integration.logo-dev.token}")
    private String apiToken;

    @Value("${integration.logo-dev.image.size:128}")
    private int imageSize;

    @Value("${integration.logo-dev.image.format:jpeg}")
    private String imageFormat;

    public CompanyLogoService(CompanyLogoRepository logoRepository,
                              @Qualifier("logoClient") RestClient restClient) {
        this.logoRepository = logoRepository;
        this.restClient = restClient;
    }

    @Transactional
    public CompanyLogo getCompanyLogo(String brand, String tld) {
        logger.debug("Received FetchLogoRequest[brand='{}', tld='{}'] at GetCompanyLogo service method.", brand, tld);

        var dbImage = logoRepository.findByBrandAndTopLevelDomain(brand, tld);
        if (dbImage.isEmpty()) {
            return fetchAndCache(brand, tld);
        }

        var companyLogo = dbImage.get();
        if (isValid(companyLogo)) {
            return refetch(companyLogo);
        }

        return companyLogo;
    }

    @Transactional
    public CompanyLogo getCompanyLogo(String brand) {
        logger.debug("Received FetchLogoRequest[brand='{}'] at GetCompanyLogo service method.", brand);

        var dbImage = logoRepository.findByBrandAndTopLevelDomain(brand, "");
        if (dbImage.isEmpty()) {
            return fetchAndCache(brand);
        }

        var companyLogo = dbImage.get();
        if (isValid(companyLogo)) {
            return refetch(companyLogo);
        }

        return companyLogo;
    }

    private CompanyLogo fetchAndCache(String brand, String tld) {
        try {
            byte[] image = fetchImage(brand, tld);

            CompanyLogo logo = new CompanyLogo(brand, tld, image, imageSize, toImageContentType(imageFormat), now());
            return logoRepository.save(logo);
        } catch (Exception e) {
            throw new RuntimeException("Could not fetch logo", e);
        }
    }

    private CompanyLogo fetchAndCache(String brand) {
        try {
            byte[] image = fetchImage(brand);

            CompanyLogo logo = new CompanyLogo(brand, "", image, imageSize, toImageContentType(imageFormat), now());
            return logoRepository.save(logo);
        } catch (Exception e) {
            throw new RuntimeException("Could not fetch logo", e);
        }
    }

    private CompanyLogo refetch(CompanyLogo logo) {
        try {
            byte[] image;
            if (logo.getTopLevelDomain().isBlank()) {
                image = fetchImage(logo.getBrand());
            } else {
                image = fetchImage(logo.getBrand(), logo.getTopLevelDomain());
            }

            logo.setImageBytes(image);
            logo.setSize(imageSize);
            logo.setContentType(toImageContentType(imageFormat));
            logo.setLastUpdated(now());

            return logoRepository.save(logo);
        } catch (Exception e) {
            if (logo.getId() != null) {
                return logo;
            }

            throw e;
        }
    }

    private byte[] fetchImage(String brand, String tld) throws RestClientResponseException {
        return restClient.get()
                         .uri(uriBuilder -> uriBuilder
                                 .path("/{domain}")
                                 .queryParam("token", apiToken)
                                 .queryParam("size", imageSize)
                                 .queryParam("format", imageFormat)
                                 .build(toDomain(brand, tld)))
                         .retrieve()
                         .body(byte[].class);
    }

    private byte[] fetchImage(String brand) throws RestClientResponseException {
        return restClient.get()
                         .uri(uriBuilder -> uriBuilder
                                 .path("/name/{brand}")
                                 .queryParam("token", apiToken)
                                 .queryParam("size", imageSize)
                                 .queryParam("format", imageFormat)
                                 .build(brand))
                         .retrieve()
                         .body(byte[].class);
    }

    private static String toImageContentType(String type) {
        if (type == null) {
            return null;
        }

        if (type.equals("jpg")) {
            return "image/jpeg";
        }

        return "image/" + type;
    }

    private boolean isValid(CompanyLogo logo) {
        return !logo.getContentType().equals(toImageContentType(imageFormat)) || !logo.getSize().equals(imageSize);
    }

    private static String toDomain(String brand, String tld) {
        return brand + '.' + tld;
    }
}
