package com.findmycoach.notification;

import com.findmycoach.common.web.PageResponse;
import com.findmycoach.security.AuthUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Tag(name = "Notifications")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService service;

    @GetMapping
    public PageResponse<NotificationService.NotificationResponse> list(@AuthenticationPrincipal AuthUser me,
                                                                       @RequestParam(defaultValue = "false") boolean unreadOnly,
                                                                       @RequestParam(defaultValue = "0") int page,
                                                                       @RequestParam(defaultValue = "20") int size) {
        return service.list(me.id(), unreadOnly, page, size);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal AuthUser me) {
        return Map.of("count", service.unreadCount(me.id()));
    }

    @PostMapping("/{id}/read")
    public NotificationService.NotificationResponse markRead(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        return service.markRead(me.id(), id);
    }

    @PostMapping("/read-all")
    public Map<String, Integer> markAllRead(@AuthenticationPrincipal AuthUser me) {
        return Map.of("updated", service.markAllRead(me.id()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        service.delete(me.id(), id);
    }
}
