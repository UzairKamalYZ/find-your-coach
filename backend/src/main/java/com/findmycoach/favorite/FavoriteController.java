package com.findmycoach.favorite;

import com.findmycoach.security.AuthUser;
import com.findmycoach.trainer.dto.TrainerResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "My account")
@RestController
@RequestMapping("/api/v1/me/favorites")
@RequiredArgsConstructor
public class FavoriteController {
    private final FavoriteService service;

    @GetMapping
    public List<TrainerResponse> list(@AuthenticationPrincipal AuthUser me) {
        return service.list(me.id());
    }

    @GetMapping("/ids")
    public List<String> ids(@AuthenticationPrincipal AuthUser me) {
        return service.ids(me.id());
    }

    @PutMapping("/{trainerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void add(@AuthenticationPrincipal AuthUser me, @PathVariable String trainerId) {
        service.add(me.id(), trainerId);
    }

    @DeleteMapping("/{trainerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal AuthUser me, @PathVariable String trainerId) {
        service.remove(me.id(), trainerId);
    }
}
