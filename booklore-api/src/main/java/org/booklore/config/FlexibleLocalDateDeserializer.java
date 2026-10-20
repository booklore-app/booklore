package org.booklore.config;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class FlexibleLocalDateDeserializer extends StdDeserializer<LocalDate> {

    public FlexibleLocalDateDeserializer() {
        super(LocalDate.class);
    }

    @Override
    public LocalDate deserialize(JsonParser p, DeserializationContext ctx) throws JacksonException {
        String value = p.getText();
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        // ISO-8601 full date: "2020-01-15"
        try {
            return LocalDate.parse(trimmed);
        } catch (DateTimeParseException ignored) {}
        // Year-month: "2020-01" → first of that month
        if (trimmed.matches("\\d{4}-\\d{2}")) {
            try {
                return LocalDate.parse(trimmed + "-01");
            } catch (DateTimeParseException ignored) {}
        }
        // Year-only: "2020" → January 1
        if (trimmed.matches("\\d{4}")) {
            try {
                return LocalDate.of(Integer.parseInt(trimmed), 1, 1);
            } catch (NumberFormatException ignored) {}
        }
        return (LocalDate) ctx.handleWeirdStringValue(LocalDate.class, value,
                "Cannot parse LocalDate from '%s': expected yyyy-MM-dd, yyyy-MM, or yyyy", value);
    }
}
