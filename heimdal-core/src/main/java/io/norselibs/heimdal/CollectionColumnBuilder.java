package io.norselibs.heimdal;

import io.ran.AutoMapper;
import io.ran.Clazz;
import io.ran.QueryWrapper;
import io.ran.TypeDescriberImpl;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Declares columns for a {@link FormBuilder#collectionField} inline editable list.
 * Uses Ran method references to resolve property names and types from the item class.
 *
 * <pre>
 * f -> f.collectionField(Claim::getWitnesses, Witness.class, c -> {
 *     c.column(Witness::getName).label("Full Name");
 *     c.column(Witness::getPhone);
 * })
 * </pre>
 */
public class CollectionColumnBuilder<I> {
    private final I proxyInstance;
    private final QueryWrapper queryWrapper;
    private final List<Map<String, Object>> columns = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public CollectionColumnBuilder(Class<I> itemClass) {
        TypeDescriberImpl.getTypeDescriber(itemClass); // ensure mapped
        try {
            this.proxyInstance = (I) AutoMapper.getQueryMaps(itemClass).getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Ran could not create proxy for " + itemClass.getName(), e);
        }
        this.queryWrapper = (QueryWrapper) this.proxyInstance;
    }

    public CollectionColumnDef<I> column(Function<I, ?> getter) {
        getter.apply(proxyInstance);
        var property = queryWrapper.getCurrentProperty().copy();
        Map<String, Object> col = new LinkedHashMap<>();
        col.put("name",  property.getToken().camelHump());
        col.put("label", FormBuilder.titleCase(property.getToken().humanReadable()));
        col.put("type",  mapType(property.getType()));
        columns.add(col);
        return new CollectionColumnDef<>(col);
    }

    List<String> columnNames() {
        return columns.stream().map(c -> (String) c.get("name")).collect(Collectors.toList());
    }

    List<Map<String, Object>> build() {
        return columns;
    }

    private static String mapType(Clazz<?> type) {
        Class<?> c = type.getBoxed().clazz;
        if (c == String.class)                  return "string";
        if (c == Integer.class || c == Long.class) return "integer";
        if (c == java.math.BigDecimal.class || c == Double.class || c == Float.class) return "decimal";
        if (c == Boolean.class)                 return "boolean";
        if (c == java.time.LocalDate.class)     return "date";
        return "string";
    }
}
