package io.norselibs.heimdal.predicate;

import java.util.Map;

/**
 * Evaluates {@code field <= constant} using lexicographic string comparison.
 * Works correctly for ISO 8601 date strings. For numeric fields use a
 * plain {@link io.norselibs.heimdal.Validator} lambda that parses the value.
 */
public class LteNode implements PredicateNode {
    private final String field;
    private final String value;

    public LteNode(String field, String value) {
        this.field = field;
        this.value = value;
    }

    @Override
    public boolean evaluate(Map<String, String> values) {
        String raw = values.getOrDefault(field, "");
        return raw.isEmpty() || raw.compareTo(value) <= 0;
    }

    @Override
    public Map<String, Object> toJson() {
        return Map.of("op", "lte", "field", field, "value", value);
    }
}
