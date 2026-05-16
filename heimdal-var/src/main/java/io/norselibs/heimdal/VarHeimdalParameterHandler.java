package io.norselibs.heimdal;

import io.varhttp.Serializer;
import io.varhttp.parameterhandlers.IParameterHandler;
import io.varhttp.parameterhandlers.IParameterHandlerMatcher;

import javax.inject.Inject;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Registers {@link VarHeimdal} as an injectable controller parameter.
 *
 * Add at startup:
 * <pre>
 * config.addParameterHandler(VarHeimdalParameterHandler.class);
 * </pre>
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
