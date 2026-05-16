package io.norselibs.heimdal;

import java.util.Map;
import java.util.Optional;

/**
 * A validation rule that can see all current form values, enabling cross-field
 * validation. For single-field rules use the simpler {@link Validator} instead.
 */
@FunctionalInterface
public interface ContextValidator {
    Optional<String> validate(String fieldValue, Map<String, String> allFormValues);
}
