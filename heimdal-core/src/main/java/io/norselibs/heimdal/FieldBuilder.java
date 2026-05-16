package io.norselibs.heimdal;

import io.norselibs.heimdal.definition.FieldDefinition;

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

    public FieldBuilder<T> readonly() {
        def.setReadonly(true);
        return this;
    }

    public FieldBuilder<T> component(String componentName) {
        def.setComponent(componentName);
        return this;
    }
}
