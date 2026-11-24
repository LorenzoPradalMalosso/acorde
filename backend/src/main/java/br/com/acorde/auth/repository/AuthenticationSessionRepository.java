package br.com.acorde.auth.repository;

import br.com.acorde.auth.model.AuthenticationSession;
import br.com.acorde.organizacoes.model.MemberRole;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthenticationSessionRepository extends JpaRepository<AuthenticationSession, UUID> {
    @Query("""
            select count(s) from AuthenticationSession s
            join s.member m join m.user u join m.organization o
            where s.id = :sessionId and s.revokedAt is null and s.expiresAt > :now
              and m.active = true and u.active = true and o.id = :organizationId
              and m.id = :memberId and u.id = :userId and m.role = :role
            """)
    long countActiveSession(@Param("sessionId") UUID sessionId,
                            @Param("organizationId") UUID organizationId,
                            @Param("memberId") UUID memberId,
                            @Param("userId") UUID userId,
                            @Param("role") MemberRole role,
                            @Param("now") Instant now);

    @Modifying
    @Query("update AuthenticationSession s set s.revokedAt = :now where s.member.id = :memberId and s.revokedAt is null")
    int revokeAllForMember(@Param("memberId") UUID memberId, @Param("now") Instant now);

    @Modifying
    @Query("delete from AuthenticationSession s where s.expiresAt < :now or s.revokedAt is not null")
    int deleteExpiredOrRevoked(@Param("now") Instant now);
}
