package com.appalanche.backend.applications.persistence;

import com.appalanche.backend.applications.persistence.dao.JobApplicationStatus;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobApplicationStatusRepository extends CrudRepository<JobApplicationStatus, Long> {
    Optional<JobApplicationStatus> findByCode(String code);
}