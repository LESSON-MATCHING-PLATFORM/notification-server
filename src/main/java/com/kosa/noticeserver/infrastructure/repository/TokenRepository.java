package com.kosa.noticeserver.infrastructure.repository;

import com.kosa.noticeserver.domain.model.TokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TokenRepository extends JpaRepository<TokenEntity, Long> {
    @Query("SELECT t.token FROM TokenEntity t WHERE t.userId = :userId")
    List<String> findAllTokensByUserId(String userId);

    List<TokenEntity> findAllByUserIdIn(List<String> userIds);

    long deleteByTokenIn(Collection<String> tokens);
}
