package com.findmycoach.common.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class Pagination {
    public static final int MAX_SIZE = 100;

    private Pagination() {
    }

    public static Pageable of(int page, int size, Sort sort) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(MAX_SIZE, Math.max(1, size));
        return PageRequest.of(safePage, safeSize, sort);
    }
}
