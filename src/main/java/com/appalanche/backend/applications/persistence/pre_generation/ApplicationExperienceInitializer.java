package com.appalanche.backend.applications.persistence.pre_generation;

import com.appalanche.backend.applications.config.JobApplicationExperienceProperties;
import com.appalanche.backend.applications.persistence.JobApplicationExperienceRepository;
import com.appalanche.backend.applications.persistence.dao.JobApplicationExperience;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ApplicationExperienceInitializer implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationExperienceInitializer.class);

    private final JobApplicationExperienceRepository repository;
    private final JobApplicationExperienceProperties properties;

    public ApplicationExperienceInitializer(JobApplicationExperienceRepository repository,
                                            JobApplicationExperienceProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Override
    public void run(String... args) {
        if (repository.count() == 0) {
            for (JobApplicationExperienceProperties.ExperienceConfig config : properties.getLevels()) {
                var experienceLevel = new JobApplicationExperience(config.getCode(), config.getLabel(), config.getDescription());
                repository.save(experienceLevel);
            }

            logger.debug("Database initialized with experience levels from config.");
        }
    }
}
