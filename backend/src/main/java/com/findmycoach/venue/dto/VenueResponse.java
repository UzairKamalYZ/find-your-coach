package com.findmycoach.venue.dto;

import com.findmycoach.venue.Venue;

public record VenueResponse(
    String id, String name, String type, String sport,
    String city, double pricePerHour, double rating, String imageUrl
) {
    public static VenueResponse from(Venue v) {
        return new VenueResponse(v.getId(), v.getName(), v.getType(), v.getSport(),
            v.getCity(), v.getPricePerHour() / 100.0, v.getRating().doubleValue(), v.getImageUrl());
    }
}
