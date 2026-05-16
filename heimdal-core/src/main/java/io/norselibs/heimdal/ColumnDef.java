package io.norselibs.heimdal;

import io.ran.Property;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

class ColumnDef<T> {
    final Property<?> property;
    final Function<T, ?> getter; // null for auto-list columns (reflection used instead)
    String labelOverride;
    // sortable: false — stubbed for future pagination/sorting support

    ColumnDef(Property<?> property, Function<T, ?> getter) {
        this.property = property;
        this.getter = getter;
    }

    String name() {
        return property.getToken().camelHump();
    }

    String label() {
        return labelOverride != null ? labelOverride
                : titleCase(property.getToken().humanReadable());
    }

    String component() {
        return ComponentRegistry.resolve(property.getType()).componentName;
    }

    Object rawValue(T item) {
        return getter != null ? getter.apply(item) : FormBuilder.safeGet(property, item);
    }

    String serializedValue(T item) {
        Object raw = rawValue(item);
        return ComponentRegistry.resolve(property.getType()).serialize(raw);
    }

    private static String titleCase(String s) {
        return Arrays.stream(s.split(" "))
                .map(w -> w.isEmpty() ? w : Character.toUpperCase(w.charAt(0)) + w.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
}
