# Heimdal

Heimdal is the UI layer in the norselibs stack, sitting alongside [Ran](../ran) (model introspection) and [Valqueries](../valqueries-sql) (persistence).

The goal is to let backend developers define forms, lists, and detail pages from typed Java code, while frontend developers own the component vocabulary as web components. Neither team has to work in the other's domain.

## The problem

Most teams building internal tools or B2B SaaS have 90% backend developers who understand the domain model and can articulate what a page should do, and 10% frontend developers who spend most of their time on individual pages. Heimdal inverts this: the frontend team builds a library of reusable typed web components; the backend team composes pages from that library using the same fluent, method-reference-driven style they already use in Valqueries.

## Architecture

Heimdal uses an MPA-shaped baseline with server-driven partial updates.

- **Server owns**: field visibility, validation rules, form structure
- **Client owns**: current input values (what the user has typed)
- **Stateless server**: each request is independent — no per-user session state

The wire protocol has two HTTP interactions per form URL:

| Method | Purpose |
|--------|---------|
| `GET /bikes/new` | Returns an HTML page with the form definition embedded as JSON |
| `POST /bikes/new` | Validate event — server re-runs validators for a specific field, returns errors |

Submit goes to a separate URL using whatever the framework provides for JSON deserialization. Heimdal is not involved in the submit path.

## Modules

```
heimdal-core              Framework-agnostic. Form builder, predicate algebra,
                          component registry, wire protocol types.
                          No dependency on any web framework or JSON library.

heimdal-integration-test  Runnable demo. Wires heimdal-core into var-http.
                          Shows what an adapter module for any other framework
                          (Spring, Quarkus) would look like.
```

## Form builder

A form is built by chaining typed method references. Ran intercepts the getter calls to derive property names, types, and labels automatically.

```java
Form.of(Bike.class, new Bike())
    .field(Bike::getName)
        .required()
    .field(Bike::getBikeType)           // enum → hm-select-field with options
        .required()
    .section(
        q -> q.eq(Bike::getBikeType, BikeType.MOUNTAIN),   // visibility predicate
        section -> section
            .field(Bike::getSuspensionTravel)
                .label("Suspension Travel (mm)")
                .required()
                .validateOnBlur()
    )
    .field(Bike::getNotes)
        .multiline()                    // String → hm-textarea-field
        .validateOnBlur()
    .submitUrl("/bikes")
    .build();
```

**What the framework infers from Ran** — the developer does not declare these:

| Builder call | Derived automatically |
|---|---|
| `Bike::getName` | Type `String` → `hm-text-field`; label "Name" from token |
| `Bike::getBikeType` | Type `BikeType` (enum) → `hm-select-field`; options from `BikeType.values()` |
| `Bike::getSuspensionTravel` | Type `int` → `hm-number-field` |
| `.multiline()` | Overrides component to `hm-textarea-field` |
| `q -> q.eq(Bike::getBikeType, BikeType.MOUNTAIN)` | Predicate serialized to JSON for client-side evaluation; dependency graph derived for validate events |

**What the developer must state explicitly**:

| Call | Why explicit |
|---|---|
| `.required()` | UI concern, not a domain annotation |
| `.label("...")` | Only when the token-derived label is wrong or needs a unit |
| `.multiline()` | `String` is ambiguous — could be text or textarea |
| `.validateOnBlur()` | Developer decides which fields justify a server round-trip |
| `.submitUrl("/bikes")` | Where the form POSTs on submit |

## Conditional sections

Section visibility uses the same predicate algebra as Valqueries queries. Simple predicates (`eq`, `neq`, `in`) are serialized to JSON and evaluated client-side — no round trip. Complex predicates fall back to the server.

```java
.section(q -> q.eq(Bike::getBikeType, BikeType.MOUNTAIN), section -> section
    .field(Bike::getSuspensionTravel).required()
)

// Multiple values
.section(q -> q.in(Bike::getStatus, Status.ACTIVE, Status.PENDING), section -> section
    .field(Claim::getExpiryDate).required()
)
```

## Component registration

Built-in Java types are pre-registered. Register custom types at startup:

```java
ComponentRegistry.register(
    ComponentRegistration.forType(Money.class)
        .component("hm-money-field")
        .serialize(m -> m.getAmount().toPlainString())
        .deserialize(Money::parse)
        .extraJson((json, field) -> json.put("currency", "USD"))
        .build()
);
```

`hm-form` passes all extra JSON keys as JS properties on the element, so `<hm-money-field>` can read `this.currency` without extra wiring.

For generic types (e.g. `List<Photo>` vs `List<String>`):

```java
ComponentRegistry.register(
    ComponentRegistration.forType(Clazz.ofClasses(List.class, Photo.class))
        .component("hm-photo-upload")
        .build()
);
```

## Layout components

Components with no backing field (info panels, dividers, help text) use `.layout()`:

```java
.layout("hm-info-panel", props -> props
    .put("title", "About bike types")
    .put("content", "Mountain bikes have front suspension.")
)
```

These appear in the form definition JSON with a `component` key but no `name`, and are not included in field tracking or validation.

## Web components contract

Field components (`hm-text-field`, `hm-select-field`, etc.) must implement:

```javascript
get value()                // returns current value as a string
setErrors(messages: string[])  // display or clear inline errors
```

`hm-form` (the framework's own component) creates the component tree from the JSON definition, wires validate events, evaluates visibility predicates, and handles submit. Load `fields.js` before `hm-form.js` so components are defined before `hm-form` renders.

Reference implementations of all built-in field components are in `heimdal-core/src/main/resources/static/heimdal/fields.js`.

## var-http integration

The `heimdal-integration-test` module shows how to wire Heimdal into var-http. The key piece is `VarHeimdal`, a per-request object injected as a controller parameter.

**One-time setup** in app startup:

```java
config.addParameterHandler(VarHeimdalParameterHandler.class);
```

**Controller**:

```java
@ControllerClass
public class BikeFormController {

    @Controller(path = "/bikes/new")
    public Object page(VarHeimdal vh) throws Exception {
        return vh.form(Bike.class, form -> form
                .field(Bike::getName).required()
                .field(Bike::getBikeType).required()
                .section(
                        q -> q.eq(Bike::getBikeType, BikeType.MOUNTAIN),
                        section -> section
                            .field(Bike::getSuspensionTravel).label("Suspension Travel (mm)").required().validateOnBlur()
                )
                .field(Bike::getNotes).multiline().validateOnBlur()
                .submitUrl("/bikes")
        );
    }

    @Controller(path = "/bikes", httpMethods = HttpMethod.POST)
    public Map<String, Object> createBike(@RequestBody Bike bike) {
        bikeService.save(bike);
        return Map.of("redirect", "/bikes/new");
    }
}
```

`vh.form()` dispatches on HTTP method: GET returns the HTML page, POST handles a validate event. The submit endpoint (`/bikes`) is a regular MVC method — var-http deserializes `Bike` from the JSON body automatically, just as Spring or Quarkus would.

For edit forms, provide the existing entity as the initial value:

```java
vh.form(Bike.class, existingBike, form -> form. ...)
```

## Running the integration test

```bash
./gradlew :heimdal-integration-test:run
```

Open `http://localhost:8080/bikes/new`.

## Further reading

- `docs/spec.md` — full architecture and design rationale
- `docs/decisions/2026-05-10_wire-protocol-v0.md` — wire protocol specification with annotated JSON examples
