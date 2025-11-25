package com.jobhunt.backend.JobHunt_Modulith;

import com.jobhunt.backend.JobHunt_Modulith.applications.config.JobApplicationStatusProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JobApplicationStatusProperties.class)
public class JobHuntModulithApplication {
//test
    public static void main(String[] args) {
        SpringApplication.run(JobHuntModulithApplication.class, args);
    }

}
