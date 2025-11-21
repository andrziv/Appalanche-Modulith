package com.jobhunt.backend.JobHunt_Modulith.applications.persistence;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobApplicationStatusRepository extends CrudRepository<JobApplicationStatus, Long> {
    Optional<JobApplicationStatus> findByLabelAndRound(String label, Integer round);

    Optional<JobApplicationStatus> findByCode(String code);
}