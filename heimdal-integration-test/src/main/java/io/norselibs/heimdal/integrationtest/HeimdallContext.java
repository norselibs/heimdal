package io.norselibs.heimdal.integrationtest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
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
        binder.bind(ObjectMapper.class).to(() -> new ObjectMapper());
        binder.bind(XmlMapper.class).to(() -> new XmlMapper());
        binder.bind(ObjectFactory.class).to(HeimdallObjectFactory.class);
    }
}
