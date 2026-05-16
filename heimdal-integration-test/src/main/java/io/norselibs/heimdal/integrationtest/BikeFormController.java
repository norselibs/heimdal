package io.norselibs.heimdal.integrationtest;

import io.norselibs.heimdal.VarHeimdal;
import io.varhttp.Controller;
import io.varhttp.ControllerClass;
import io.varhttp.HttpMethod;
import io.varhttp.RequestBody;

import java.util.Map;

@ControllerClass
public class BikeFormController {

    @Controller(path = "/bikes/new")
    public Object page(VarHeimdal vh) throws Exception {
        return vh.form(Bike.class, "/bikes",
                f -> f.textField(Bike::getName).required(),
                f -> f.field(Bike::getBikeType).required(),
                f -> f.section(
                        q -> q.eq(Bike::getBikeType, BikeType.MOUNTAIN),
                        s -> s.integerField(Bike::getSuspensionTravel)
                              .label("Suspension Travel (mm)").required().validateOnBlur()
                ),
                f -> f.textareaField(Bike::getNotes).validateOnBlur()
        );
    }

    /** Auto-form: field structure and hints inferred entirely from Bike's annotations. */
    @Controller(path = "/bikes/auto")
    public Object autoPage(VarHeimdal vh) throws Exception {
        return vh.autoForm(Bike.class, "/bikes");
    }

    @Controller(path = "/bikes", httpMethods = HttpMethod.POST)
    public Map<String, Object> createBike(@RequestBody Bike bike) {
        System.out.printf("Saved: %s (%s)%n", bike.getName(), bike.getBikeType());
        return Map.of("redirect", "/bikes/new");
    }
}
