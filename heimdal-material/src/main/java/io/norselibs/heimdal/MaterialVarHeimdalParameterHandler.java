package io.norselibs.heimdal;

import io.varhttp.Serializer;
import io.varhttp.parameterhandlers.IParameterHandler;
import io.varhttp.parameterhandlers.IParameterHandlerMatcher;

import javax.inject.Inject;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Injects {@link MaterialVarHeimdal} as a {@link VarHeimdal} controller parameter.
 * Register this instead of {@link VarHeimdalParameterHandler} to opt in to MUI styling.
 */
public class MaterialVarHeimdalParameterHandler implements IParameterHandlerMatcher {

    private final Serializer serializer;

    @Inject
    public MaterialVarHeimdalParameterHandler(Serializer serializer) {
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
            return ctx -> new MaterialVarHeimdal(ctx, serializer);
        }
        return null;
    }
}
