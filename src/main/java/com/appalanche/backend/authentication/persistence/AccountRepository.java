package com.appalanche.backend.authentication.persistence;

import com.appalanche.backend.authentication.persistence.dao.Account;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends CrudRepository<Account, Long> {
    Optional<Account> findByEmail(String email);

    Optional<Account> findByAccountId(UUID accountId);
}
