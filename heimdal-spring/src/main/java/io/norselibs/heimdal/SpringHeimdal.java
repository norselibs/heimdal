package io.norselibs.heimdal;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * Spring MVC adapter for {@link AbstractHeimdal}.
 *
 * Returns {@link ResponseEntity} from every method — Spring handles content negotiation
 * and response serialization. Register via {@link HeimdalAutoConfiguration} (automatic
 * with Spring Boot) or manually:
 *
 * <pre>
 * // In a @Configuration class:
 * {@literal @}Override
 * public void addArgumentResolvers(List&lt;HandlerMethodArgumentResolver&gt; resolvers) {
 *     resolvers.add(new SpringHeimdalArgumentResolver(objectMapper));
 * }
 * </pre>
 *
 * Controller methods declare {@code SpringHeimdal} as a parameter and return
 * {@code ResponseEntity&lt;?&gt;}:
 *
 * <pre>
 * {@literal @}RequestMapping("/bikes/new")   // handles GET + POST (validate events)
 * public ResponseEntity&lt;?&gt; newBike(SpringHeimdal vh) throws Exception {
 *     return vh.form(Bike.class, "/bikes/save",
 *         f -&gt; f.textField(Bike::getName).required(),
 *         f -&gt; f.field(Bike::getBikeType).required()
 *     );
 * }
 *
 * {@literal @}PostMapping("/bikes/save")
 * public ResponseEntity&lt;?&gt; saveBike({@literal @}RequestBody Bike bike, SpringHeimdal vh) throws Exception {
 *     return vh.save(bike, actionDef, this::doSave, "/bikes");
 * }
 * </pre>
 */
public class SpringHeimdal extends AbstractHeimdal<ResponseEntity<?>> {

    private final HttpServletRequest  request;
    private final HttpServletResponse response;
    private final ObjectMapper        objectMapper;

    public SpringHeimdal(HttpServletRequest request, HttpServletResponse response,
                         ObjectMapper objectMapper) {
        this.request      = request;
        this.response     = response;
        this.objectMapper = objectMapper;
    }

    @Override protected String requestMethod()        { return request.getMethod(); }
    @Override protected String requestPath()          { return request.getRequestURI(); }
    @Override protected String requestParam(String n) { return request.getParameter(n); }

    @Override
    @SuppressWarnings("unchecked")
    protected Map<String, Object> readJsonBody() throws Exception {
        return objectMapper.readValue(request.getReader(), Map.class);
    }

    @Override
    protected String serializeToJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    @Override
    protected ResponseEntity<?> htmlResponse(String contentType, String html) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(html);
    }

    @Override
    protected ResponseEntity<?> jsonResponse(Object data) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(data);
    }

    @Override
    protected ResponseEntity<?> errorJsonResponse(int status, Object data) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(data);
    }
}
