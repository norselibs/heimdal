package io.norselibs.heimdal;

import io.ran.Clazz;

public class Form {
    private Form() {}

    public static <T> Hm<T> of(Class<T> clazz, T initialValue) {
        return new Hm<>(Clazz.of(clazz), initialValue);
    }
}
