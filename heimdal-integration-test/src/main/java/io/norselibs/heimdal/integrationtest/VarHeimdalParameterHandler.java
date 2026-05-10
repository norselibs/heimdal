package io.norselibs.heimdal.integrationtest;

import io.varhttp.Serializer;
import io.varhttp.parameterhandlers.IParameterHandler;
import io.varhttp.parameterhandlers.IParameterHandlerMatcher;

import javax.inject.Inject;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Registers VarHeimdal as an injectable controller parameter.
 *
 * OdinJector creates one instance of this matcher (with Serializer injected),
 * which then produces a fresh VarHeimdal per request from the ControllerContext.
 */
public class VarHeimdalParameterHandler implements IParameterHandlerMatcher {

    private final Serializer serializer;

    @Inject
    public VarHeimdalParameterHandler(Serializer serializer) {
        this.serializer = serializer;
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public IParameterHandler getHandlerIfMatches(Method method, Parameter parameter,
                                                  String path, String classPath) {
        if (VarHeimdal.class == parameter.getType()) {
            return ctx -> new VarHeimdal(ctx, serializer);
        }
        return null;
    }
}
