package io.norselibs.heimdal.integrationtest;

import io.varhttp.Controller;
import io.varhttp.ControllerClass;
import io.varhttp.PathVariable;
import io.varhttp.ResponseHeader;
import io.varhttp.ResponseStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Serves static assets from classpath resources using wildcard path variables.
 * A real deployment would use a CDN or proper asset pipeline.
 */
@ControllerClass
public class StaticController {

    @Controller(path = "/heimdal/{file}")
    public void heimdal(@PathVariable(name = "file") String file,
                         ResponseStream rs, ResponseHeader rh) throws IOException {
        serve("/static/heimdal/" + sanitize(file), contentType(file), rs, rh);
    }

    @Controller(path = "/heimdal-material/{file}")
    public void heimdalMaterial(@PathVariable(name = "file") String file,
                                 ResponseStream rs, ResponseHeader rh) throws IOException {
        serve("/static/heimdal-material/" + sanitize(file), contentType(file), rs, rh);
    }

    private static String sanitize(String file) {
        // Only allow safe filename characters — prevent path traversal
        return file.replaceAll("[^a-zA-Z0-9._-]", "");
    }

    private static String contentType(String file) {
        return file.endsWith(".css") ? "text/css" : "application/javascript";
    }

    private void serve(String resourcePath, String contentType, ResponseStream rs, ResponseHeader rh)
            throws IOException {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) { rh.setStatus(404); return; }
            OutputStream out = rs.getOutputStream(contentType, StandardCharsets.UTF_8);
            is.transferTo(out);
        }
    }
}
