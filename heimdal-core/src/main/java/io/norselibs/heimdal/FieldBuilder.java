package io.norselibs.heimdal;

import io.norselibs.heimdal.definition.FieldDefinition;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Returned by FormBuilder.field(). Exposes field-specific configuration methods,
 * then delegates back to FormBuilder for the next field or section.
 */
public class FieldBuilder<T> {
    private final FormBuilder<T> form;
    private final FieldDefinition def;

    FieldBuilder(FormBuilder<T> form, FieldDefinition def) {
        this.form = form;
        this.def = def;
    }

    public FieldBuilder<T> required() {
        def.setRequired(true);
        return this;
    }

    public FieldBuilder<T> label(String label) {
        def.setLabel(label);
        return this;
    }

    public FieldBuilder<T> multiline() {
        def.setComponent("hm-textarea-field");
        return this;
    }

    public FieldBuilder<T> validateOnBlur() {
        def.setValidateOn("blur");
        return this;
    }

    // --- delegate to parent FormBuilder so chains continue naturally ---

    public FieldBuilder<T> field(Function<T, ?> getter) {
        return form.field(getter);
    }

    public FormBuilder<T> layout(String componentName, Consumer<Map<String, Object>> config) {
        return form.layout(componentName, config);
    }

    public FormBuilder<T> section(Consumer<FormPredicate<T>> predicateConsumer,
                                   Consumer<FormBuilder<T>> bodyConsumer) {
        return form.section(predicateConsumer, bodyConsumer);
    }

    public FormBuilder<T> submitUrl(String url) {
        return form.submitUrl(url);
    }

    public FormDefinition<T> build() {
        return form.build();
    }
}
