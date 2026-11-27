package com.findmycoach.config;

import com.findmycoach.user.Role;
import com.findmycoach.venue.VenueType;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Locale;

/** Lets query parameters use any case for enums (?status=confirmed, ?type=Court, ?role=trainee). */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, VenueType.class, VenueType::from);
        registry.addConverter(String.class, Role.class, Role::from);
        registry.addConverterFactory(new CaseInsensitiveEnumFactory());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static class CaseInsensitiveEnumFactory implements ConverterFactory<String, Enum> {
        @Override
        public <T extends Enum> Converter<String, T> getConverter(Class<T> targetType) {
            return source -> {
                if (source == null || source.isBlank()) return null;
                return (T) Enum.valueOf(targetType, source.trim().toUpperCase(Locale.ROOT));
            };
        }
    }
}
