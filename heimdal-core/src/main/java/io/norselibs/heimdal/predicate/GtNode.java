package io.norselibs.heimdal.predicate;

import java.util.Map;

/** Evaluates {@code field > constant} using lexicographic string comparison. */
public class GtNode implements PredicateNode {
    private final String field;
    private final String value;

    public GtNode(String field, String value) {
        this.field = field;
        this.value = value;
    }

    @Override
    public boolean evaluate(Map<String, String> values) {
        String raw = values.getOrDefault(field, "");
        return raw.isEmpty() || raw.compareTo(value) > 0;
    }

    @Override
    public Map<String, Object> toJson() {
        return Map.of("op", "gt", "field", field, "value", value);
    }
}
