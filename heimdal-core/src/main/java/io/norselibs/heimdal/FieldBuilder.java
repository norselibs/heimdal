package io.norselibs.heimdal;

import io.norselibs.heimdal.definition.FieldDefinition;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class FieldBuilder<T> {
    final FormBuilder<T> form;  // package-private for predicate validator access
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

    /**
     * Validates using a predicate from the same algebra used for section visibility.
     * Message comes first to avoid overload resolution ambiguity with {@link Validator}.
     * The predicate sees only this field's value in the map; use a plain
     * {@link Validator} lambda for cross-field rules.
     *
     * <pre>
     * f -> f.dateField(Claim::getIncidentDate)
     *        .validate("Cannot be in the future", q -> q.lte(Claim::getIncidentDate, LocalDate.now()))
     * </pre>
     *
     * Note: comparison is lexicographic — correct for ISO 8601 dates, not for numbers.
     */
    public FieldBuilder<T> validate(String message, Function<Q<T>, Q<T>> predicateConsumer) {
        Q<T> q = new Q<>(form.proxyInstance, form.queryWrapper);
        predicateConsumer.apply(q);
        var node = q.build();
        // Store as ContextValidator so it receives all form values — needed for cross-field predicates.
        def.addContextValidator((rawValue, allValues) ->
                node.evaluate(allValues) ? java.util.Optional.empty() : java.util.Optional.of(message));
        def.setValidateOn("blur");
        return this;
    }

    /** Triggers a server-side visibility update using the project default trigger (change or blur). */
    public FieldBuilder<T> triggersUpdate() {
        def.setTriggersUpdate(HeimdallConfig.defaultUpdateTrigger());
        return this;
    }

    /** Triggers a server-side visibility update on the specified event. */
    public FieldBuilder<T> triggersUpdate(UpdateTrigger trigger) {
        def.setTriggersUpdate(trigger);
        return this;
    }

    public FieldBuilder<T> minLength(int min) { return validate(Validators.minLength(min)); }
    public FieldBuilder<T> maxLength(int max) { return validate(Validators.maxLength(max)); }

    public FieldBuilder<T> validate(Validator validator) {
        def.addValidator(validator);
        def.setValidateOn("blur"); // attaching a rule implies blur-time validation
        return this;
    }

    /** Validates with the given rule but replaces its default message. */
    public FieldBuilder<T> validate(Validator validator, String messageOverride) {
        return validate(v -> validator.validate(v).map(ignored -> messageOverride));
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
