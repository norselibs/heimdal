package io.norselibs.heimdal;

import io.ran.Clazz;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Framework-agnostic Heimdal context. Contains all form/list/detail business logic.
 * Concrete adapters extend this and implement the eight abstract methods that
 * read from the request and write to the response in a framework-specific way.
 *
 * <p>RESPONSE is the return type of all public API methods:
 * <ul>
 *   <li>{@code Object} for var-http (return value serialized by the framework)
 *   <li>{@code ResponseEntity<?>} for Spring (returned by the controller method)
 * </ul>
 *
 * @param <RESPONSE> the type returned to the web framework for each request
 */
public abstract class AbstractHeimdal<RESPONSE> {

    // ---- Static startup configuration ----

    private static final List<String>   componentScripts = new ArrayList<>();
    static final         List<MenuItem> menuItems        = new ArrayList<>();
    static               String         appNameHtml      = null;

    public static void setAppName(String html)                           { appNameHtml = html; }
    public static void addMenuItem(String label, String url)             { menuItems.add(MenuItem.of(label, url)); }
    public static void addMenuItem(String label, String url, String icon){ menuItems.add(MenuItem.of(label, url, icon)); }
    public static void registerComponentScript(String url)               { componentScripts.add(url); }

    // ---- Abstract framework integration ----

    /** Produce a response with HTML content (form page or list page). */
    protected abstract RESPONSE htmlResponse(String contentType, String html) throws Exception;

    /** Produce a JSON response (validate events, save redirects). */
    protected abstract RESPONSE jsonResponse(Object data) throws Exception;

    /** Produce a JSON response with a non-200 status (e.g. 422 for onError). */
    protected abstract RESPONSE errorJsonResponse(int status, Object data) throws Exception;

    /** HTTP method of the current request (GET, POST, …). */
    protected abstract String requestMethod();

    /** Path portion of the current request URI. */
    protected abstract String requestPath();

    /** Query parameter from the current request, or null if absent. */
    protected abstract String requestParam(String name);

    /** Deserialises the request body as a JSON object map. */
    protected abstract Map<String, Object> readJsonBody() throws Exception;

    /** Serialises an object to a JSON string (used when embedding schema in HTML). */
    protected abstract String serializeToJson(Object obj) throws Exception;

    // ---- Public API ----

    @SuppressWarnings("unchecked")
    public <T> RESPONSE form(Class<T> clazz, String submitUrl,
                              Consumer<Hm<T>>... definitions) throws Exception {
        T init = clazz.getDeclaredConstructor().newInstance();
        Hm<T> builder = Form.of(clazz, init);
        for (var def : definitions) def.accept(builder);
        builder.submitUrl(submitUrl);
        return dispatch(builder.build());
    }

    @SuppressWarnings("unchecked")
    public <T> RESPONSE form(Class<T> clazz, Consumer<Hm<T>>... definitions) throws Exception {
        return form(clazz, "", definitions);
    }

    @SuppressWarnings("unchecked")
    public <T> RESPONSE form(Class<T> clazz, T initialValue, String submitUrl,
                              Consumer<Hm<T>>... definitions) throws Exception {
        Hm<T> builder = Form.of(clazz, initialValue);
        for (var def : definitions) def.accept(builder);
        builder.submitUrl(submitUrl);
        return dispatch(builder.build());
    }

    public <T> RESPONSE autoForm(Class<T> clazz, String submitUrl) throws Exception {
        T init = clazz.getDeclaredConstructor().newInstance();
        var builder = Form.of(clazz, init);
        builder.submitUrl(submitUrl);
        return dispatch(builder.autoBuild());
    }

    public <T> RESPONSE autoForm(Class<T> clazz, String submitUrl,
                                  Consumer<AutoOverride<T>> overrides) throws Exception {
        T init = clazz.getDeclaredConstructor().newInstance();
        var builder = Form.of(clazz, init);
        builder.submitUrl(submitUrl);
        var ao = new AutoOverride<>(builder.proxyInstance, builder.queryWrapper);
        overrides.accept(ao);
        return dispatch(builder.autoBuild(ao));
    }

    public <T> RESPONSE autoForm(Class<T> clazz, T initialValue, String submitUrl) throws Exception {
        var builder = Form.of(clazz, initialValue);
        builder.submitUrl(submitUrl);
        return dispatch(builder.autoBuild());
    }

