package io.norselibs.heimdal;

import java.util.function.Function;

/**
 * Returned by {@link FormBuilder#action}. Configures an action button.
 *
 * <pre>
 * f -> f.action("Submit", "/claims/save")
 *        .enabledWhen(q -> q.allRequiredFieldsValid())
 * </pre>
 */
public class ActionBuilder<T> {
    private final FormBuilder<T> form;
    private final FormActionDef def;

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
}
