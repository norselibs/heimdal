package io.norselibs.heimdal.predicate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InNode implements PredicateNode {
    private final String field;
    private final List<String> values;

    public InNode(String field, List<String> values) {
        this.field = field;
        this.values = values;
    }

    @Override
    public boolean evaluate(Map<String, String> values) {
        return this.values.contains(values.getOrDefault(field, ""));
    }

    @Override
    public Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("op", "in");
        m.put("field", field);
        m.put("values", values);
        return m;
    }
}
