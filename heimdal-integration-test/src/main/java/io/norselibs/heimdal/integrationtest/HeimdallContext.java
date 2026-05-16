package io.norselibs.heimdal.integrationtest;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.odinjector.binding.Binder;
import io.odinjector.binding.BindingContext;
import io.varhttp.ObjectFactory;
import io.varhttp.Serializer;
import io.varhttp.VarConfig;
import io.varhttp.VarTestSerializer;

public class HeimdallContext extends BindingContext {
    private final VarConfig config;

    public HeimdallContext(VarConfig config) {
        this.config = config;
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(VarConfig.class).to(() -> config);
        binder.bind(Serializer.class).to(VarTestSerializer.class);
        binder.bind(ObjectMapper.class).to(HeimdallContext::objectMapper);
        binder.bind(XmlMapper.class).to(() -> new XmlMapper());
        binder.bind(ObjectFactory.class).to(HeimdallObjectFactory.class);
    }

    private static ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                // Dates as ISO strings, not timestamps
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                // Empty string → null for date/object fields (blank form fields)
                .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
                // Don't fail when the form sends extra fields the model doesn't have
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
