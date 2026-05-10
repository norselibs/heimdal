package io.norselibs.heimdal.integrationtest;

import io.varhttp.Controller;
import io.varhttp.ControllerClass;
import io.varhttp.HttpMethod;
import io.varhttp.RequestBody;

import java.util.Map;

@ControllerClass
public class BikeFormController {

    @Controller(path = "/bikes/new")
    public Object page(VarHeimdal vh) throws Exception {
        return vh.form(Bike.class, form -> form
                .field(Bike::getName)
                    .required()
                .field(Bike::getBikeType)
                    .required()
                .section(
                        q -> q.eq(Bike::getBikeType, BikeType.MOUNTAIN),
                        section -> section
                            .field(Bike::getSuspensionTravel)
                                .label("Suspension Travel (mm)")
                                .required()
                                .validateOnBlur()
                )
                .field(Bike::getNotes)
                    .multiline()
                    .validateOnBlur()
                .submitUrl("/bikes")
        );
    }

    @Controller(path = "/bikes", httpMethods = HttpMethod.POST)
    public Map<String, Object> createBike(@RequestBody Bike bike) {
        System.out.printf("Saved: %s (%s)%n", bike.getName(), bike.getBikeType());
        return Map.of("redirect", "/bikes/new");
    }
}
