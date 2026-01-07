package com.appalanche.backend.applications.persistence;

import com.appalanche.backend.applications.persistence.dao.JobApplication;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationRepository extends ListCrudRepository<JobApplication, Long>, JpaSpecificationExecutor<JobApplication> {
    Optional<JobApplication> findByApplicationId(UUID applicationId);

    Optional<JobApplication> findByApplicationIdAndOwnerAccountId(UUID applicationId, UUID ownerAccountId);
}
