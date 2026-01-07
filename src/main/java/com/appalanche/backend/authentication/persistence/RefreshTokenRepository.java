package com.appalanche.backend.authentication.persistence;

import com.appalanche.backend.authentication.persistence.dao.RefreshToken;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends CrudRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findAllByAccount_AccountIdOrderByLastUsedAsc(UUID accountId);
}
