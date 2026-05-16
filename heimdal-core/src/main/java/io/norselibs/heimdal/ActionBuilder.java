package io.norselibs.heimdal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Returned by {@link FormBuilder#action}. Configures an action button.
 *
 * <pre>
 * f -> f.action("Submit Claim", "/claims/save")
 *        .enabledWhen(q -> q.allRequiredFieldsValid())
 *        .onError(DuplicateClaimException.class, (e, err) ->
 *            err.field(Claim::getPolicyNumber, "A claim already exists"))
 * </pre>
 */
public class ActionBuilder<T> {
    final FormBuilder<T> form;
    final FormActionDef def;
    final List<ErrorHandler<?>> errorHandlers = new ArrayList<>();

    ActionBuilder(FormBuilder<T> form, FormActionDef def) {
        this.form = form;
        this.def = def;
    }

    public ActionBuilder<T> enabledWhen(Function<Q<T>, Q<T>> predicateConsumer) {
        Q<T> q = new Q<>(form.proxyInstance, form.queryWrapper);
        predicateConsumer.apply(q);
        def.enabledWhen = q.build();
        return this;
    }

    public <E extends Exception> ActionBuilder<T> onError(Class<E> type,
                                                           BiConsumer<E, FieldErrors<T>> handler) {
        errorHandlers.add(new ErrorHandler<>(type, handler));
        return this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    boolean tryHandle(Exception ex, FieldErrors<T> errors) {
        for (ErrorHandler h : errorHandlers) {
            if (h.type.isInstance(ex)) {
                h.handler.accept(h.type.cast(ex), errors);
                return true;
            }
        }
        return false;
    }

    record ErrorHandler<E extends Exception>(Class<E> type, BiConsumer<E, ?> handler) {}
}
