package io.norselibs.heimdal;

import io.ran.Clazz;

public class Form {
    private Form() {}

    public static <T> FormBuilder<T> of(Class<T> clazz, T initialValue) {
        return new FormBuilder<>(Clazz.of(clazz), initialValue);
    }
}
