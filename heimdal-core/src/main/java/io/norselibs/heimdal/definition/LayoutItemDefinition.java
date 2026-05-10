package io.norselibs.heimdal.definition;

import java.util.Map;

/**
 * A non-field item in the form — a structural/layout component with no backing
 * Java field, no name, no value, and no validation. Examples: info panels,
 * popups, dividers, help text blocks.
 *
 * hm-form creates the element and passes all properties through, but does not
 * add it to the field map or wire validate/submit events.
 */
public class LayoutItemDefinition implements ItemDefinition {
    private final Map<String, Object> json;

    public LayoutItemDefinition(Map<String, Object> json) {
        this.json = Map.copyOf(json);
    }

    @Override
    public Map<String, Object> toJson() {
        return json;
    }
}
