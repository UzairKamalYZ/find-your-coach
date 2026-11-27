package com.findmycoach.notification;

import com.findmycoach.common.exception.ApiException;
import com.findmycoach.common.web.PageResponse;
import com.findmycoach.common.web.Pagination;
import com.findmycoach.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository repo;

    @Transactional
    public void notify(User user, NotificationType type, String title, String message, String link) {
        if (user == null) return;
        Notification n = new Notification();
        n.setUser(user);
        n.setType(type);
        n.setTitle(title.length() > 150 ? title.substring(0, 150) : title);
        n.setMessage(message != null && message.length() > 500 ? message.substring(0, 500) : message);
        n.setLink(link);
        repo.save(n);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(UUID userId, boolean unreadOnly, int page, int size) {
        var pageable = Pagination.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var result = unreadOnly ? repo.findByUserIdAndReadFalse(userId, pageable) : repo.findByUserId(userId, pageable);
        return PageResponse.of(result, NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return repo.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markRead(UUID userId, UUID id) {
        Notification n = repo.findById(id).orElseThrow(() -> ApiException.notFound("Notification not found"));
        if (!n.getUser().getId().equals(userId)) throw ApiException.notFound("Notification not found");
        n.setRead(true);
        return NotificationResponse.from(n);
    }

    @Transactional
    public int markAllRead(UUID userId) {
        return repo.markAllRead(userId);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Notification n = repo.findById(id).orElseThrow(() -> ApiException.notFound("Notification not found"));
        if (!n.getUser().getId().equals(userId)) throw ApiException.notFound("Notification not found");
        repo.delete(n);
    }

    public record NotificationResponse(UUID id, NotificationType type, String title, String message, String link,
                                       boolean read, Instant createdAt) {
        static NotificationResponse from(Notification n) {
            return new NotificationResponse(n.getId(), n.getType(), n.getTitle(), n.getMessage(), n.getLink(),
                    n.isRead(), n.getCreatedAt());
        }
    }
}
