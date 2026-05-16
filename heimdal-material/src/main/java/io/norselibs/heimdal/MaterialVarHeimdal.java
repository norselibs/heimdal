package io.norselibs.heimdal;

import io.varhttp.ControllerContext;
import io.varhttp.Serializer;

/**
 * MUI CSS-styled variant of {@link VarHeimdal}.
 *
 * Loads MUI CSS from CDN and a small CSS override ({@code /heimdal-material/forms.css})
 * that maps Heimdal's existing field classes to a Material look. The default
 * {@code fields.js} and {@code hm-form.js} web components are unchanged —
 * no new components needed.
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
    protected String pageShell(String formJson) {
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
                    <script type="module" src="/heimdal/hm-form.js"></script>
                </head>
                <body>
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
