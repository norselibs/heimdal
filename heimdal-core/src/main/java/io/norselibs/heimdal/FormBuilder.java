package io.norselibs.heimdal;

import io.ran.AutoMapper;
import io.ran.Clazz;
import io.ran.QueryWrapper;
import io.ran.TypeDescriber;
import io.ran.TypeDescriberImpl;
import io.norselibs.heimdal.definition.FieldDefinition;
import io.norselibs.heimdal.definition.ItemDefinition;
import io.norselibs.heimdal.definition.LayoutItemDefinition;
import io.norselibs.heimdal.definition.SectionDefinition;

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
        var predicate = new Q<>(proxyInstance, queryWrapper);
        predicateConsumer.accept(predicate);
        var bodyBuilder = new FormBuilder<>(this);
        for (var def : bodyDefs) def.accept(bodyBuilder);
        String sectionId = "s" + sectionCounter.getAndIncrement();
        items.add(new SectionDefinition(sectionId, predicate.build(), bodyBuilder.fieldItems()));
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

    List<FieldDefinition> fieldItems() {
        return items.stream()
                .filter(i -> i instanceof FieldDefinition)
                .map(i -> (FieldDefinition) i)
                .toList();
    }
}
