package com.findmycoach.sport;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Catalogue")
@RestController
@RequestMapping("/api/v1/sports")
@RequiredArgsConstructor
public class SportController {
    private final SportService service;

    @GetMapping
    public List<SportService.SportResponse> list() {
        return service.listActive();
    }
}
