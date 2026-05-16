package io.norselibs.heimdal;

import java.util.Optional;

/**
 * A single validation rule for a form field.
 *
 * Receives the field's raw string value from the form submission and returns
 * an error message, or {@link Optional#empty()} if the value is valid.
 *
 * Use {@link Validators} for common rules. Custom validators are plain lambdas:
 * <pre>
 * f -> f.textField(Bike::getName)
 *       .validate(v -> v.contains(" ") ? Optional.empty() : Optional.of("Must be a full name"))
 * </pre>
 */
@FunctionalInterface
public interface Validator {
    Optional<String> validate(String value);
}
