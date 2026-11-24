package br.com.acorde.auth.service;

import br.com.acorde.auth.repository.AuthenticationSessionRepository;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuthenticationSessionMaintenance {
    private final AuthenticationSessionRepository sessions;

    public AuthenticationSessionMaintenance(AuthenticationSessionRepository sessions) {
        this.sessions = sessions;
    }

    @Scheduled(fixedDelay = 3_600_000, initialDelay = 3_600_000)
    @Transactional
    public void removeExpiredSessions() {
        sessions.deleteExpiredOrRevoked(Instant.now());
    }
}
