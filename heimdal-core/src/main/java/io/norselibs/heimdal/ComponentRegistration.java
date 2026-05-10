package io.norselibs.heimdal;

import io.ran.Clazz;
import io.norselibs.heimdal.definition.FieldDefinition;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Describes the contract between one Java type and its web component counterpart.
 *
 * Register custom components at startup:
 *
 * <pre>
 * ComponentRegistry.register(
 *     ComponentRegistration.forType(Money.class)
 *         .component("hm-money-field")
 *         .serialize(money -> money.getAmount().toPlainString())
 *         .deserialize(Money::parse)
 *         .extraJson((json, type) -> json.put("currency", "USD"))
 *         .build()
 * );
 * </pre>
 *
 * The extraJson callback adds component-specific properties to the field's JSON
 * definition. hm-form passes these through to the web component as JS properties,
 * so hm-money-field can read {@code this.currency} without any extra wiring.
 */
public class ComponentRegistration<T> {
    final Clazz<T> type;
    public final String componentName;
    private final Function<Object, String> serializer;
    private final Function<String, Object> deserializer;
    private final BiConsumer<Map<String, Object>, FieldDefinition> extraJson;

    private ComponentRegistration(Clazz<T> type, String componentName,
                                   Function<Object, String> serializer,
                                   Function<String, Object> deserializer,
                                   BiConsumer<Map<String, Object>, FieldDefinition> extraJson) {
        this.type = type;
        this.componentName = componentName;
        this.serializer = serializer;
        this.deserializer = deserializer;
        this.extraJson = extraJson;
    }

    public String serialize(Object value) {
        return value != null ? serializer.apply(value) : "";
    }

    public Object deserialize(String raw) {
        return deserializer.apply(raw);
    }

    /** Adds any component-specific keys to the JSON field definition. */
    public void addExtraJson(Map<String, Object> json, FieldDefinition field) {
        if (extraJson != null) extraJson.accept(json, field);
    }

    // -------------------------------------------------------------------------

    public static <T> Builder<T> forType(Class<T> type) {
        return new Builder<>(Clazz.of(type));
    }

    /**
     * Register for a generic type, preserving the type parameters.
     * Use this when the component depends on the element type, not just the raw class.
     *
     * <pre>
     * ComponentRegistration.forType(Clazz.ofClasses(List.class, Photo.class))
     *     .component("hm-photo-upload")
     *     .build()
     * </pre>
     */
    public static <T> Builder<T> forType(Clazz<T> type) {
        return new Builder<>(type);
    }

    public static class Builder<T> {
        private final Clazz<T> type;
        private String componentName;
        @SuppressWarnings("unchecked")
        private Function<Object, String> serializer = v -> v != null ? v.toString() : "";
        private Function<String, Object> deserializer = s -> s;
        private BiConsumer<Map<String, Object>, FieldDefinition> extraJson;

        private Builder(Clazz<T> type) {
            this.type = type;
        }

        public Builder<T> component(String name) {
            this.componentName = name;
            return this;
        }

        @SuppressWarnings("unchecked")
        public Builder<T> serialize(Function<T, String> fn) {
            this.serializer = v -> fn.apply((T) v);
            return this;
        }

        @SuppressWarnings("unchecked")
        public Builder<T> deserialize(Function<String, T> fn) {
            this.deserializer = s -> fn.apply(s);
            return this;
        }

        public Builder<T> extraJson(BiConsumer<Map<String, Object>, FieldDefinition> extra) {
            this.extraJson = extra;
            return this;
        }

        public ComponentRegistration<T> build() {
            Objects.requireNonNull(componentName, "component name is required");
            return new ComponentRegistration<>(type, componentName, serializer, deserializer, extraJson);
        }
    }
}
