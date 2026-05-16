package io.norselibs.heimdal;

import io.varhttp.ControllerContext;
import io.varhttp.Serializer;

/**
 * MUI CSS-styled variant of {@link VarHeimdal}.
 *
 * Loads MUI CSS from CDN and {@code /heimdal-material/forms.css}.
 * The default field web components are unchanged — styling is CSS-only.
 *
 * Register at startup instead of {@link VarHeimdalParameterHandler}:
 * <pre>
 * config.addParameterHandler(MaterialVarHeimdalParameterHandler.class);
 * </pre>
 */
public class MaterialVarHeimdal extends VarHeimdal {

    MaterialVarHeimdal(ControllerContext ctx, Serializer serializer) {
        super(ctx, serializer);
    }

    @Override
    protected String renderMenu(String currentPath) {
        if (menuItems.isEmpty() && appNameHtml == null) return "";
        var sb = new StringBuilder();
        sb.append("<div class=\"mui-appbar\"><div class=\"mui-container hm-nav\">");
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
        sb.append("</div></div>");
        return sb.toString();
    }

    @Override
    protected String pageShell(String formJson) {
        String path = requestPath();
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <title>Heimdal</title>
                    <link rel="stylesheet" href="//cdn.muicss.com/mui-0.10.3/css/mui.min.css">
                    <link rel="stylesheet" href="/heimdal-material/forms.css">
                    <script type="module" src="/heimdal/fields.js"></script>
                    """ + extraScriptTags() + """
                    <script type="module" src="/heimdal/hm-form.js"></script>
                </head>
                <body>
                    """ + renderMenu(path) + """
                    <div class="mui-container" style="padding-top:2rem">
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

    @Override
    protected String listPageShell(String listJson) {
        String path = requestPath();
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <title>Heimdal</title>
                    <link rel="stylesheet" href="//cdn.muicss.com/mui-0.10.3/css/mui.min.css">
                    <link rel="stylesheet" href="/heimdal-material/forms.css">
                    <script type="module" src="/heimdal/hm-list.js"></script>
                </head>
                <body>
                    """ + renderMenu(path) + """
                    <div class="mui-container" style="padding-top:2rem">
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
}
