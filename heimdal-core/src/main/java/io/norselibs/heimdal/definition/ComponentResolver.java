package io.norselibs.heimdal.definition;

import io.ran.Clazz;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public class ComponentResolver {
    private static final Map<Class<?>, String> DEFAULTS = Map.of(
            String.class,     "hm-text-field",
            Integer.class,    "hm-number-field",
            Long.class,       "hm-number-field",
            BigDecimal.class, "hm-number-field",
            Boolean.class,    "hm-checkbox-field",
            LocalDate.class,  "hm-date-field"
    );

    public static String resolve(Clazz<?> type) {
        if (type.clazz.isEnum()) return "hm-select-field";
        // getBoxed() normalises int→Integer, long→Long etc., so no need to list primitives separately
        return DEFAULTS.getOrDefault(type.getBoxed().clazz, "hm-text-field");
    }
}
