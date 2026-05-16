package io.norselibs.heimdal;

import io.ran.QueryWrapper;
import io.norselibs.heimdal.predicate.EqNode;
import io.norselibs.heimdal.predicate.AllRequiredValidNode;
import io.norselibs.heimdal.predicate.GtFieldNode;
import io.norselibs.heimdal.predicate.GtNode;
import io.norselibs.heimdal.predicate.GteFieldNode;
import io.norselibs.heimdal.predicate.GteNode;
import io.norselibs.heimdal.predicate.InNode;
import io.norselibs.heimdal.predicate.LtFieldNode;
import io.norselibs.heimdal.predicate.LtNode;
import io.norselibs.heimdal.predicate.LteFieldNode;
import io.norselibs.heimdal.predicate.LteNode;
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

    /** field &lt;= constant. Uses lexicographic comparison — correct for ISO 8601 dates. */
    public <X> Q<T> lte(Function<T, X> field, X value) {
        field.apply(proxyInstance);
        result = new LteNode(queryWrapper.getCurrentProperty().getToken().camelHump(), String.valueOf(value));
        return this;
    }

    /** field &gt;= constant. Uses lexicographic comparison — correct for ISO 8601 dates. */
    public <X> Q<T> gte(Function<T, X> field, X value) {
        field.apply(proxyInstance);
        result = new GteNode(queryWrapper.getCurrentProperty().getToken().camelHump(), String.valueOf(value));
        return this;
    }

    /** field &lt; constant. Uses lexicographic comparison — correct for ISO 8601 dates. */
    public <X> Q<T> lt(Function<T, X> field, X value) {
        field.apply(proxyInstance);
        result = new LtNode(queryWrapper.getCurrentProperty().getToken().camelHump(), String.valueOf(value));
        return this;
    }

    // Cross-field comparisons (field vs field, both from form values)
    public <X> Q<T> gtField(Function<T, X> field1, Function<T, X> field2) {
        field1.apply(proxyInstance); String f1 = queryWrapper.getCurrentProperty().getToken().camelHump();
        field2.apply(proxyInstance); String f2 = queryWrapper.getCurrentProperty().getToken().camelHump();
        result = new GtFieldNode(f1, f2); return this;
    }
    public <X> Q<T> gteField(Function<T, X> field1, Function<T, X> field2) {
        field1.apply(proxyInstance); String f1 = queryWrapper.getCurrentProperty().getToken().camelHump();
        field2.apply(proxyInstance); String f2 = queryWrapper.getCurrentProperty().getToken().camelHump();
        result = new GteFieldNode(f1, f2); return this;
    }
    public <X> Q<T> ltField(Function<T, X> field1, Function<T, X> field2) {
        field1.apply(proxyInstance); String f1 = queryWrapper.getCurrentProperty().getToken().camelHump();
        field2.apply(proxyInstance); String f2 = queryWrapper.getCurrentProperty().getToken().camelHump();
        result = new LtFieldNode(f1, f2); return this;
    }
    public <X> Q<T> lteField(Function<T, X> field1, Function<T, X> field2) {
        field1.apply(proxyInstance); String f1 = queryWrapper.getCurrentProperty().getToken().camelHump();
        field2.apply(proxyInstance); String f2 = queryWrapper.getCurrentProperty().getToken().camelHump();
        result = new LteFieldNode(f1, f2); return this;
    }

    /** For use in {@code action().enabledWhen(...)} — client evaluates whether all required fields have values. */
    public Q<T> allRequiredFieldsValid() {
        result = AllRequiredValidNode.INSTANCE; return this;
    }

    /** field &gt; constant. Uses lexicographic comparison — correct for ISO 8601 dates. */
    public <X> Q<T> gt(Function<T, X> field, X value) {
        field.apply(proxyInstance);
        result = new GtNode(queryWrapper.getCurrentProperty().getToken().camelHump(), String.valueOf(value));
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
