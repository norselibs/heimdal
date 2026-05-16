package io.norselibs.heimdal.integrationtest;

import io.varhttp.Controller;
import io.varhttp.ControllerClass;
import io.varhttp.ResponseHeader;
import io.varhttp.ResponseStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Serves static assets from classpath resources.
 * A real deployment would use a CDN or proper asset pipeline.
 */
@ControllerClass
public class StaticController {

    @Controller(path = "/heimdal/hm-form.js")
    public void hmForm(ResponseStream rs, ResponseHeader rh) throws IOException {
        serveJs("/static/heimdal/hm-form.js", rs, rh);
    }

    @Controller(path = "/heimdal/fields.js")
    public void fields(ResponseStream rs, ResponseHeader rh) throws IOException {
        serveJs("/static/heimdal/fields.js", rs, rh);
    }

    @Controller(path = "/heimdal/hm-list.js")
    public void hmList(ResponseStream rs, ResponseHeader rh) throws IOException {
        serveJs("/static/heimdal/hm-list.js", rs, rh);
    }

    @Controller(path = "/heimdal-material/forms.css")
    public void materialCss(ResponseStream rs, ResponseHeader rh) throws IOException {
        serveCss("/static/heimdal-material/forms.css", rs, rh);
    }

    private void serveJs(String path, ResponseStream rs, ResponseHeader rh) throws IOException {
        serve(path, "application/javascript", rs, rh);
    }

    private void serveCss(String path, ResponseStream rs, ResponseHeader rh) throws IOException {
        serve(path, "text/css", rs, rh);
    }

    private void serve(String resourcePath, String contentType, ResponseStream rs, ResponseHeader rh)
            throws IOException {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                rh.setStatus(404);
                return;
            }
            OutputStream out = rs.getOutputStream(contentType, StandardCharsets.UTF_8);
            is.transferTo(out);
        }
    }
}
