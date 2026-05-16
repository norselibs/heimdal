package io.norselibs.heimdal;

import io.ran.AutoMapper;
import io.ran.Clazz;
import io.ran.Property;
import io.ran.QueryWrapper;
import io.ran.TypeDescriber;
import io.ran.TypeDescriberImpl;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ListBuilder<T> {
    final Clazz<T> clazz;
    final List<T> items;
    final T proxyInstance;
    final QueryWrapper queryWrapper;
    final TypeDescriber<T> typeDescriber;
    final List<ColumnDef<T>> columns = new ArrayList<>();
    final List<ActionDef> actions = new ArrayList<>();
    final List<RowActionDef<T>> rowActions = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public ListBuilder(Clazz<T> clazz, List<T> items) {
        this.clazz = clazz;
        this.items = items;
        this.typeDescriber = TypeDescriberImpl.getTypeDescriber(clazz.clazz);
        try {
            this.proxyInstance = (T) AutoMapper.getQueryMaps(clazz.clazz).getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Ran could not create query proxy for " + clazz.clazz.getName(), e);
        }
        this.queryWrapper = (QueryWrapper) this.proxyInstance;
    }

    public ColumnBuilder<T> column(Function<T, ?> getter) {
        getter.apply(proxyInstance);
        var property = queryWrapper.getCurrentProperty().copy();
        var def = new ColumnDef<>(property, getter);
        columns.add(def);
        return new ColumnBuilder<>(this, def);
    }

    /** Page-level action button (e.g. "New"). */
    public ListBuilder<T> action(String label, String url) {
        actions.add(new ActionDef(label, url));
        return this;
    }

    /**
     * Per-row action link (e.g. "Edit").
     *
     * The URL producer may return a {@code String} or any object whose
     * {@code toString()} produces a URL — including var-http {@code Route} objects.
     */
    public ListBuilder<T> rowAction(String label, Function<T, ?> urlProducer) {
        rowActions.add(new RowActionDef<>(label, urlProducer));
        return this;
    }

    public ListDefinition<T> build() {
        return new ListDefinition<>(this);
    }

    /**
     * Auto-list: columns inferred from the DTO's registered/primitive properties
     * in declaration order. Complex types and {@link HmExclude}-annotated fields
     * are skipped.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ListDefinition<T> autoBuild() {
        for (Property<?> property : typeDescriber.allFields()) {
            Annotation[] annotations = FormBuilder.fieldAnnotations(clazz.clazz, property.getToken().camelHump());
            boolean excluded = false;
            for (Annotation a : annotations) { if (a instanceof HmExclude) { excluded = true; break; } }
            if (excluded) continue;

            Clazz<?> type = property.getType();
            if (!ComponentRegistry.isRegistered(type) && !type.clazz.isPrimitive()) continue;

            var def = new ColumnDef<T>(property, null); // null getter → reflection in ColumnDef
            // Apply @HmLabel if present
            for (Annotation a : annotations) {
                if (a instanceof HmLabel lbl) { def.labelOverride = lbl.value(); break; }
            }
            columns.add(def);
        }
        return build();
    }
}
