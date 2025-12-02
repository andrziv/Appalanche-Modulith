package com.appalanche.backend.applications.persistence;

import com.appalanche.backend.applications.persistence.dao.JobApplicationExperience;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface JobApplicationExperienceRepository extends CrudRepository<JobApplicationExperience, Long> {
    Optional<JobApplicationExperience> findByCode(String code);
}