    public <T> RESPONSE autoForm(Class<T> clazz, T initialValue, String submitUrl,
                                  Consumer<AutoOverride<T>> overrides) throws Exception {
        var builder = Form.of(clazz, initialValue);
        builder.submitUrl(submitUrl);
        var ao = new AutoOverride<>(builder.proxyInstance, builder.queryWrapper);
        overrides.accept(ao);
        return dispatch(builder.autoBuild(ao));
    }

    @SuppressWarnings("unchecked")
    public <T> RESPONSE detail(Class<T> clazz, T entity,
                                Consumer<Hm<T>>... lambdas) throws Exception {
        Hm<T> builder = Form.of(clazz, entity);
        for (var l : lambdas) l.accept(builder);
        return dispatch(builder.autoBuildReadonly());
    }

    public <T> RESPONSE save(T model, FormDefinition<T> def,
                              ThrowingConsumer<T> handler, String redirectUrl) throws Exception {
        try {
            handler.accept(model);
            return jsonResponse(Map.of("redirect", redirectUrl));
        } catch (Exception ex) {
            var errors = def.handleException(ex);
            if (errors == null) throw ex;
            return errorJsonResponse(422, Map.of("errors", errors));
        }
    }

    @SuppressWarnings("unchecked")
    public <T> RESPONSE list(Class<T> clazz, List<T> items,
                              Consumer<ListBuilder<T>>... definitions) throws Exception {
        ListBuilder<T> builder = new ListBuilder<>(Clazz.of(clazz), items);
        for (Consumer<ListBuilder<T>> def : definitions) def.accept(builder);
        ListDefinition<T> def = builder.build();
        return renderList(def);
    }

    @SuppressWarnings("unchecked")
    public <T> RESPONSE autoList(Class<T> clazz, List<T> items,
                                  Consumer<ListBuilder<T>>... definitions) throws Exception {
        ListBuilder<T> builder = new ListBuilder<>(Clazz.of(clazz), items);
        for (Consumer<ListBuilder<T>> def : definitions) def.accept(builder);
        ListDefinition<T> def = builder.autoBuild();
        return renderList(def);
    }

    // ---- Internals ----

    private boolean wantsJson() {
        return "json".equals(requestParam("format"));
    }

    private void injectNav(Map<String, Object> json) {
        if (appNameHtml != null) json.put("appName", appNameHtml);
        if (!menuItems.isEmpty()) {
            json.put("navItems", menuItems.stream().map(m -> {
                var item = new LinkedHashMap<String, Object>();
                item.put("label", m.label());
                item.put("url",   m.url());
                if (m.iconHtml() != null) item.put("iconHtml", m.iconHtml());
                return item;
            }).collect(Collectors.toList()));
        }
    }

    private <T> RESPONSE dispatch(FormDefinition<T> def) throws Exception {
        if ("POST".equalsIgnoreCase(requestMethod())) return validateEvent(def);
        return renderPage(def);
    }

    @SuppressWarnings("unchecked")
    private <T> RESPONSE validateEvent(FormDefinition<T> def) throws Exception {
        Map<String, Object> body = readJsonBody();
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
        return jsonResponse(response);
    }

    private <T> RESPONSE renderPage(FormDefinition<T> def) throws Exception {
        String path   = requestPath();
        String formId = "frm-" + path.replaceFirst("^/", "").replace("/", "-");
        Map<String, Object> json = def.toJson(formId, "");
        json.put("eventEndpoint", path);

        if (wantsJson()) {
            injectNav(json);
            return jsonResponse(json);
        }
        return htmlResponse("text/html; charset=UTF-8",
                pageShell(serializeToJson(json).replace("</", "<\\/")));
    }

