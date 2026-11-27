package com.findmycoach.venue.dto;

import com.findmycoach.venue.Venue;
import com.findmycoach.venue.VenueType;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

public record VenueResponse(String id, String name, VenueType type, String sport, String city, String address,
                            String description, BigDecimal pricePerHour, double rating, String image,
                            List<String> amenities, int capacity, LocalTime openTime, LocalTime closeTime,
                            boolean active) {
    public static VenueResponse from(Venue v) {
        return new VenueResponse(v.getId(), v.getName(), v.getType(), v.getSport(), v.getCity(), v.getAddress(),
                v.getDescription(), v.getPricePerHour(), v.getRating(), v.getImage(), List.copyOf(v.getAmenities()),
                v.getCapacity(), v.getOpenTime(), v.getCloseTime(), v.isActive());
    }
}
