package com.appalanche.backend.profiles.persistence;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountProfileRepository extends CrudRepository<AccountProfile, Long> {
    Optional<AccountProfile> findByAccountId(UUID id);
}
