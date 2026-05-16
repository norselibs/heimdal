package io.norselibs.heimdal;

import java.util.function.Function;

record RowActionDef<T>(String label, Function<T, ?> urlProducer) {
    String url(T item) {
        return String.valueOf(urlProducer.apply(item));
    }
}
