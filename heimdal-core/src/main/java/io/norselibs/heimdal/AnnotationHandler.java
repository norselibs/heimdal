package io.norselibs.heimdal;

import java.lang.annotation.Annotation;

@FunctionalInterface
public interface AnnotationHandler<A extends Annotation> {
    void apply(A annotation, AutoFieldConfig field);
}
