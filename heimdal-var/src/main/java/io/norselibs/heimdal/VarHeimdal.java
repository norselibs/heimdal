package io.norselibs.heimdal;

import io.ran.Clazz;
import io.varhttp.ControllerContext;
import io.varhttp.Serializer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
// ThrowingConsumer used via ThrowingConsumer<T>
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

    /** Extra component script URLs included in every page shell, in registration order. */
    private static final java.util.List<String> componentScripts = new java.util.ArrayList<>();

    // ---- Menu configuration (set once at startup) ----
    static final java.util.List<MenuItem> menuItems = new java.util.ArrayList<>();
    static String appNameHtml = null;

    /**
     * Sets the application name/logo shown at the start of the nav bar.
     * Accepts arbitrary HTML:
     * <pre>
     * VarHeimdal.setAppName("&lt;img src='/logo.svg' height='24'&gt; MyApp");
     * </pre>
     */
    public static void setAppName(String html) {
        appNameHtml = html;
    }

    /** Adds a menu item without an icon. */
    public static void addMenuItem(String label, String url) {
        menuItems.add(MenuItem.of(label, url));
    }

    /**
     * Adds a menu item with an HTML icon injected before the label.
     * <pre>
     * VarHeimdal.addMenuItem("Bikes",  "/bikes",  "🚴");
     * VarHeimdal.addMenuItem("Claims", "/claims", "&lt;i class='material-icons'&gt;assignment&lt;/i&gt;");
     * </pre>
     */
    public static void addMenuItem(String label, String url, String iconHtml) {
        menuItems.add(MenuItem.of(label, url, iconHtml));
    }

    /**
     * Registers a component JS file to be loaded in every page shell.
     * Call once at app startup for each custom component file.
     *
     * <pre>
     * VarHeimdal.registerComponentScript("/heimdal/custom-fields.js");
     * </pre>
     */
    public static void registerComponentScript(String url) {
        componentScripts.add(url);
    }

    protected String extraScriptTags() {
        return componentScripts.stream()
                .map(s -> "<script type=\"module\" src=\"" + s + "\"></script>")
                .collect(Collectors.joining("\n                    "));
    }

    /**
     * Renders the navigation bar. Override in adapter subclasses for framework-specific
     * styling (e.g. MUI AppBar in {@link MaterialVarHeimdal}).
     */
    protected String renderMenu(String currentPath) {
        if (menuItems.isEmpty() && appNameHtml == null) return "";
        var sb = new StringBuilder();
        sb.append("<nav class=\"hm-nav\">");
        if (appNameHtml != null) {
            sb.append("<span class=\"hm-nav-brand\">").append(appNameHtml).append("</span>");
        }
        for (MenuItem item : menuItems) {
            sb.append("<a href=\"").append(item.url())
              .append("\" class=\"hm-nav-item")
              .append(item.isActive(currentPath) ? " hm-nav-item--active" : "")
              .append("\">");
            if (item.iconHtml() != null) {
                sb.append("<span class=\"hm-nav-icon\">").append(item.iconHtml()).append("</span>");
            }
            sb.append(item.label()).append("</a>");
        }
        sb.append("</nav>");
        return sb.toString();
    }

    // -------------------------------------------------------------------------

    private final ControllerContext ctx;
    final Serializer serializer;  // package-private for MaterialVarHeimdal

    protected String currentPath() {
        return ctx.request().getRequestURI();
    }

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

    /**
     * Runs a save handler and applies any matching {@code onError} handlers
     * registered on the form's actions. Returns a 422 with field errors if a
     * handler matched; otherwise returns a redirect to the given URL.
     */
    public <T> Object save(T model, FormDefinition<T> def,
                           ThrowingConsumer<T> handler, String redirectUrl) throws Exception {
        try {
            handler.accept(model);
            return Map.of("redirect", redirectUrl);
        } catch (Exception ex) {
            var errors = def.handleException(ex);
            if (errors == null) throw ex;
            ctx.response().setStatus(422);
            return Map.of("errors", errors);
        }
    }

    // -------------------------------------------------------------------------
    // List
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    public <T> Object list(Class<T> clazz, List<T> items,
                           Consumer<ListBuilder<T>>... definitions) throws Exception {
        var builder = new ListBuilder<>(Clazz.of(clazz), items);
        for (Consumer<ListBuilder<T>> def : definitions) def.accept(builder);
        return renderList(builder.build());
    }

    @SuppressWarnings("unchecked")
    public <T> Object autoList(Class<T> clazz, List<T> items,
                               Consumer<ListBuilder<T>>... definitions) throws Exception {
        var builder = new ListBuilder<>(Clazz.of(clazz), items);
        for (Consumer<ListBuilder<T>> def : definitions) def.accept(builder);
        return renderList(builder.autoBuild());
    }

    // -------------------------------------------------------------------------

    /** True when the client wants JSON rather than the HTML page shell (e.g. a React SPA). */
    private boolean wantsJson() {
        String accept = ctx.request().getHeader("Accept");
        return accept != null && accept.contains("application/json") && !accept.contains("text/html");
    }

    private <T> String renderList(ListDefinition<T> def) throws Exception {
        String path = currentPath();
        String listId = "lst-" + path.replaceFirst("^/", "").replace("/", "-");
        var json = def.toJson(listId);

        java.io.StringWriter sw = new java.io.StringWriter();
        serializer.serialize(sw, json, "application/json");

        if (wantsJson()) {
            ctx.setContentType("application/json; charset=UTF-8");
            return sw.toString();
        }
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

        String type  = (String) body.getOrDefault("type", "validate");
        String field = (String) body.get("field");
        int seq = ((Number) body.get("seq")).intValue();
        var values = extractValues(body);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("seq", seq);

        if ("update".equals(type)) {
            response.put("sections", def.handleVisibilityUpdate(values));
        } else {
            response.put("errors", def.handleValidate(field, values));
        }
        return response;
    }

    private <T> String renderPage(FormDefinition<T> def) throws Exception {
        String path = currentPath();
        String formId = "frm-" + path.replaceFirst("^/", "").replace("/", "-");

        Map<String, Object> json = def.toJson(formId, "");
        json.put("eventEndpoint", path);

        java.io.StringWriter sw = new java.io.StringWriter();
        serializer.serialize(sw, json, "application/json");

        if (wantsJson()) {
            ctx.setContentType("application/json; charset=UTF-8");
            return sw.toString();
        }
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
        String path = currentPath();
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <title>Heimdal</title>
                    <script type="module" src="/heimdal/hm-list.js"></script>
                    <style>
                        * { box-sizing: border-box; }
                        body { margin: 0; font-family: sans-serif; }
                        .hm-nav { display: flex; align-items: center; background: #333; padding: 0 1rem; }
                        .hm-nav-brand { color: #fff; font-size: 1.05rem; font-weight: 500; padding: .7rem 1.5rem .7rem 0; margin-right: .5rem; text-decoration: none; }
                        .hm-nav-item { color: rgba(255,255,255,.75); text-decoration: none; padding: .7rem 1rem; font-size: .875rem; font-weight: 500; text-transform: uppercase; letter-spacing: .04em; display: flex; align-items: center; gap: .4rem; }
                        .hm-nav-item:hover { color: #fff; }
                        .hm-nav-item--active { color: #fff; box-shadow: inset 0 -2px #fff; }
                        .hm-content { max-width: 960px; margin: 0 auto; padding: 2rem 1rem; }
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
                    """ + renderMenu(path) + """
                    <div class="hm-content">
                        <hm-list>
                            <script type="application/json">
                """ + listJson + """
                            </script>
                        </hm-list>
                    </div>
                </body>
                </html>
                """;
    }

    protected String pageShell(String formJson) {
        String path = currentPath();
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <title>Heimdal</title>
                    <script type="module" src="/heimdal/fields.js"></script>
                    """ + extraScriptTags() + """
                    <script type="module" src="/heimdal/hm-form.js"></script>
                    <style>
                        * { box-sizing: border-box; }
                        body { margin: 0; font-family: sans-serif; }
                        .hm-nav { display: flex; align-items: center; background: #333; padding: 0 1rem; }
                        .hm-nav-brand { color: #fff; font-size: 1.05rem; font-weight: 500; padding: .7rem 1.5rem .7rem 0; margin-right: .5rem; text-decoration: none; }
                        .hm-nav-item { color: rgba(255,255,255,.75); text-decoration: none; padding: .7rem 1rem; font-size: .875rem; font-weight: 500; text-transform: uppercase; letter-spacing: .04em; display: flex; align-items: center; gap: .4rem; }
                        .hm-nav-item:hover { color: #fff; }
                        .hm-nav-item--active { color: #fff; box-shadow: inset 0 -2px #fff; }
                        .hm-content { max-width: 640px; margin: 0 auto; padding: 2rem 1rem; }
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
                    """ + renderMenu(path) + """
                    <div class="hm-content">
                        <hm-form>
                            <script type="application/json">
                """ + formJson + """
                            </script>
                        </hm-form>
                    </div>
                </body>
                </html>
                """;
    }
}
