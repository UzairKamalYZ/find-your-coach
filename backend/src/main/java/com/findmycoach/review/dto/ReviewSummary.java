package com.findmycoach.review.dto;

import java.util.Map;

/** breakdown: stars (1-5) to number of written reviews on the platform. */
public record ReviewSummary(double rating, int reviews, Map<Integer, Long> breakdown) {
}
