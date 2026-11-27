package com.findmycoach.trainer;

/** Listing lifecycle: new coach listings wait for admin approval before they appear publicly. */
public enum TrainerStatus {
    PENDING, ACTIVE, SUSPENDED, REJECTED
}
