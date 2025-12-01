package com.appalanche.backend.applications.persistence;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends ListCrudRepository<JobApplication, Long> {
    List<JobApplication> findByOwnerEmail(String email);

    Optional<JobApplication> findByIdAndOwnerEmail(long id, String email);
}
