package com.findmycoach.trainer;

import com.findmycoach.trainer.dto.DayAvailability;
import com.findmycoach.trainer.dto.TrainerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/trainers")
@RequiredArgsConstructor
public class TrainerController {

    private final TrainerService trainerService;

    @GetMapping
    public Page<TrainerResponse> list(
        @RequestParam(required = false) String sport,
        @RequestParam(required = false) String city,
        @RequestParam(required = false) String q,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        return trainerService.search(sport, city, q, PageRequest.of(page, Math.min(size, 100)));
    }

    @GetMapping("/{id}")
    public TrainerResponse getById(@PathVariable String id) {
        return trainerService.getById(id);
    }

    @GetMapping("/{id}/availability")
    public List<DayAvailability> availability(
        @PathVariable String id,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return trainerService.getAvailability(id, from, to);
    }
}
