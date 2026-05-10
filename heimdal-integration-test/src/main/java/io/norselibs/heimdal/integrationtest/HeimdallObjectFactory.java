package io.norselibs.heimdal.integrationtest;

import io.odinjector.Injector;
import io.varhttp.ObjectFactory;

import javax.inject.Inject;

public class HeimdallObjectFactory implements ObjectFactory {
    private final Injector injector;

    @Inject
    public HeimdallObjectFactory(Injector injector) {
        this.injector = injector;
    }

    @Override
    public <T> T getInstance(Class<T> clazz) {
        return injector.getInstance(clazz);
    }
}
