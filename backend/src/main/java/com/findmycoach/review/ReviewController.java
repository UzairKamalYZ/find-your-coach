package com.findmycoach.review;

import com.findmycoach.review.dto.CreateReviewRequest;
import com.findmycoach.review.dto.ReviewResponse;
import com.findmycoach.review.dto.UpdateReviewRequest;
import com.findmycoach.security.AuthUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Reviews")
@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody CreateReviewRequest req) {
        return service.create(me, req);
    }

    @PatchMapping("/{id}")
    public ReviewResponse update(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id,
                                 @Valid @RequestBody UpdateReviewRequest req) {
        return service.update(me, id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        service.delete(me, id);
    }
}
