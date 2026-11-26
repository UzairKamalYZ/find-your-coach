package com.findmycoach.sport;

import com.findmycoach.common.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SportService {
    private final SportRepository repo;

    @Transactional(readOnly = true)
    public List<SportResponse> listActive() {
        return repo.findByActiveTrueOrderBySortOrderAscNameAsc().stream().map(SportResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<SportResponse> listAll() {
        return repo.findAllByOrderBySortOrderAscNameAsc().stream().map(SportResponse::from).toList();
    }

    /** Returns the canonical sport name, or fails if it is unknown or inactive. */
    @Transactional(readOnly = true)
    public String requireActive(String name) {
        Sport s = repo.findByNameIgnoreCase(name == null ? "" : name.trim())
                .orElseThrow(() -> ApiException.badRequest("Unknown sport: " + name));
        if (!s.isActive()) throw ApiException.badRequest("Sport is not currently offered: " + s.getName());
        return s.getName();
    }

    @Transactional
    public SportResponse upsert(SportRequest req) {
        Sport s = repo.findByNameIgnoreCase(req.name().trim()).orElseGet(() -> {
            Sport n = new Sport();
            n.setName(req.name().trim());
            return n;
        });
        s.setEmoji(req.emoji());
        if (req.active() != null) s.setActive(req.active());
        if (req.sortOrder() != null) s.setSortOrder(req.sortOrder());
        return SportResponse.from(repo.save(s));
    }

    public record SportResponse(String name, String emoji, boolean active, int sortOrder) {
        static SportResponse from(Sport s) {
            return new SportResponse(s.getName(), s.getEmoji(), s.isActive(), s.getSortOrder());
        }
    }

    public record SportRequest(
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 50) String name,
            @jakarta.validation.constraints.Size(max = 16) String emoji,
            Boolean active,
            Integer sortOrder) {
    }
}
