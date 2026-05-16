package io.norselibs.heimdal;

import io.ran.Clazz;
import org.junit.Test;


import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class FormBuilderTest {

    /** The example from the wire-protocol doc, built once and reused across tests. */
    private FormDefinition<Bike> bikeForm() {
        var fb = Form.of(Bike.class, new Bike());
        fb.field(Bike::getName).required();
        fb.field(Bike::getBikeType).required();
        fb.section(
                q -> q.eq(Bike::getBikeType, BikeType.MOUNTAIN),
                section -> section.field(Bike::getSuspensionTravel)
                        .label("Suspension Travel (mm)").required().validateOnBlur()
        );
        fb.field(Bike::getNotes).multiline().validateOnBlur();
        return fb.build();
    }

    // -------------------------------------------------------------------------
    // toJson
    // -------------------------------------------------------------------------

    @Test
    public void toJson_topLevelShape() {
        var json = bikeForm().toJson("frm-bike-new", "");

        assertEquals("frm-bike-new",                    json.get("formId"));
        assertEquals("/heimdal/frm-bike-new/event",     json.get("eventEndpoint"));
        assertEquals("/heimdal/frm-bike-new/submit",    json.get("submitEndpoint"));
        assertNotNull(json.get("items"));
        assertNotNull(json.get("actions"));
    }

    @Test
    public void toJson_textFieldShape() {
        var items = items(bikeForm().toJson("frm-bike-new", ""));
        var name = items.get(0);

        assertEquals("hm-text-field", name.get("component"));
        assertEquals("name",          name.get("name"));
        assertEquals("Name",          name.get("label"));
        assertEquals("",              name.get("value"));
        assertEquals(true,            name.get("required"));
        assertNull(name.get("validateOn"));
    }

    @Test
    public void toJson_selectFieldHasEnumOptions() {
        var items = items(bikeForm().toJson("frm-bike-new", ""));
        var bikeType = items.get(1);

        assertEquals("hm-select-field", bikeType.get("component"));
        assertEquals("Bike Type",       bikeType.get("label"));

        @SuppressWarnings("unchecked")
        var options = (List<Map<String, String>>) bikeType.get("options");
        assertEquals(3, options.size());
        assertEquals("MOUNTAIN", options.get(0).get("value"));
        assertEquals("Mountain", options.get(0).get("label"));
        assertEquals("RACER",    options.get(1).get("value"));
        assertEquals("CITY",     options.get(2).get("value"));
    }

    @Test
    public void toJson_sectionShape() {
        var items = items(bikeForm().toJson("frm-bike-new", ""));
        var section = items.get(2);

        assertNotNull(section.get("section"));
        @SuppressWarnings("unchecked")
        var visibleWhen = (Map<String, Object>) section.get("visibleWhen");
        assertEquals("eq",       visibleWhen.get("op"));
        assertEquals("bikeType", visibleWhen.get("field"));
        assertEquals("MOUNTAIN", visibleWhen.get("value"));

        @SuppressWarnings("unchecked")
        var sectionItems = (List<Map<String, Object>>) section.get("items");
        assertEquals(1, sectionItems.size());
        var travel = sectionItems.get(0);
        assertEquals("hm-number-field",        travel.get("component"));
        assertEquals("suspensionTravel",       travel.get("name"));
        assertEquals("Suspension Travel (mm)", travel.get("label"));
        assertEquals("blur",                   travel.get("validateOn"));
    }

    @Test
    public void toJson_textareaFromMultiline() {
        var items = items(bikeForm().toJson("frm-bike-new", ""));
        var notes = items.get(3);

        assertEquals("hm-textarea-field", notes.get("component"));
        assertEquals("notes",             notes.get("name"));
        assertEquals("Notes",             notes.get("label"));
        assertEquals("blur",              notes.get("validateOn"));
        assertNull(notes.get("required"));
    }

    @Test
    public void toJson_contextPathPrefixedOnEndpoints() {
        var json = bikeForm().toJson("frm-bike-new", "/app");

        assertEquals("/app/heimdal/frm-bike-new/event",  json.get("eventEndpoint"));
        assertEquals("/app/heimdal/frm-bike-new/submit", json.get("submitEndpoint"));
    }

    @Test
    public void toJson_initialValuesReflectedInOutput() {
        Bike existing = new Bike();
        existing.setName("Trek");
        existing.setBikeType(BikeType.MOUNTAIN);
        existing.setSuspensionTravel(120);

        var fb = Form.of(Bike.class, existing);
        fb.field(Bike::getName);
        fb.field(Bike::getBikeType);
        fb.section(q -> q.eq(Bike::getBikeType, BikeType.MOUNTAIN),
                section -> section.field(Bike::getSuspensionTravel));
        var json = fb.build().toJson("frm-bike-edit", "");

        var jsonItems = items(json);
        assertEquals("Trek",     jsonItems.get(0).get("value"));
        assertEquals("MOUNTAIN", jsonItems.get(1).get("value"));

        @SuppressWarnings("unchecked")
        var sectionItems = (List<Map<String, Object>>) jsonItems.get(2).get("items");
        assertEquals("120", sectionItems.get(0).get("value"));
    }

    // -------------------------------------------------------------------------
    // handleValidate
    // -------------------------------------------------------------------------

    @Test
    public void validate_requiredFieldError() {
        var errors = bikeForm().handleValidate("suspensionTravel",
                Map.of("name", "Trek", "bikeType", "MOUNTAIN", "suspensionTravel", "", "notes", ""));

        assertTrue(errors.containsKey("suspensionTravel"));
        assertFalse(errors.get("suspensionTravel").isEmpty());
    }

    @Test
    public void validate_requiredFieldPasses() {
        var errors = bikeForm().handleValidate("suspensionTravel",
                Map.of("name", "Trek", "bikeType", "MOUNTAIN", "suspensionTravel", "120", "notes", ""));

        assertTrue(errors.containsKey("suspensionTravel"));
        assertTrue(errors.get("suspensionTravel").isEmpty());
    }

    @Test
    public void validate_skipsFieldInHiddenSection() {
        var errors = bikeForm().handleValidate("suspensionTravel",
                Map.of("name", "Trek", "bikeType", "RACER", "suspensionTravel", "", "notes", ""));

        assertFalse(errors.containsKey("suspensionTravel"));
    }

    @Test
    public void validate_fieldWithoutValidateOnReturnsEmpty() {
        var errors = bikeForm().handleValidate("name",
                Map.of("name", "", "bikeType", "", "suspensionTravel", "", "notes", ""));

        assertTrue(errors.isEmpty());
    }

    // -------------------------------------------------------------------------
    // submitUrl
    // -------------------------------------------------------------------------

    @Test
    public void toJson_submitUrlAppearsInDefinition() {
        var fb = Form.of(Bike.class, new Bike());
        fb.field(Bike::getName);
        fb.submitUrl("/bikes");
        var json = fb.build().toJson("frm-bike-new", "");

        assertEquals("/bikes", json.get("submitEndpoint"));
    }

    @Test
    public void toJson_submitUrlDefaultsToHeimdallConvention() {
        var fb = Form.of(Bike.class, new Bike());
        fb.field(Bike::getName);
        var json = fb.build().toJson("frm-bike-new", "");

        assertEquals("/heimdal/frm-bike-new/submit", json.get("submitEndpoint"));
    }

    // -------------------------------------------------------------------------
    // layout components
    // -------------------------------------------------------------------------

    @Test
    public void toJson_layoutComponentAppearsInItems() {
        var fb = Form.of(Bike.class, new Bike());
        fb.field(Bike::getName).required();
        fb.layout("hm-info-panel", props -> {
            props.put("title", "About bike types");
            props.put("content", "Mountain bikes have suspension.");
        });
        fb.field(Bike::getBikeType);
        var json = fb.build().toJson("frm-bike-new", "");

        var jsonItems = items(json);
        assertEquals(3, jsonItems.size());

        var panel = jsonItems.get(1);
        assertEquals("hm-info-panel",                  panel.get("component"));
        assertEquals("About bike types",               panel.get("title"));
        assertEquals("Mountain bikes have suspension.", panel.get("content"));
        assertNull("layout items have no name",        panel.get("name"));
        assertNull("layout items have no value",       panel.get("value"));
    }

    @Test
    public void toJson_layoutInsideSectionNotAllowed_layoutOnlyAtFormLevel() {
        var items = items(bikeForm().toJson("frm-bike-new", ""));
        @SuppressWarnings("unchecked")
        var sectionItems = (List<Map<String, Object>>) items.get(2).get("items");
        assertTrue("section items are all fields",
                sectionItems.stream().allMatch(i -> i.containsKey("name")));
    }

    // -------------------------------------------------------------------------
    // generic-type component registration
    // -------------------------------------------------------------------------

    @Test
    public void registry_genericTypeResolvesToSpecificComponent() {
        ComponentRegistry.register(
                ComponentRegistration.forType(Clazz.ofClasses(List.class, String.class))
                        .component("hm-tag-list")
                        .build()
        );

        var listOfString  = Clazz.ofClasses(List.class, String.class);
        var listOfInteger = Clazz.ofClasses(List.class, Integer.class);

        assertEquals("hm-tag-list",  ComponentRegistry.resolve(listOfString).componentName);
        assertEquals("hm-text-field", ComponentRegistry.resolve(listOfInteger).componentName);
    }

    @Test
    public void registry_genericTypeDistinctFromRawClass() {
        ComponentRegistry.register(
                ComponentRegistration.forType(Clazz.ofClasses(List.class, String.class))
                        .component("hm-string-list")
                        .build()
        );

        var bareList = Clazz.of(List.class);
        assertEquals("hm-text-field", ComponentRegistry.resolve(bareList).componentName);
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> items(Map<String, Object> json) {
        return (List<Map<String, Object>>) json.get("items");
    }
}
