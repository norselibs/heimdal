package io.norselibs.heimdal.definition;

import io.norselibs.heimdal.predicate.PredicateNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SectionDefinition implements ItemDefinition {
    private final String id;
    private final String label;        // null = no visible header
    private final PredicateNode visibleWhen;
    private final List<FieldDefinition> fields;

    public SectionDefinition(String id, String label, PredicateNode visibleWhen, List<FieldDefinition> fields) {
        this.id = id;
        this.label = label;
        this.visibleWhen = visibleWhen;
        this.fields = fields;
    }

    /** Backwards-compatible no-label constructor. */
    public SectionDefinition(String id, PredicateNode visibleWhen, List<FieldDefinition> fields) {
        this(id, null, visibleWhen, fields);
    }

    public String getId()              { return id; }
    public String getLabel()           { return label; }
    public PredicateNode getVisibleWhen() { return visibleWhen; }
    public List<FieldDefinition> getFields() { return fields; }

    @Override
    public Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("section", id);
        if (label != null)       m.put("label", label);
        if (visibleWhen != null) m.put("visibleWhen", visibleWhen.toJson());
        m.put("items", fields.stream().map(FieldDefinition::toJson).collect(Collectors.toList()));
        return m;
    }
}
