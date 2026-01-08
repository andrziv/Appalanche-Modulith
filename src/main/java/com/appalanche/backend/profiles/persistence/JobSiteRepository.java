package com.appalanche.backend.profiles.persistence;

import com.appalanche.backend.profiles.persistence.dao.JobSite;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobSiteRepository extends CrudRepository<JobSite, Long> {
    Optional<JobSite> findByUrl(String url);
}
