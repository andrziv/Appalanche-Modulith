package com.appalanche.backend;

import com.appalanche.backend.applications.config.JobApplicationStatusProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JobApplicationStatusProperties.class)
public class AppalancheModulithApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppalancheModulithApplication.class, args);
    }

}
