package io.norselibs.heimdal;

import io.ran.QueryWrapper;
import io.norselibs.heimdal.predicate.EqNode;
import io.norselibs.heimdal.predicate.InNode;
import io.norselibs.heimdal.predicate.NeqNode;
import io.norselibs.heimdal.predicate.PredicateNode;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Q<T> {
    private final T proxyInstance;
    private final QueryWrapper queryWrapper;
    private PredicateNode result;

    Q(T proxyInstance, QueryWrapper queryWrapper) {
        this.proxyInstance = proxyInstance;
        this.queryWrapper = queryWrapper;
    }

    public <X> Q<T> eq(Function<T, X> field, X value) {
        field.apply(proxyInstance);
        String fieldName = queryWrapper.getCurrentProperty().getToken().camelHump();
        result = new EqNode(fieldName, String.valueOf(value));
        return this;
    }

    public <X> Q<T> neq(Function<T, X> field, X value) {
        field.apply(proxyInstance);
        String fieldName = queryWrapper.getCurrentProperty().getToken().camelHump();
        result = new NeqNode(fieldName, String.valueOf(value));
        return this;
    }

    @SafeVarargs
    public final <X> Q<T> in(Function<T, X> field, X... values) {
        field.apply(proxyInstance);
        String fieldName = queryWrapper.getCurrentProperty().getToken().camelHump();
        List<String> stringValues = Arrays.stream(values)
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = new InNode(fieldName, stringValues);
        return this;
    }

    PredicateNode build() {
        if (result == null) {
            throw new IllegalStateException("No predicate was defined in section condition");
        }
        return result;
    }
}
