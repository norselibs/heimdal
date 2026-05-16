package io.norselibs.heimdal;

import io.ran.Clazz;
import io.varhttp.ControllerContext;
import io.varhttp.Serializer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Per-request Heimdal context injected as a controller parameter by var-http.
 *
 * Register at startup:
 * <pre>
 * config.addParameterHandler(VarHeimdalParameterHandler.class);
 * </pre>
 *
 * Each form field is its own lambda receiving {@link Hm}&lt;T&gt;:
 * <pre>
 * vh.form(Bike.class, "/bikes",
 *     f -> f.textField(Bike::getName).required(),
 *     f -> f.field(Bike::getBikeType).required(),
 *     f -> f.section(q -> q.eq(Bike::getBikeType, BikeType.MOUNTAIN),
 *                    s -> s.integerField(Bike::getSuspensionTravel).required())
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

    /** Form with an explicit submit URL. */
    @SuppressWarnings("unchecked")
    public <T> Object form(Class<T> clazz, String submitUrl,
                           Consumer<Hm<T>>... definitions) throws Exception {
        T initialValue = clazz.getDeclaredConstructor().newInstance();
        Hm<T> builder = Form.of(clazz, initialValue);
        for (Consumer<Hm<T>> def : definitions) def.accept(builder);
        builder.submitUrl(submitUrl);
        return dispatch(builder.build());
    }

    /** Form without a submit URL. */
    @SuppressWarnings("unchecked")
    public <T> Object form(Class<T> clazz,
                           Consumer<Hm<T>>... definitions) throws Exception {
        return form(clazz, "", definitions);
    }

    /** Edit-form: existing entity provides initial field values. */
    @SuppressWarnings("unchecked")
    public <T> Object form(Class<T> clazz, T initialValue, String submitUrl,
                           Consumer<Hm<T>>... definitions) throws Exception {
        Hm<T> builder = Form.of(clazz, initialValue);
        for (Consumer<Hm<T>> def : definitions) def.accept(builder);
        builder.submitUrl(submitUrl);
        return dispatch(builder.build());
    }

    /** Auto-form: field structure and config inferred from the DTO's annotations. */
    public <T> Object autoForm(Class<T> clazz, String submitUrl) throws Exception {
        T initialValue = clazz.getDeclaredConstructor().newInstance();
        var builder = Form.of(clazz, initialValue);
        builder.submitUrl(submitUrl);
        return dispatch(builder.autoBuild());
    }

    /** Auto-form with an existing entity for initial values. */
    public <T> Object autoForm(Class<T> clazz, T initialValue, String submitUrl) throws Exception {
        var builder = Form.of(clazz, initialValue);
        builder.submitUrl(submitUrl);
        return dispatch(builder.autoBuild());
    }

    // -------------------------------------------------------------------------
    // List
    // -------------------------------------------------------------------------

    /**
     * Explicit list: columns declared via varargs lambdas.
     *
     * <pre>
     * vh.list(Bike.class, bikes,
     *     l -> l.column(Bike::getName),
     *     l -> l.column(Bike::getBikeType),
     *     l -> l.column(Bike::getSuspensionTravel).label("Travel (mm)"),
     *     l -> l.action("New", "/bikes/new"),
     *     l -> l.rowAction("Edit", bike -> "/bikes/" + bike.getId())
     * )
     * </pre>
     *
     * The URL producer in {@code rowAction} may return a {@code String} or any
     * object whose {@code toString()} yields a URL — including var-http Route objects.
     */
    @SuppressWarnings("unchecked")
    public <T> Object list(Class<T> clazz, List<T> items,
                           Consumer<ListBuilder<T>>... definitions) throws Exception {
        var builder = new ListBuilder<>(Clazz.of(clazz), items);
        for (Consumer<ListBuilder<T>> def : definitions) def.accept(builder);
        return renderList(builder.build());
    }

    /**
     * Auto-list: columns inferred from the DTO's properties and annotations.
     * Lambdas can still add actions and row actions:
     *
     * <pre>
     * vh.autoList(Bike.class, bikes,
     *     l -> l.action("New", "/bikes/new"),
     *     l -> l.rowAction("Edit", bike -> "/bikes/" + bike.getId() + "/edit")
     * )
     * </pre>
     */
    @SuppressWarnings("unchecked")
    public <T> Object autoList(Class<T> clazz, List<T> items,
                               Consumer<ListBuilder<T>>... definitions) throws Exception {
        var builder = new ListBuilder<>(Clazz.of(clazz), items);
        for (Consumer<ListBuilder<T>> def : definitions) def.accept(builder);
        return renderList(builder.autoBuild());
    }

    // -------------------------------------------------------------------------

    private <T> String renderList(ListDefinition<T> def) throws Exception {
        String path = ctx.request().getRequestURI();
        String listId = "lst-" + path.replaceFirst("^/", "").replace("/", "-");

        java.io.StringWriter sw = new java.io.StringWriter();
        serializer.serialize(sw, def.toJson(listId), "application/json");

        ctx.setContentType("text/html; charset=UTF-8");
        return listPageShell(sw.toString().replace("</", "<\\/"));
    }

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

    protected String listPageShell(String listJson) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <title>Heimdal</title>
                    <script type="module" src="/heimdal/hm-list.js"></script>
                    <style>
                        body { font-family: sans-serif; max-width: 900px; margin: 2rem auto; }
                        .hm-list-toolbar { margin-bottom: 1rem; }
                        .hm-list-action { display: inline-block; padding: .4rem 1rem; margin-right: .5rem;
                                          background: #333; color: #fff; text-decoration: none; font-size: .9rem; }
                        .hm-table { width: 100%; border-collapse: collapse; }
                        .hm-table th, .hm-table td { padding: .5rem .75rem; text-align: left;
                                                       border-bottom: 1px solid #ddd; font-size: .95rem; }
                        .hm-table th { background: #f5f5f5; font-weight: 600; }
                        .hm-row-action { margin-right: .5rem; font-size: .875rem; }
                    </style>
                </head>
                <body>
                    <hm-list>
                        <script type="application/json">
                """ + listJson + """
                        </script>
                    </hm-list>
                </body>
                </html>
                """;
    }

    protected String pageShell(String formJson) {
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
                        fieldset.hm-section { border: 1px solid #ddd; padding: 1rem; margin-bottom: 1rem; }
                        fieldset.hm-section legend { font-weight: 600; padding: 0 .4rem; }
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
