package com.findmycoach.common.jpa;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Stores a short list of strings as a single "|"-delimited column. */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {
    private static final String SEP = "|";

    @Override
    public String convertToDatabaseColumn(List<String> list) {
        if (list == null || list.isEmpty()) return "";
        return String.join(SEP, list.stream().map(s -> s.replace(SEP, "/").trim()).filter(s -> !s.isEmpty()).toList());
    }

    @Override
    public List<String> convertToEntityAttribute(String db) {
        if (db == null || db.isBlank()) return new ArrayList<>();
        return new ArrayList<>(Arrays.stream(db.split("\\|")).map(String::trim).filter(s -> !s.isEmpty()).toList());
    }
}
