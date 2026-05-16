package io.norselibs.heimdal;

import io.varhttp.ControllerContext;
import io.varhttp.Serializer;

import java.util.Map;

/**
 * var-http adapter for {@link AbstractHeimdal}. Injects into controllers via
 * {@link VarHeimdalParameterHandler}; register it at startup:
 * <pre>
 * config.addParameterHandler(VarHeimdalParameterHandler.class);
 * </pre>
 */
public class VarHeimdal extends AbstractHeimdal<Object> {

    private final ControllerContext ctx;
    final         Serializer        serializer; // package-private for MaterialVarHeimdal

    VarHeimdal(ControllerContext ctx, Serializer serializer) {
        this.ctx        = ctx;
        this.serializer = serializer;
    }

    @Override protected String requestMethod()          { return ctx.request().getMethod(); }
    @Override protected String requestPath()            { return ctx.request().getRequestURI(); }
    @Override protected String requestParam(String n)   { return ctx.request().getParameter(n); }

    @Override
    @SuppressWarnings("unchecked")
    protected Map<String, Object> readJsonBody() throws Exception {
        return serializer.deserialize(ctx.request().getReader(), Map.class, "application/json");
    }

    @Override
    protected String serializeToJson(Object obj) throws Exception {
        java.io.StringWriter sw = new java.io.StringWriter();
        serializer.serialize(sw, obj, "application/json");
        return sw.toString();
    }

    @Override
    protected Object htmlResponse(String contentType, String html) {
        ctx.setContentType(contentType);
        return html;
    }

    @Override
    protected Object jsonResponse(Object data) throws Exception {
        ctx.setContentType("application/json; charset=UTF-8");
        return serializeToJson(data);
    }

    @Override
    protected Object errorJsonResponse(int status, Object data) throws Exception {
        ctx.response().setStatus(status);
        return jsonResponse(data);
    }
}
