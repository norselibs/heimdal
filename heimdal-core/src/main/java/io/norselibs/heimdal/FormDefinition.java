package io.norselibs.heimdal;

import io.ran.Clazz;
import io.norselibs.heimdal.definition.FieldDefinition;
import io.norselibs.heimdal.definition.ItemDefinition;
import io.norselibs.heimdal.definition.SectionDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FormDefinition<T> {
    private final Clazz<T> clazz;
    private final List<ItemDefinition> items;
    private final String submitUrl;

    FormDefinition(FormBuilder<T> builder) {
        this.clazz = builder.clazz;
        this.items = List.copyOf(builder.items);
        this.submitUrl = builder.submitUrl;
    }

    /**
     * Produces the JSON form definition embedded in the page shell.
     *
     * @param formId      stable identifier for this form, e.g. "frm-bike-new"
     * @param contextPath servlet context path prefix, e.g. "" or "/app"
     */
    public Map<String, Object> toJson(String formId, String contextPath) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("formId", formId);
        json.put("eventEndpoint", contextPath + "/heimdal/" + formId + "/event");
        json.put("submitEndpoint", submitUrl != null ? submitUrl : contextPath + "/heimdal/" + formId + "/submit");
        json.put("items", items.stream().map(ItemDefinition::toJson).collect(Collectors.toList()));
        json.put("actions", List.of(Map.of("type", "submit", "label", "Save")));
        return json;
    }

    /**
     * Handles a validate event: runs validators for the fields declared in the
     * triggering field's {@code validates} list, skipping fields in hidden sections.
     */
    public Map<String, List<String>> handleValidate(String triggeringField,
                                                     Map<String, String> values) {
        FieldDefinition trigger = findField(triggeringField);
        if (trigger == null || trigger.getValidateOn() == null) {
            return Map.of();
        }
        Map<String, List<String>> errors = new LinkedHashMap<>();
        for (String fieldName : trigger.getValidates()) {
            FieldDefinition field = findField(fieldName);
            if (field == null || !isVisible(field, values)) continue;
            errors.put(fieldName, validateField(field, values.get(fieldName)));
        }
        return errors;
    }

    // --- internals ---

    private List<String> validateField(FieldDefinition field, String rawValue) {
        List<String> errors = new ArrayList<>();
        String value = rawValue != null ? rawValue.trim() : "";
        if (field.isRequired() && value.isEmpty()) {
            errors.add(field.getLabel() + " is required");
        }
        return errors;
    }

    private boolean isVisible(FieldDefinition field, Map<String, String> values) {
        for (ItemDefinition item : items) {
            if (item instanceof SectionDefinition section
                    && section.getFields().contains(field)) {
                return section.getVisibleWhen().evaluate(values);
            }
        }
        return true;
    }

    private FieldDefinition findField(String name) {
        for (FieldDefinition f : allFields()) {
            if (f.getName().equals(name)) return f;
        }
        return null;
    }

    private List<FieldDefinition> allFields() {
        List<FieldDefinition> result = new ArrayList<>();
        for (ItemDefinition item : items) {
            if (item instanceof FieldDefinition f) {
                result.add(f);
            } else if (item instanceof SectionDefinition s) {
                result.addAll(s.getFields());
            }
        }
        return result;
    }
}
