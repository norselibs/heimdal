package io.norselibs.heimdal;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps annotation types to the field configuration they imply.
 *
 * Heimdal's own annotations are pre-registered here. Adapter modules
 * (heimdal-spring, etc.) call {@link #register} at startup to map
 * framework-specific annotations to the same actions.
 *
 * <pre>
 * // In a heimdal-spring adapter:
 * AnnotationRegistry.register(NotNull.class,  (a, f) -> f.required());
 * AnnotationRegistry.register(NotBlank.class, (a, f) -> f.required());
 * </pre>
 */
public class AnnotationRegistry {

    private static final Map<Class<? extends Annotation>, AnnotationHandler<?>> handlers =
            new ConcurrentHashMap<>();

    static {
        register(HmRequired.class,      (a, f) -> f.required());
        register(HmLabel.class,         (a, f) -> f.label(a.value()));
        register(HmMultiline.class,     (a, f) -> f.multiline());
        register(HmValidateOnBlur.class,(a, f) -> f.validateOnBlur());
        register(HmComponent.class,     (a, f) -> f.component(a.value()));
        register(HmExclude.class,       (a, f) -> f.exclude());
    }

    public static <A extends Annotation> void register(Class<A> type, AnnotationHandler<A> handler) {
        handlers.put(type, handler);
    }

    @SuppressWarnings("unchecked")
    static void applyAll(Annotation[] annotations, AutoFieldConfig config) {
        for (Annotation a : annotations) {
            AnnotationHandler handler = handlers.get(a.annotationType());
            if (handler != null) handler.apply(a, config);
        }
    }
}
