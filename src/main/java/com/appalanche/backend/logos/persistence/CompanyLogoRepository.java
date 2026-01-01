package com.appalanche.backend.logos.persistence;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyLogoRepository extends CrudRepository<CompanyLogo, Long> {
    Optional<CompanyLogo> findByBrandAndTopLevelDomain(String brand, String topLevelDomain);
}
