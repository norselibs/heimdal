package io.norselibs.heimdal.integrationtest;

import io.norselibs.heimdal.VarHeimdal;
import io.norselibs.heimdal.Validators;
import io.varhttp.Controller;
import io.varhttp.ControllerClass;
import io.varhttp.HttpMethod;
import io.varhttp.PathVariable;
import io.varhttp.RequestBody;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@ControllerClass
public class BikeFormController {

    private static final AtomicInteger nextId = new AtomicInteger(1);
    private static final List<Bike> BIKES = seedBikes();

    // -------------------------------------------------------------------------
    // List
    // -------------------------------------------------------------------------

    @Controller(path = "/bikes")
    public Object list(VarHeimdal vh) throws Exception {
        return vh.list(Bike.class, BIKES,
                l -> l.column(Bike::getName),
                l -> l.column(Bike::getBikeType),
                l -> l.column(Bike::getSuspensionTravel).label("Travel (mm)"),
                l -> l.column(Bike::getNotes),
                l -> l.action("New bike", "/bikes/new"),
                l -> l.rowAction("Edit", bike -> "/bikes/" + bike.getId() + "/edit")
        );
    }

    @Controller(path = "/bikes/auto-list")
    public Object autoList(VarHeimdal vh) throws Exception {
        return vh.autoList(Bike.class, BIKES,
                l -> l.action("New bike", "/bikes/auto"),
                l -> l.rowAction("Edit", bike -> "/bikes/" + bike.getId() + "/auto")
        );
    }

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    @Controller(path = "/bikes/new")
    public Object newBike(VarHeimdal vh) throws Exception {
        return vh.form(Bike.class, "/bikes/save",
                f -> f.textField(Bike::getName).required()
                       .validate(Validators.minLength(3))
                       .validate(Validators.maxLength(50)),
                f -> f.ratingField(Bike::getRating),

                f -> f.field(Bike::getBikeType).required(),
                f -> f.section("Suspension",
                        q -> q.eq(Bike::getBikeType, BikeType.MOUNTAIN),
                        s -> s.integerField(Bike::getSuspensionTravel)
                              .label("Suspension Travel (mm)").required().validateOnBlur()
                ),
                f -> f.textareaField(Bike::getNotes).validateOnBlur()
                       .validate(Validators.maxLength(200))
        );
    }

    @Controller(path = "/bikes/auto")
    public Object newBikeAuto(VarHeimdal vh) throws Exception {
        return vh.autoForm(Bike.class, "/bikes/save");
    }

    @Controller(path = "/bikes/save", httpMethods = HttpMethod.POST)
    public Map<String, Object> saveBike(@RequestBody Bike bike) {
        bike.setId(nextId.getAndIncrement());
        BIKES.add(bike);
        System.out.printf("Created: %s (%s)%n", bike.getName(), bike.getBikeType());
        return Map.of("redirect", "/bikes");
    }

    // -------------------------------------------------------------------------
    // Edit
    // -------------------------------------------------------------------------

    @Controller(path = "/bikes/{id}/edit")
    public Object editBike(@PathVariable(name = "id") int id, VarHeimdal vh) throws Exception {
        Bike bike = findById(id);
        return vh.form(Bike.class, bike, "/bikes/" + id + "/save",
                f -> f.textField(Bike::getName).required()
                       .validate(Validators.minLength(3))
                       .validate(Validators.maxLength(50)),
                f -> f.field(Bike::getBikeType).required(),
                f -> f.section("Suspension",
                        q -> q.eq(Bike::getBikeType, BikeType.MOUNTAIN),
                        s -> s.integerField(Bike::getSuspensionTravel)
                              .label("Suspension Travel (mm)").required().validateOnBlur()
                ),
                f -> f.textareaField(Bike::getNotes).validateOnBlur()
                       .validate(Validators.maxLength(200))
        );
    }

    @Controller(path = "/bikes/{id}/auto")
    public Object editBikeAuto(@PathVariable(name = "id") int id, VarHeimdal vh) throws Exception {
        return vh.autoForm(Bike.class, findById(id), "/bikes/" + id + "/save");
    }

    @Controller(path = "/bikes/{id}/save", httpMethods = HttpMethod.POST)
    public Map<String, Object> updateBike(@PathVariable(name = "id") int id, @RequestBody Bike bike) {
        bike.setId(id);
        BIKES.replaceAll(b -> b.getId() == id ? bike : b);
        System.out.printf("Updated: %s (%s)%n", bike.getName(), bike.getBikeType());
        return Map.of("redirect", "/bikes");
    }

    // -------------------------------------------------------------------------

    private static Bike findById(int id) {
        return BIKES.stream()
                .filter(b -> b.getId() == id)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Bike not found: " + id));
    }

    private static List<Bike> seedBikes() {
        List<Bike> list = new ArrayList<>();
        list.add(bike(1, "Trek Marlin",    BikeType.MOUNTAIN, 100, "Great trail bike"));
        list.add(bike(2, "Canyon Aeroad",  BikeType.RACER,      0, "Race-ready"));
        list.add(bike(3, "Cube Kathmandu", BikeType.CITY,        0, "Daily commuter"));
        nextId.set(4);
        return list;
    }

    private static Bike bike(int id, String name, BikeType type, int travel, String notes) {
        Bike b = new Bike();
        b.setId(id);
        b.setName(name);
        b.setBikeType(type);
        b.setSuspensionTravel(travel);
        b.setNotes(notes);
        return b;
    }
}
