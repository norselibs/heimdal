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
 * Serves the Heimdal JS files from the heimdal-core jar's classpath resources.
 * A real deployment would use a CDN or a proper static asset pipeline; this is
 * enough to run the integration test without any external tooling.
 */
@ControllerClass
public class StaticController {

    @Controller(path = "/heimdal/hm-form.js")
    public void hmForm(ResponseStream responseStream, ResponseHeader responseHeader) throws IOException {
        serve("/static/heimdal/hm-form.js", responseStream, responseHeader);
    }

    @Controller(path = "/heimdal/fields.js")
    public void fields(ResponseStream responseStream, ResponseHeader responseHeader) throws IOException {
        serve("/static/heimdal/fields.js", responseStream, responseHeader);
    }

    @Controller(path = "/heimdal/hm-list.js")
    public void hmList(ResponseStream responseStream, ResponseHeader responseHeader) throws IOException {
        serve("/static/heimdal/hm-list.js", responseStream, responseHeader);
    }

    private void serve(String resourcePath, ResponseStream responseStream, ResponseHeader responseHeader)
            throws IOException {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                responseHeader.setStatus(404);
                return;
            }
            OutputStream out = responseStream.getOutputStream("application/javascript", StandardCharsets.UTF_8);
            is.transferTo(out);
        }
    }
}
