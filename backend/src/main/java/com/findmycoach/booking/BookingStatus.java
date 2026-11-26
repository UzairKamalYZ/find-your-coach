package com.findmycoach.booking;

public enum BookingStatus {
    /** Booked and expected to happen. */
    CONFIRMED,
    /** Cancelled by the trainee, the coach or an admin. */
    CANCELLED,
    /** Coach marked the session as delivered. */
    COMPLETED,
    /** Coach marked the trainee as not attending. */
    NO_SHOW;

    public boolean holdsSlot() {
        return this == CONFIRMED || this == COMPLETED || this == NO_SHOW;
    }
}
