# Heimdal

Heimdal is the UI layer in the norselibs stack, sitting alongside [Ran](../ran) (model introspection) and [Valqueries](../valqueries-sql) (persistence).

The goal is to let backend developers define forms from typed Java code while frontend developers own the component vocabulary as web components. Neither team has to work in the other's domain.

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
heimdal-core              Framework-agnostic. Form builder, annotation registry,
                          component registry, code generation, wire protocol types.
                          No dependency on any web framework or JSON library.

heimdal-var               var-http adapter. VarHeimdal (per-request context) and
                          VarHeimdalParameterHandler (DI integration).
                          Depends on heimdal-core via api.

heimdal-integration-test  Runnable demo. Wires heimdal-var into a var-http app.
                          Shows what an adapter module for any other framework
                          (Spring, Quarkus) would look like.
```

## Form builder

Each field is declared in its own lambda. The lambda parameter `f` is `Hm<T>` — a generated typed form builder that exposes a method for each registered component:

```java
vh.form(Bike.class, "/bikes",
    f -> f.textField(Bike::getName).required(),
    f -> f.field(Bike::getBikeType).required(),       // enum → hm-select-field automatically
    f -> f.section(
        q -> q.eq(Bike::getBikeType, BikeType.MOUNTAIN),
        s -> s.integerField(Bike::getSuspensionTravel)
              .label("Suspension Travel (mm)").required().validateOnBlur()
    ),
    f -> f.textareaField(Bike::getNotes).validateOnBlur()
)
```

Ran intercepts the getter calls to derive property names, types, and labels. The developer only states what can't be inferred:

| Call | Why explicit |
|---|---|
| `.required()` | UI concern, not a domain annotation |
| `.label("...")` | Only when the token-derived label is wrong or needs a unit |
| `.validateOnBlur()` | Developer decides which fields justify a server round-trip |

`vh.form()` dispatches on HTTP method: GET returns the HTML page, POST handles a validate event. Edit forms pass the existing entity as initial values:

```java
vh.form(Bike.class, existingBike, "/bikes",
    f -> f.textField(Bike::getName).required(),
    ...
)
```

## Auto-form

When field order and component choices can be fully derived from the model, declare the form in one line. Heimdal walks the DTO's declared fields using Ran's `TypeDescriber` and applies annotation hints:

```java
@ControllerClass
public class BikeFormController {

    @Controller(path = "/bikes/auto")
    public Object autoPage(VarHeimdal vh) throws Exception {
        return vh.autoForm(Bike.class, "/bikes");
    }
}
```

Annotate the DTO to provide hints:

```java
public class Bike {
    @HmRequired
    private String name;

    @HmRequired
    private BikeType bikeType;

    @HmLabel("Suspension Travel (mm)")
    @HmRequired
    @HmValidateOnBlur
    private int suspensionTravel;

    @HmMultiline
    @HmValidateOnBlur
    private String notes;
}
```

Available annotations: `@HmRequired`, `@HmLabel`, `@HmMultiline`, `@HmValidateOnBlur`, `@HmComponent`, `@HmExclude`.

### Sections from DTO structure

A field whose type is not a registered component is treated as a section — its own fields are rendered as a group. This lets the DTO structure express the form structure without annotations:

```java
public class BikeFormDto {
    @HmRequired String name;
    @HmRequired BikeType bikeType;
    SuspensionSection suspension;   // complex type → always-visible section
    @HmMultiline String notes;
}

public class SuspensionSection {
    @HmLabel("Suspension Travel (mm)") @HmRequired @HmValidateOnBlur
    int suspensionTravel;
    String forkBrand;
}
```

### Annotation registry

Third-party annotations (Bean Validation, Spring, etc.) can be mapped to the same actions via `AnnotationRegistry`. Heimdal's own annotations are pre-registered; adapters add theirs at startup:

```java
// In a hypothetical heimdal-spring adapter:
AnnotationRegistry.register(NotNull.class,  (a, f) -> f.required());
AnnotationRegistry.register(NotBlank.class, (a, f) -> f.required());
```

## Component system

### JS is the source of truth

Frontend developers declare component metadata alongside the component implementation:

```javascript
class HmRatingField extends HTMLElement {
    // type: language-agnostic name → Java type mapping (see table below)
    // default: true → register as the canonical component for this Java type
    static heimdal = { type: 'integer' };   // default: false (opt-in variant)

