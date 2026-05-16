package io.norselibs.heimdal;

import io.ran.QueryWrapper;
import io.norselibs.heimdal.definition.FieldDefinition;
import io.norselibs.heimdal.predicate.PredicateNode;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Captures field and section overrides for {@link FormBuilder#autoBuild(AutoOverride)}.
 * Pass to {@code vh.autoForm()} to customise specific fields while keeping all others
 * fully auto-inferred.
 *
 * <pre>
 * vh.autoForm(Bike.class, "/bikes/save", o -> {
 *     o.field(Bike::getName).label("Bicycle Name").minLength(3);
 *     o.field(Bike::getSuspensionTravel).validateOnBlur();
 *     o.sectionWhen(BikeFormDto::getSuspension,
 *                   q -> q.eq(Bike::getBikeType, BikeType.MOUNTAIN));
 * })
 * </pre>
 */
public class AutoOverride<T> {
    final T proxyInstance;
    final QueryWrapper queryWrapper;
    final Map<String, Consumer<FieldDefinition>> fieldOverrides = new LinkedHashMap<>();
    final Map<String, PredicateNode> sectionPredicates = new LinkedHashMap<>();

    AutoOverride(T proxyInstance, QueryWrapper queryWrapper) {
        this.proxyInstance = proxyInstance;
        this.queryWrapper = queryWrapper;
    }

    /**
     * Overrides configuration for the auto-inferred field identified by {@code getter}.
     * The field's component, initial value, and annotation-driven defaults are still
     * derived automatically; this only applies on top of them.
     */
    public AutoFieldOverride<T> field(Function<T, ?> getter) {
        getter.apply(proxyInstance);
        String name = queryWrapper.getCurrentProperty().getToken().camelHump();
        fieldOverrides.putIfAbsent(name, def -> {});
        return new AutoFieldOverride<>(this, name);
    }

    /**
     * Adds a visibility predicate to the section auto-generated from a nested DTO field.
     * Without this, sections from complex-type fields are always visible.
     *
     * <pre>
     * o.sectionWhen(FormDto::getAddress, q -> q.eq(FormDto::getType, "BILLING"))
     * </pre>
     */
    public AutoOverride<T> sectionWhen(Function<T, ?> getter, Consumer<Q<T>> predicateConsumer) {
        getter.apply(proxyInstance);
        String name = queryWrapper.getCurrentProperty().getToken().camelHump();
        Q<T> q = new Q<>(proxyInstance, queryWrapper);
        predicateConsumer.accept(q);
        sectionPredicates.put(name, q.build());
        return this;
    }

    void applyField(String name, FieldDefinition def) {
        Consumer<FieldDefinition> override = fieldOverrides.get(name);
        if (override != null) override.accept(def);
    }

    PredicateNode sectionPredicate(String name) {
        return sectionPredicates.get(name);
    }
}
