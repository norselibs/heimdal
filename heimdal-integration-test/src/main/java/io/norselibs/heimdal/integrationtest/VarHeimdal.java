package io.norselibs.heimdal.integrationtest;

import io.norselibs.heimdal.Form;
import io.norselibs.heimdal.Hm;
import io.norselibs.heimdal.FormDefinition;
import io.varhttp.ControllerContext;
import io.varhttp.Serializer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Per-request Heimdal context injected as a controller parameter by var-http.
 *
 * Each field is defined by its own lambda — the lambda receives Hm&lt;T&gt;
 * so typed component methods are available throughout.
 *
 * <pre>
 * vh.form(Bike.class, "/bikes",
 *     f -> f.field(Bike::getName).required(),
 *     f -> f.field(Bike::getBikeType).required(),
 *     f -> f.section(
 *         q -> q.eq(Bike::getBikeType, BikeType.MOUNTAIN),
 *         s -> s.integerField(Bike::getSuspensionTravel).required()
 *     )
 * )
 * </pre>
 */
public class VarHeimdal {
    private final ControllerContext ctx;
    private final Serializer serializer;

    VarHeimdal(ControllerContext ctx, Serializer serializer) {
        this.ctx = ctx;
        this.serializer = serializer;
    }

    /** Form with an explicit submit URL string. */
    @SuppressWarnings("unchecked")
    public <T> Object form(Class<T> clazz, String submitUrl,
                           Consumer<Hm<T>>... definitions) throws Exception {
        T initialValue = clazz.getDeclaredConstructor().newInstance();
        Hm<T> builder = Form.of(clazz, initialValue);
        for (Consumer<Hm<T>> def : definitions) def.accept(builder);
        builder.submitUrl(submitUrl);
        return dispatch(builder.build());
    }

    /** Form without a submit URL — useful when submit is handled by a separate endpoint
     *  and the URL is set via one of the lambdas, or not needed. */
    @SuppressWarnings("unchecked")
    public <T> Object form(Class<T> clazz,
                           Consumer<Hm<T>>... definitions) throws Exception {
        return form(clazz, "", definitions);
    }

    /** Edit-form overload: uses an existing entity for initial field values. */
    @SuppressWarnings("unchecked")
    public <T> Object form(Class<T> clazz, T initialValue, String submitUrl,
                           Consumer<Hm<T>>... definitions) throws Exception {
        Hm<T> builder = Form.of(clazz, initialValue);
        for (Consumer<Hm<T>> def : definitions) def.accept(builder);
        builder.submitUrl(submitUrl);
        return dispatch(builder.build());
    }

    // -------------------------------------------------------------------------

    private <T> Object dispatch(FormDefinition<T> def) throws Exception {
        if ("POST".equalsIgnoreCase(ctx.request().getMethod())) {
            return validateEvent(def);
        }
        return renderPage(def);
    }

    @SuppressWarnings("unchecked")
    private <T> Map<String, Object> validateEvent(FormDefinition<T> def) throws Exception {
        Map<String, Object> body = serializer.deserialize(
                ctx.request().getReader(), Map.class, "application/json");

        String field = (String) body.get("field");
        int seq = ((Number) body.get("seq")).intValue();
        var errors = def.handleValidate(field, extractValues(body));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("seq", seq);
        response.put("errors", errors);
        return response;
    }

    private <T> String renderPage(FormDefinition<T> def) throws Exception {
        String path = ctx.request().getRequestURI();
        String formId = "frm-" + path.replaceFirst("^/", "").replace("/", "-");

        Map<String, Object> json = def.toJson(formId, "");
        json.put("eventEndpoint", path);

        java.io.StringWriter sw = new java.io.StringWriter();
        serializer.serialize(sw, json, "application/json");

        ctx.setContentType("text/html; charset=UTF-8");
        return pageShell(sw.toString().replace("</", "<\\/"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> extractValues(Map<String, Object> body) {
        Map<String, Object> raw = (Map<String, Object>) body.get("values");
        if (raw == null) return Map.of();
        return raw.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue() != null ? e.getValue().toString() : ""
        ));
    }

    private static String pageShell(String formJson) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <title>Heimdal</title>
                    <script type="module" src="/heimdal/fields.js"></script>
                    <script type="module" src="/heimdal/hm-form.js"></script>
                    <style>
                        body { font-family: sans-serif; max-width: 600px; margin: 2rem auto; }
                        .hm-field { display: flex; flex-direction: column; margin-bottom: 1rem; }
                        .hm-field input, .hm-field select, .hm-field textarea { padding: .4rem; font-size: 1rem; }
                        .hm-error { color: crimson; font-size: .875rem; }
                        .hm-actions { margin-top: 1rem; }
                        button[type=submit] { padding: .5rem 1.5rem; font-size: 1rem; cursor: pointer; }
                    </style>
                </head>
                <body>
                    <hm-form>
                        <script type="application/json">
                """ + formJson + """
                        </script>
                    </hm-form>
                </body>
                </html>
                """;
    }
}
