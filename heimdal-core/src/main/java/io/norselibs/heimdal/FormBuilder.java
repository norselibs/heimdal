package io.norselibs.heimdal;

import io.ran.AutoMapper;
import io.ran.Clazz;
import io.ran.Property;
import io.ran.QueryWrapper;
import io.ran.TypeDescriber;
import io.ran.TypeDescriberImpl;
import io.norselibs.heimdal.definition.FieldDefinition;
import io.norselibs.heimdal.definition.ItemDefinition;
import io.norselibs.heimdal.definition.LayoutItemDefinition;
import io.norselibs.heimdal.definition.SectionDefinition;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

public class FormBuilder<T> {
    final Clazz<T> clazz;
    final T initialValue;
    final T proxyInstance;
    final QueryWrapper queryWrapper;
    final TypeDescriber<T> typeDescriber;
    final List<ItemDefinition> items = new ArrayList<>();
    final List<FormActionDef> actions = new ArrayList<>();
    final List<ActionBuilder<T>> actionBuilders = new ArrayList<>();
    final AtomicInteger sectionCounter;
    String submitUrl;

    @SuppressWarnings("unchecked")
    protected FormBuilder(Clazz<T> clazz, T initialValue) {
        this.clazz = clazz;
        this.initialValue = initialValue;
        this.typeDescriber = TypeDescriberImpl.getTypeDescriber(clazz.clazz);
        try {
            this.proxyInstance = (T) AutoMapper.getQueryMaps(clazz.clazz).getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Ran could not create query proxy for " + clazz.clazz.getName(), e);
        }
        this.queryWrapper = (QueryWrapper) this.proxyInstance;
        this.sectionCounter = new AtomicInteger(0);
    }

    public ActionBuilder<T> action(String label, String url) {
        var def = new FormActionDef(label, url);
        actions.add(def);
        var builder = new ActionBuilder<>(this, def);
        actionBuilders.add(builder);
        return builder;
    }

    // Shared-proxy constructor for section body builders
    protected FormBuilder(FormBuilder<T> parent) {
        this.clazz = parent.clazz;
        this.initialValue = parent.initialValue;
        this.proxyInstance = parent.proxyInstance;
        this.queryWrapper = parent.queryWrapper;
        this.typeDescriber = parent.typeDescriber;
        this.sectionCounter = parent.sectionCounter;
    }

    public FieldBuilder<T> field(Function<T, ?> getter) {
        getter.apply(proxyInstance);
        var property = queryWrapper.getCurrentProperty().copy();
        Object value = getter.apply(initialValue);
        var def = new FieldDefinition(property, value);
        items.add(def);
        return new FieldBuilder<>(this, def);
    }

    @SuppressWarnings("unchecked")
    public FormBuilder<T> section(Consumer<Q<T>> predicateConsumer,
                                   Consumer<FormBuilder<T>>... bodyDefs) {
        return section(null, predicateConsumer, bodyDefs);
    }

    @SuppressWarnings("unchecked")
    public FormBuilder<T> section(String label, Consumer<Q<T>> predicateConsumer,
                                   Consumer<FormBuilder<T>>... bodyDefs) {
        var predicate = new Q<>(proxyInstance, queryWrapper);
        predicateConsumer.accept(predicate);
        var bodyBuilder = new FormBuilder<>(this);
        for (var def : bodyDefs) def.accept(bodyBuilder);
        String sectionId = "s" + sectionCounter.getAndIncrement();
        items.add(new SectionDefinition(sectionId, label, predicate.build(), bodyBuilder.fieldItems()));
        return this;
    }

