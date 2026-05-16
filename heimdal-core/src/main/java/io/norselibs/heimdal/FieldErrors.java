package io.norselibs.heimdal;

import io.ran.QueryWrapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Accumulates field-level error messages to return as a 422 response.
 *
 * <pre>
 * .onError(DuplicateClaimException.class, (e, err) ->
 *     err.field(Claim::getPolicyNumber, "A claim already exists for this incident"))
 * </pre>
 */
public class FieldErrors<T> {
    private final Map<String, List<String>> errors = new LinkedHashMap<>();
    private final T proxyInstance;
    private final QueryWrapper queryWrapper;

    FieldErrors(T proxyInstance, QueryWrapper queryWrapper) {
        this.proxyInstance = proxyInstance;
        this.queryWrapper = queryWrapper;
    }

    public FieldErrors<T> field(Function<T, ?> getter, String message) {
        getter.apply(proxyInstance);
        String name = queryWrapper.getCurrentProperty().getToken().camelHump();
        errors.computeIfAbsent(name, k -> new ArrayList<>()).add(message);
        return this;
    }

    public FieldErrors<T> global(String message) {
        errors.computeIfAbsent("_global", k -> new ArrayList<>()).add(message);
        return this;
    }

    Map<String, List<String>> toMap() { return errors; }
}
