package com.kosa.noticeserver.infrastructure.repository;

import com.kosa.noticeserver.domain.model.TokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<TokenEntity, Long> {
    @Query("SELECT t.token FROM TokenEntity t WHERE t.userId = :userId")
    List<String> findAllTokensByUserId(String userId);

    Optional<TokenEntity> findByToken(String token);

    List<TokenEntity> findAllByUserIdIn(List<String> userIds);

    @Transactional
    long deleteByTokenIn(Collection<String> tokens);

    @Modifying
    @Query(value = """
            INSERT INTO token_entity (token, user_id, created_at, updated_at)
            VALUES (:token, :userId, NOW(6), NOW(6))
            ON DUPLICATE KEY UPDATE
                user_id = VALUES(user_id),
                updated_at = NOW(6)
            """, nativeQuery = true)
    void upsertByToken(@Param("token") String token, @Param("userId") String userId);
}
