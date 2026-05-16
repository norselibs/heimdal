package io.norselibs.heimdal;

import io.ran.Clazz;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry mapping Java types to their web component registrations.
 *
 * Resolution order:
 *  1. Exact Clazz match — preserves generic parameters, so List&lt;Photo&gt; and
 *     List&lt;String&gt; can resolve to different components.
 *  2. Raw Class match (boxed, so int and Integer share one entry).
 *  3. Enum — automatic hm-select-field with options from enum constants.
 *  4. Fallback — hm-text-field.
 *
 * Register custom types at application startup before any forms are built:
 * <pre>
 * // Generic-aware registration:
 * ComponentRegistry.register(
 *     ComponentRegistration.forType(Clazz.ofClasses(List.class, Photo.class))
 *         .component("hm-photo-upload")
 *         .build()
 * );
 *
 * // Simple type registration:
 * ComponentRegistry.register(
 *     ComponentRegistration.forType(Money.class)
 *         .component("hm-money-field")
 *         .serialize(m -> m.getAmount().toPlainString())
 *         .deserialize(Money::parse)
 *         .build()
 * );
 * </pre>
 */
public class ComponentRegistry {
    // Keyed by full Clazz (including generics) — checked first
    private static final Map<Clazz<?>, ComponentRegistration<?>> byClazz = new ConcurrentHashMap<>();
    // Keyed by raw boxed Class — checked second
    private static final Map<Class<?>, ComponentRegistration<?>> byType  = new ConcurrentHashMap<>();

    // Standard component registrations come from GeneratedFormBuilder's static block,
    // which is populated by the generateFormBuilder task scanning fields.js.
    private static final ComponentRegistration<?> FALLBACK =
            ComponentRegistration.forType(String.class).component("hm-text-field").build();

    public static <T> void register(ComponentRegistration<T> registration) {
        store(registration);
    }

    private static <T> void store(ComponentRegistration<T> registration) {
        if (!registration.type.generics.isEmpty()) {
            // Generic type: store with full Clazz so List<Photo> ≠ List<String>
            byClazz.put(registration.type, registration);
        } else {
            // Simple type: normalise int→Integer etc.
            byType.put(registration.type.getBoxed().clazz, registration);
        }
    }

    public static boolean isRegistered(Clazz<?> type) {
        if (byClazz.containsKey(type)) return true;
        if (type.clazz.isEnum()) return true;
        return byType.containsKey(type.getBoxed().clazz);
    }

    public static ComponentRegistration<?> resolve(Clazz<?> type) {
        // 1. Exact Clazz match (honours generics)
        ComponentRegistration<?> reg = byClazz.get(type);
        if (reg != null) return reg;

        // 2. Enum — automatic select field
        if (type.clazz.isEnum()) return buildEnum(type);

        // 3. Raw class match (boxed)
        reg = byType.get(type.getBoxed().clazz);
        return reg != null ? reg : FALLBACK;
    }

    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static ComponentRegistration<?> buildEnum(Clazz<?> type) {
        return buildEnumTyped((Class) type.clazz);
    }

    private static <E extends Enum<E>> ComponentRegistration<E> buildEnumTyped(Class<E> enumClass) {
        return ComponentRegistration.forType(enumClass)
                .component("hm-select-field")
                .deserialize(s -> Enum.valueOf(enumClass, s))
                .extraJson((json, field) -> json.put("options", enumOptions(field.getType())))
                .build();
    }

    static List<Map<String, String>> enumOptions(Clazz<?> type) {
        return Arrays.stream(type.clazz.getEnumConstants())
                .map(c -> {
                    String value = ((Enum<?>) c).name();
                    String label = titleCase(value.replace("_", " ").toLowerCase());
                    return Map.of("value", value, "label", label);
                })
                .collect(Collectors.toList());
    }

    private static String titleCase(String s) {
        return Arrays.stream(s.split(" "))
                .map(w -> w.isEmpty() ? w : Character.toUpperCase(w.charAt(0)) + w.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
}
