package io.norselibs.heimdal;

import io.norselibs.heimdal.definition.FieldDefinition;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Fluent config terminal returned by {@link AutoOverride#field}.
 * Each method chains onto the override applied to the field's {@link FieldDefinition}
 * after annotation-driven defaults are applied.
 */
public class AutoFieldOverride<T> {
    private final AutoOverride<T> owner;
    private final String name;

    AutoFieldOverride(AutoOverride<T> owner, String name) {
        this.owner = owner;
        this.name = name;
    }

    private AutoFieldOverride<T> chain(Consumer<FieldDefinition> step) {
        owner.fieldOverrides.merge(name, step, Consumer::andThen);
        return this;
    }

    public AutoFieldOverride<T> label(String label)     { return chain(d -> d.setLabel(label)); }
    public AutoFieldOverride<T> required()              { return chain(d -> d.setRequired(true)); }
    public AutoFieldOverride<T> readonly()              { return chain(d -> d.setReadonly(true)); }
    public AutoFieldOverride<T> multiline()             { return chain(d -> d.setComponent("hm-textarea-field")); }
    public AutoFieldOverride<T> component(String c)     { return chain(d -> d.setComponent(c)); }

    public AutoFieldOverride<T> validateOnBlur() {
        return chain(d -> d.setValidateOn("blur"));
    }

    public AutoFieldOverride<T> validate(Validator v) {
        return chain(d -> { d.addValidator(v); d.setValidateOn("blur"); });
    }

    public AutoFieldOverride<T> validate(Validator v, String message) {
        return validate(raw -> v.validate(raw).map(ignored -> message));
    }

    public AutoFieldOverride<T> validate(String message, Function<Q<T>, Q<T>> predicateConsumer) {
        Q<T> q = new Q<>(owner.proxyInstance, owner.queryWrapper);
        predicateConsumer.apply(q);
        var node = q.build();
        return chain(d -> {
            d.addContextValidator((rawValue, allValues) ->
                    node.evaluate(allValues) ? Optional.empty() : Optional.of(message));
            d.setValidateOn("blur");
        });
    }

    public AutoFieldOverride<T> minLength(int min) { return validate(Validators.minLength(min)); }
    public AutoFieldOverride<T> maxLength(int max) { return validate(Validators.maxLength(max)); }
}
