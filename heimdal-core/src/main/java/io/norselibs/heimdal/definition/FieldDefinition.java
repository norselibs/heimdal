package io.norselibs.heimdal.definition;

import io.ran.Clazz;
import io.ran.Property;
import io.norselibs.heimdal.ComponentRegistration;
import io.norselibs.heimdal.ComponentRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FieldDefinition implements ItemDefinition {
    final Property<?> property;
    final Clazz<?> type;
    final ComponentRegistration<?> registration;
    final String name;
    String label;
    String component;
    Object initialValue;
    boolean required = false;
    boolean readonly = false;
    String validateOn = null;
    List<String> validates;

    public FieldDefinition(Property<?> property, Object initialValue) {
        this.property = property;
        this.type = property.getType();
        this.registration = ComponentRegistry.resolve(type);
        this.name = property.getToken().camelHump();
        this.label = titleCase(property.getToken().humanReadable());
        this.component = registration.componentName;
        this.initialValue = initialValue;
        this.validates = new ArrayList<>(List.of(this.name));
    }

    private static String titleCase(String humanReadable) {
        return Arrays.stream(humanReadable.split(" "))
                .map(w -> w.isEmpty() ? w : Character.toUpperCase(w.charAt(0)) + w.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    public void setLabel(String label)       { this.label = label; }
    public void setComponent(String c)       { this.component = c; }
    public void setRequired(boolean r)       { this.required = r; }
    public void setReadonly(boolean r)       { this.readonly = r; }
    public void setValidateOn(String event)  { this.validateOn = event; }
    public void setValidates(List<String> v) { this.validates = v; }

    public String getName()            { return name; }
    public String getLabel()           { return label; }
    public boolean isRequired()        { return required; }
    public String getValidateOn()      { return validateOn; }
    public List<String> getValidates() { return validates; }
    public Clazz<?> getType()          { return type; }
    public Property<?> getProperty()   { return property; }
    public ComponentRegistration<?> getRegistration() { return registration; }

    @Override
    public Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("component", component);
        m.put("name", name);
        m.put("label", label);
        m.put("value", registration.serialize(initialValue));
        if (required)  m.put("required", true);
        if (readonly)  m.put("readonly", true);
        if (validateOn != null) {
            m.put("validateOn", validateOn);
            m.put("validates", validates);
        }
        // Delegate component-specific extras (options, currency, etc.) to the registration
        registration.addExtraJson(m, this);
        return m;
    }
}
