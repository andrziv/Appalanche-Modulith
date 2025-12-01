package com.appalanche.backend.applications.persistence.pre_generation;

import com.appalanche.backend.applications.config.JobApplicationStatusProperties;
import com.appalanche.backend.applications.persistence.JobApplicationStatus;
import com.appalanche.backend.applications.persistence.JobApplicationStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;

@Component
public class ApplicationStatusInitializer implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationStatusInitializer.class);

    private final JobApplicationStatusRepository repository;
    private final JobApplicationStatusProperties properties;

    public ApplicationStatusInitializer(JobApplicationStatusRepository repository,
                                        JobApplicationStatusProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Override
    public void run(String... args) {
        if (repository.count() == 0) {
            for (JobApplicationStatusProperties.StatusConfig config : properties.getStatuses()) {
                for (int round = 1; round <= config.getMaxRounds(); round++) {
                    var statusCode = MessageFormat.format(config.getCode(), round);
                    JobApplicationStatus status =
                            new JobApplicationStatus(statusCode, config.getLabel(), round, config.getColour(), config.getTextColour());
                    repository.save(status);
                }
            }

            logger.debug("Database initialized with statuses from config.");
        }
    }
}