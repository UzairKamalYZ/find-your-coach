package com.findmycoach.audit;

import com.findmycoach.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository repo;

    @Transactional
    public void record(AuthUser actor, String action, String targetType, Object targetId, String details) {
        AuditLog log = new AuditLog();
        if (actor != null) {
            log.setActorId(actor.id());
            log.setActorEmail(actor.email());
        }
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId == null ? null : targetId.toString());
        log.setDetails(details == null ? null : details.length() > 1000 ? details.substring(0, 1000) : details);
        repo.save(log);
    }
}