    get value() { /* return current value as string */ }
    setErrors(messages) { /* display or clear inline errors */ }
}
customElements.define('hm-rating-field', HmRatingField);
```

Standard components in `fields.js` use `default: true`:

```javascript
class HmTextField extends HmBaseField {
    static heimdal = { type: 'string', default: true };
    ...
}
customElements.define('hm-text-field', HmTextField);
```

### Type mapping

| Heimdal type | Java type |
|---|---|
| `string` | `String` |
| `integer` | `Integer` |
| `long` | `Long` |
| `decimal` | `BigDecimal` |
| `boolean` | `Boolean` |
| `date` | `LocalDate` |
| `datetime` | `LocalDateTime` |

A component with `types: ['integer', 'long', 'decimal']` generates one typed method per type (`integerField`, `longField`, `decimalField`). A component with `type: 'string', multiline: true` generates `textareaField()` which sets the component override rather than registering as the String default.

### Code generation

`./gradlew generateFormBuilder` scans all `static/heimdal/*.js` files from every JAR and resource directory on the classpath. It generates `Hm.java` — a typed `FormBuilder` subclass — into `build/generated-sources/heimdal/`. This runs before `compileJava` with no circular dependency.

The generated `Hm<T>` includes:
- `ComponentRegistry.register(...)` calls for every `default: true` component
- A typed method per component (`textField`, `integerField`, `ratingField`, ...)
- A `section()` overload that passes `Hm<T>` to the body, so typed methods are available inside sections

Drop a JS file into `src/main/resources/static/heimdal/` to add project-specific components. The generator picks it up automatically.

### Wiring in Gradle

```groovy
def generatedSourcesDir = layout.buildDirectory.dir('generated-sources/heimdal')

def generateFormBuilder = tasks.register('generateFormBuilder', JavaExec) {
    classpath = configurations.runtimeClasspath + sourceSets.main.resources.sourceDirectories
    mainClass = 'io.norselibs.heimdal.FormBuilderSourceGenerator'
    args = [generatedSourcesDir.get().asFile.absolutePath]
    inputs.files(configurations.runtimeClasspath, sourceSets.main.resources.srcDirs)
    outputs.dir(generatedSourcesDir)
}

sourceSets.main.java.srcDir generatedSourcesDir
tasks.named('compileJava') { dependsOn generateFormBuilder }
```

## Conditional sections

Section visibility uses the same predicate algebra as Valqueries queries. Simple predicates (`eq`, `neq`, `in`) are serialized to JSON and evaluated client-side — no round trip.

```java
f -> f.section(
    q -> q.eq(Bike::getBikeType, BikeType.MOUNTAIN),
    s -> s.integerField(Bike::getSuspensionTravel).required()
)

// Multiple values
f -> f.section(
    q -> q.in(Bike::getStatus, Status.ACTIVE, Status.PENDING),
    s -> s.dateField(Claim::getExpiryDate).required()
)
```

## Layout components

Non-field elements (info panels, dividers, help text) use `layout()`:

```java
f -> f.layout("hm-info-panel", props -> props
    .put("title", "About bike types")
    .put("content", "Mountain bikes have front suspension.")
)
```

Layout items appear in the form JSON with a `component` key but no `name` and are excluded from field tracking and validation.

## Web components contract

Field components must implement:

```javascript
get value()                        // returns current value as a string
setErrors(messages: string[])      // display or clear inline errors
```

`hm-form` creates the component tree from the embedded JSON, wires validate events, evaluates visibility predicates, and handles submit. Load `fields.js` before `hm-form.js`.

## var-http integration

`heimdal-var` provides `VarHeimdal` (per-request context) and `VarHeimdalParameterHandler` (DI wiring). Add to your project:

```groovy
implementation 'io.norselibs:heimdal-var:0.1-SNAPSHOT'
```

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

    @Controller(path = "/bikes", httpMethods = HttpMethod.POST)
    public Map<String, Object> createBike(@RequestBody Bike bike) {
        bikeService.save(bike);
        return Map.of("redirect", "/bikes/new");
    }
}
```

## Running the integration test

```bash
./gradlew :heimdal-integration-test:run
```

Open `http://localhost:8080/bikes/new` for the explicit form or `http://localhost:8080/bikes/auto` for the auto-form demo.

## Further reading

- `docs/spec.md` — full architecture and design rationale
- `docs/decisions/2026-05-10_wire-protocol-v0.md` — wire protocol specification with annotated JSON examples