    private <T> RESPONSE renderList(ListDefinition<T> def) throws Exception {
        String path   = requestPath();
        String listId = "lst-" + path.replaceFirst("^/", "").replace("/", "-");
        var json = def.toJson(listId);

        if (wantsJson()) {
            injectNav(json);
            return jsonResponse(json);
        }
        return htmlResponse("text/html; charset=UTF-8",
                listPageShell(serializeToJson(json).replace("</", "<\\/")));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> extractValues(Map<String, Object> body) {
        Map<String, Object> raw = (Map<String, Object>) body.get("values");
        if (raw == null) return Map.of();
        return raw.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue() != null ? e.getValue().toString() : ""));
    }

    // ---- Page shells (protected — override for custom styling) ----

    protected String extraScriptTags() {
        return componentScripts.stream()
                .map(s -> "<script type=\"module\" src=\"" + s + "\"></script>")
                .collect(Collectors.joining("\n                    "));
    }

    protected String renderMenu(String currentPath) {
        if (menuItems.isEmpty() && appNameHtml == null) return "";
        var sb = new StringBuilder();
        sb.append("<nav class=\"hm-nav\">");
        if (appNameHtml != null)
            sb.append("<span class=\"hm-nav-brand\">").append(appNameHtml).append("</span>");
        for (MenuItem item : menuItems) {
            sb.append("<a href=\"").append(item.url())
              .append("\" class=\"hm-nav-item")
              .append(item.isActive(currentPath) ? " hm-nav-item--active" : "")
              .append("\">");
            if (item.iconHtml() != null)
                sb.append("<span class=\"hm-nav-icon\">").append(item.iconHtml()).append("</span>");
            sb.append(item.label()).append("</a>");
        }
        sb.append("</nav>");
        return sb.toString();
    }

    protected String listPageShell(String listJson) {
        String path = requestPath();
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
                        .hm-nav-brand { color: #fff; font-size: 1.05rem; font-weight: 500; padding: .7rem 1.5rem .7rem 0; margin-right: .5rem; }
                        .hm-nav-item { color: rgba(255,255,255,.75); text-decoration: none; padding: .7rem 1rem; font-size: .875rem; font-weight: 500; text-transform: uppercase; letter-spacing: .04em; display: flex; align-items: center; gap: .4rem; }
                        .hm-nav-item:hover { color: #fff; } .hm-nav-item--active { color: #fff; box-shadow: inset 0 -2px #fff; }
                        .hm-content { max-width: 960px; margin: 0 auto; padding: 2rem 1rem; }
                        .hm-list-toolbar { margin-bottom: 1rem; }
                        .hm-list-action { display: inline-block; padding: .4rem 1rem; margin-right: .5rem; background: #333; color: #fff; text-decoration: none; font-size: .9rem; }
                        .hm-table { width: 100%; border-collapse: collapse; }
                        .hm-table th, .hm-table td { padding: .5rem .75rem; text-align: left; border-bottom: 1px solid #ddd; font-size: .95rem; }
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
        String path = requestPath();
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
                        .hm-nav-brand { color: #fff; font-size: 1.05rem; font-weight: 500; padding: .7rem 1.5rem .7rem 0; margin-right: .5rem; }
                        .hm-nav-item { color: rgba(255,255,255,.75); text-decoration: none; padding: .7rem 1rem; font-size: .875rem; font-weight: 500; text-transform: uppercase; letter-spacing: .04em; display: flex; align-items: center; gap: .4rem; }
                        .hm-nav-item:hover { color: #fff; } .hm-nav-item--active { color: #fff; box-shadow: inset 0 -2px #fff; }
                        .hm-content { max-width: 640px; margin: 0 auto; padding: 2rem 1rem; }
                        .hm-field { display: flex; flex-direction: column; margin-bottom: 1rem; }
                        .hm-field input, .hm-field select, .hm-field textarea { padding: .4rem; font-size: 1rem; }
                        .hm-error { color: crimson; font-size: .875rem; }
                        fieldset.hm-section { border: 1px solid #ddd; padding: 1rem; margin-bottom: 1rem; }
                        fieldset.hm-section legend { font-weight: 600; padding: 0 .4rem; }
                        .hm-file-row { display: flex; align-items: center; gap: .5rem; margin-top: .25rem; }
                        .hm-file-btn { background: #eee; border: 1px solid #ccc; padding: .3rem .75rem; border-radius: 2px; cursor: pointer; font-size: .9rem; }
                        .hm-file-name { font-size: .9rem; color: #555; }
                        .hm-file-clear { background: none; border: none; cursor: pointer; color: #c00; font-size: 1rem; }
                        .hm-actions { margin-top: 1rem; display: flex; gap: .5rem; align-items: center; }
                        button[type=submit], button[type=button] { padding: .5rem 1.5rem; font-size: 1rem; cursor: pointer; }
                        a.hm-link-action { padding: .5rem 1.5rem; font-size: 1rem; text-decoration: none; color: #333; border: 1px solid #ccc; }
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
