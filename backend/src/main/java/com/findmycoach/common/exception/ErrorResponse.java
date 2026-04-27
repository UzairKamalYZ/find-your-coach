package com.findmycoach.common.exception;

import java.util.Map;

public record ErrorResponse(String error, Map<String, String> details) {
    public static ErrorResponse of(String error) {
        return new ErrorResponse(error, null);
    }
}
