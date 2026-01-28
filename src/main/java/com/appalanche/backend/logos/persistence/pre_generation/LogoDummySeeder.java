package com.appalanche.backend.logos.persistence.pre_generation;

import com.appalanche.backend.logos.persistence.CompanyLogo;
import com.appalanche.backend.logos.persistence.CompanyLogoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import static java.time.Instant.now;

@Component
@ConditionalOnProperty(name = "appalanche.dummy_seeding.logos", havingValue = "true")
public class LogoDummySeeder implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(LogoDummySeeder.class);

    private final CompanyLogoRepository repository;

    @Value("classpath:images/umbrella.jpg")
    private Resource customImage;

    public LogoDummySeeder(CompanyLogoRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) throws Exception {
        String brand = "Umbrella Corporation";
        String topLevelDomain = "";

        if (repository.findByBrandAndTopLevelDomain(brand, topLevelDomain).isEmpty()) {
            byte[] bytes = customImage.getContentAsByteArray();

            CompanyLogo logo = new CompanyLogo(brand, topLevelDomain, bytes, 128, "image/jpeg", now());

            repository.save(logo);

            logger.debug("Custom logo for {} has been inserted.", brand);
        }
    }
}
