package io.norselibs.heimdal.predicate;

import java.util.Map;

/** Evaluates {@code field1 < field2} using lexicographic string comparison. */
public class LtFieldNode implements PredicateNode {
    private final String field1;
    private final String field2;

    public LtFieldNode(String field1, String field2) {
        this.field1 = field1;
        this.field2 = field2;
    }

    @Override
    public boolean evaluate(Map<String, String> values) {
        String v1 = values.getOrDefault(field1, "");
        String v2 = values.getOrDefault(field2, "");
        return !v1.isEmpty() && !v2.isEmpty() && v1.compareTo(v2) < 0;
    }

    @Override
    public Map<String, Object> toJson() {
        return Map.of("op", "lt", "field", field1, "otherField", field2);
    }
}
