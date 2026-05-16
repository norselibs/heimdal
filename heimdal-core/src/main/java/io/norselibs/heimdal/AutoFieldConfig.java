package io.norselibs.heimdal;

/**
 * Passed to {@link AnnotationHandler} implementations so they can configure
 * a field without accessing FieldDefinition internals directly.
 *
 * The handlers registered in {@link AnnotationRegistry} use this interface,
 * whether they come from heimdal-core's own annotations or from an adapter
 * module (heimdal-spring, etc.) mapping third-party annotations.
 */
public interface AutoFieldConfig {
    AutoFieldConfig required();
    AutoFieldConfig label(String label);
    AutoFieldConfig multiline();
    AutoFieldConfig validateOnBlur();
    AutoFieldConfig component(String componentName);
    void exclude();
}
