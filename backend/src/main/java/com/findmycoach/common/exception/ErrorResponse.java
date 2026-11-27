package com.findmycoach.common.exception;

import java.time.Instant;
import java.util.Map;

/** Shape of every error body returned by the API. */
public record ErrorResponse(int status, String code, String error, Map<String, String> details, String path, Instant timestamp) {
}
