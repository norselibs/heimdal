package io.norselibs.heimdal;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Spring Boot auto-configuration for Heimdal.
 *
 * Registers {@link SpringHeimdalArgumentResolver} so controllers can declare
 * {@link SpringHeimdal} as a parameter. Provides a sensible default
 * {@link ObjectMapper} if none is already defined (dates as ISO 8601,
 * empty strings as null, unknown properties ignored).
 */
@AutoConfiguration
public class HeimdalAutoConfiguration implements WebMvcConfigurer {

    @Autowired(required = false)
    private ObjectMapper objectMapper;

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper heimdalObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        ObjectMapper mapper = objectMapper != null ? objectMapper : heimdalObjectMapper();
        resolvers.add(new SpringHeimdalArgumentResolver(mapper));
    }
}
