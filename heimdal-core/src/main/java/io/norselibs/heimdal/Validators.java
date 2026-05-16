package io.norselibs.heimdal;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Built-in validators with sensible default messages.
 *
 * Pass to {@link FieldBuilder#validate(Validator)} or override the message
 * with {@link FieldBuilder#validate(Validator, String)}:
 *
 * <pre>
 * f -> f.textField(Claim::getEmail)
 *       .validate(Validators.email())
 *
 * f -> f.textField(Bike::getName)
 *       .validate(Validators.minLength(3), "Name is too short")
 * </pre>
 */
public final class Validators {

    private Validators() {}

    public static Validator minLength(int min) {
        return minLength(min, "Must be at least " + min + " character" + (min == 1 ? "" : "s"));
    }

    public static Validator minLength(int min, String message) {
        return v -> v.length() < min ? Optional.of(message) : Optional.empty();
    }

    public static Validator maxLength(int max) {
        return maxLength(max, "Must be at most " + max + " character" + (max == 1 ? "" : "s"));
    }

    public static Validator maxLength(int max, String message) {
        return v -> v.length() > max ? Optional.of(message) : Optional.empty();
    }

    public static Validator email() {
        return email("Must be a valid email address");
    }

    public static Validator email(String message) {
        Pattern p = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
        return v -> p.matcher(v).matches() ? Optional.empty() : Optional.of(message);
    }

    public static Validator pattern(Pattern pattern, String message) {
        return v -> pattern.matcher(v).matches() ? Optional.empty() : Optional.of(message);
    }

    public static Validator pattern(String regex, String message) {
        return pattern(Pattern.compile(regex), message);
    }

    public static Validator numeric() {
        return numeric("Must be a number");
    }

    public static Validator numeric(String message) {
        return v -> {
            try { Double.parseDouble(v); return Optional.empty(); }
            catch (NumberFormatException e) { return Optional.of(message); }
        };
    }
}
