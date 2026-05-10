package io.norselibs.heimdal.predicate;

import java.util.LinkedHashMap;
import java.util.Map;

public class NeqNode implements PredicateNode {
    private final String field;
    private final String value;

    public NeqNode(String field, String value) {
        this.field = field;
        this.value = value;
    }

    @Override
    public boolean evaluate(Map<String, String> values) {
        return !value.equals(values.getOrDefault(field, ""));
    }

    @Override
    public Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("op", "neq");
        m.put("field", field);
        m.put("value", value);
        return m;
    }
}