    public FormBuilder<T> layout(String componentName, Consumer<Map<String, Object>> config) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("component", componentName);
        if (config != null) config.accept(props);
        items.add(new LayoutItemDefinition(props));
        return this;
    }

    public FormBuilder<T> submitUrl(String url) {
        this.submitUrl = url;
        return this;
    }

    public FormDefinition<T> build() {
        return new FormDefinition<>(this);
    }

    /**
     * Builds the form by inferring fields from the DTO's declared properties.
     * Properties are visited in declaration order. For each:
     * <ul>
     *   <li>Registered component type or enum → field, annotation handlers applied</li>
     *   <li>Complex object type (not registered) → always-visible section, recurse</li>
     *   <li>{@link HmExclude} on the field → skipped entirely</li>
     * </ul>
     */
    public FormDefinition<T> autoBuild() {
        for (Property<?> property : typeDescriber.allFields()) {
            autoAddProperty(property, clazz.clazz, initialValue);
        }
        return build();
    }

    private void autoAddProperty(Property<?> property, Class<?> ownerClass, Object ownerValue) {
        Annotation[] annotations = fieldAnnotations(ownerClass, property.getToken().camelHump());
        for (Annotation a : annotations) {
            if (a instanceof HmExclude) return;
        }

        Clazz<?> propType = property.getType();
        if (ComponentRegistry.isRegistered(propType) || propType.clazz.isPrimitive()) {
            var def = new FieldDefinition(property, safeGet(property, ownerValue));
            FieldDefConfig config = new FieldDefConfig(def);
            AnnotationRegistry.applyAll(annotations, config);
            if (!config.excluded) items.add(def);
        } else {
            autoAddSection(property, ownerValue);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void autoAddSection(Property<?> property, Object ownerValue) {
        Object nestedValue = safeGet(property, ownerValue);
        Class<?> nestedClass = property.getType().clazz;
        TypeDescriber nestedDescriber = TypeDescriberImpl.getTypeDescriber(nestedClass);

        List<FieldDefinition> sectionFields = new ArrayList<>();
        for (Object p : nestedDescriber.allFields()) {
            Property<?> nestedProp = (Property<?>) p;
            Annotation[] annotations = fieldAnnotations(nestedClass, nestedProp.getToken().camelHump());
            boolean excluded = false;
            for (Annotation a : annotations) { if (a instanceof HmExclude) { excluded = true; break; } }
            if (excluded) continue;

            var def = new FieldDefinition(nestedProp, safeGet(nestedProp, nestedValue));
            FieldDefConfig config = new FieldDefConfig(def);
            AnnotationRegistry.applyAll(annotations, config);
            if (!config.excluded) sectionFields.add(def);
        }

        if (!sectionFields.isEmpty()) {
            String sectionId = "s" + sectionCounter.getAndIncrement();
            String label = titleCase(property.getToken().humanReadable());
            items.add(new SectionDefinition(sectionId, label, null, sectionFields));
        }
    }

    static Object safeGet(Property<?> property, Object owner) {
        if (owner == null) return null;
        try {
            Field f = findField(owner.getClass(), property.getToken().camelHump());
            if (f != null) {
                f.setAccessible(true);
                return f.get(owner);
            }
        } catch (Exception ignored) {}
        return null;
    }

    static String titleCase(String s) {
        return java.util.Arrays.stream(s.split(" "))
                .map(w -> w.isEmpty() ? w : Character.toUpperCase(w.charAt(0)) + w.substring(1).toLowerCase())
                .collect(java.util.stream.Collectors.joining(" "));
    }

    static Annotation[] fieldAnnotations(Class<?> cls, String fieldName) {
        Field f = findField(cls, fieldName);
        return f != null ? f.getAnnotations() : new Annotation[0];
    }

    private static Field findField(Class<?> cls, String name) {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            try { return c.getDeclaredField(name); }
            catch (NoSuchFieldException ignored) {}
        }
        return null;
    }

    // AutoFieldConfig adapter over FieldDefinition
    private static class FieldDefConfig implements AutoFieldConfig {
        private final FieldDefinition def;
        boolean excluded = false;

        FieldDefConfig(FieldDefinition def) { this.def = def; }

        @Override public AutoFieldConfig required()                  { def.setRequired(true);           return this; }
        @Override public AutoFieldConfig label(String label)         { def.setLabel(label);             return this; }
        @Override public AutoFieldConfig multiline()                 { def.setComponent("hm-textarea-field"); return this; }
        @Override public AutoFieldConfig validateOnBlur()            { def.setValidateOn("blur");       return this; }
        @Override public AutoFieldConfig readonly()                  { def.setReadonly(true);           return this; }
        @Override public AutoFieldConfig component(String name)      { def.setComponent(name);          return this; }
        @Override public void           exclude()                    { excluded = true; }
    }

    List<FieldDefinition> fieldItems() {
        return items.stream()
                .filter(i -> i instanceof FieldDefinition)
                .map(i -> (FieldDefinition) i)
                .toList();
    }
}
