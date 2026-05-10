# Wire Protocol v0 — Simple Form

**Scope:** the minimum protocol needed to demonstrate a complete round-trip for a simple form:
a handful of fields, one conditional section whose visibility depends on a select field,
per-field validation, and a submit action with server-side error feedback.

Everything in this document is normative for v0. Features that are deliberately excluded are
listed at the end so they don't creep in.

---

## Running example

A "New Bike" form:

| Field             | Type     | Rules                                   |
|-------------------|----------|-----------------------------------------|
| `name`            | text     | required                                |
| `bikeType`        | select   | required; controls visibility below     |
| `suspensionTravel`| number   | visible only when `bikeType = MOUNTAIN`; required when visible |
| `notes`           | textarea | optional; min 10 chars if non-empty     |

The form has one action: **Save** (submit).

---

## 1. Initial page load

The server sends a minimal HTML page containing a single `<hm-form>` element. Inside it
is one `<script type="application/json">` block — the **form definition** — which is the
single source of truth for everything: field types, labels, current values, section
visibility rules, validate triggers, and endpoints.

The `<hm-form>` custom element (provided by the framework) reads this definition and
builds the entire component tree in the browser. The server never renders field HTML.

### 1.1 Page shell

```html
<!DOCTYPE html>
<html>
<head>
  <script type="module" src="/heimdal/hm-form.js"></script>
  <script type="module" src="/components/fields.js"></script>
</head>
<body>
  <hm-form>
    <script type="application/json">
    {
      "formId": "frm-bike-new",
      "eventEndpoint": "/heimdal/frm-bike-new/event",
      "submitEndpoint": "/heimdal/frm-bike-new/submit",
      "items": [
        {
          "component": "hm-text-field",
          "name":      "name",
          "label":     "Name",
          "value":     "",
          "required":  true
        },
        {
          "component": "hm-select-field",
          "name":      "bikeType",
          "label":     "Bike Type",
          "value":     "",
          "required":  true,
          "options": [
            { "value": "MOUNTAIN", "label": "Mountain" },
            { "value": "RACER",    "label": "Racer"    },
            { "value": "CITY",     "label": "City"     }
          ]
        },
        {
          "section":     "suspension",
          "visibleWhen": { "op": "eq", "field": "bikeType", "value": "MOUNTAIN" },
          "items": [
            {
              "component":  "hm-number-field",
              "name":       "suspensionTravel",
              "label":      "Suspension Travel (mm)",
              "value":      "",
              "required":   true,
              "validateOn": "blur",
              "validates":  ["suspensionTravel"]
            }
          ]
        },
        {
          "component":  "hm-textarea-field",
          "name":       "notes",
          "label":      "Notes",
          "value":      "",
          "validateOn": "blur",
          "validates":  ["notes"]
        }
      ],
      "actions": [
        { "type": "submit", "label": "Save" }
      ]
    }
    </script>
  </hm-form>
</body>
</html>
```

The page shell (`<html>`, `<head>`, script imports) is static and cacheable. Only the
JSON inside `<hm-form>` is dynamic.

### 1.2 Backend code

The Java code that produces the JSON above. The framework turns this into the form
definition at request time; the developer never writes JSON by hand.

```java
@Controller
public class BikeController {

    private final BikeService bikeService;

    @Page("/bikes/new")
    public FormDefinition<Bike> newBike() {
        return Form.of(Bike.class, new Bike())
            .field(Bike::getName)
                .required()
            .field(Bike::getBikeType)
                .required()
            .section(q -> q.eq(Bike::getBikeType, BikeType.MOUNTAIN), section -> section
                .field(Bike::getSuspensionTravel)
                    .label("Suspension Travel (mm)")
                    .required()
                    .validateOnBlur()
            )
            .field(Bike::getNotes)
                .multiline()
                .validateOnBlur()
            .onSubmit(bike -> {
                bikeService.save(bike);
                return Redirect.to("/bikes");
            });
    }
}
```

**What the framework derives from this without being told:**

| Builder call | What Ran / the framework infers | JSON produced |
|---|---|---|
| `Bike::getName` | Property type is `String` → text field | `"component": "hm-text-field"` |
| `Bike::getName` | Token `name` → title case | `"label": "Name"` |
| `Bike::getBikeType` | Property type is `enum BikeType` → select field | `"component": "hm-select-field"` |
| `Bike::getBikeType` | `BikeType.values()` → option list | `"options": [{"value":"MOUNTAIN","label":"Mountain"}, ...]` |
| `Bike::getBikeType` | Token `bikeType` → title case | `"label": "Bike Type"` |
| `Bike::getSuspensionTravel` | Property type is `Integer` → number field | `"component": "hm-number-field"` |
| `.label("Suspension Travel (mm)")` | Explicit override | `"label": "Suspension Travel (mm)"` |
| `.validateOnBlur()` | Field validates itself | `"validateOn": "blur", "validates": ["suspensionTravel"]` |
| `.multiline()` | Overrides component | `"component": "hm-textarea-field"` |
| `q -> q.eq(Bike::getBikeType, BikeType.MOUNTAIN)` | Ran proxy records `getBikeType` call; predicate tree walked and serialized | `"visibleWhen": {"op":"eq","field":"bikeType","value":"MOUNTAIN"}` |
| `new Bike()` | All fields empty / default | `"value": ""` for each field |
| `@Page("/bikes/new")` | Route slug → stable form id | `"formId": "frm-bike-new"`, endpoints derived |

**What the framework does not derive and the developer must state explicitly:**
- `.required()` — no annotation on the entity for this; it is a UI concern
- `.label(...)` — only when the default token label is wrong or needs a unit
- `.multiline()` — `String` is ambiguous between text and textarea
- `.validateOnBlur()` — the developer decides which fields justify a server round-trip
- The submit handler and redirect — business logic

### 1.3 Form definition reference

**Top level:**

```
formId          string   stable identifier for this form type + instance
eventEndpoint   string   URL for validate events
submitEndpoint  string   URL for submit
items           array    ordered list of field definitions and section definitions
actions         array    action buttons to render
```

**Field item** (has a `component` key):

```
component    string    custom element name, e.g. "hm-text-field"
name         string    wire name (camelCase, from Ran Token)
label        string    display label
value        string    initial value; always a string
required     boolean   whether this field is required (optional, default false)
validateOn   "blur" | "change" | null   when to send a validate event (optional, default null)
validates    string[]  field names the server will re-validate on this event (required if validateOn is set)
```

Additional keys (e.g. `options` for selects, `min`/`max` for numbers) are passed
through to the component as properties. `<hm-form>` does not interpret them.

**Section item** (has a `section` key):

```
section      string    identifier for this section
visibleWhen  Predicate client-evaluable visibility predicate
items        array     nested field definitions (same shape as top-level field items)
```

Sections do not nest. A section item cannot contain another section item.

**Action item:**

```
type    "submit"   (only type in v0)
label   string     button text
```

**Predicate shapes for v0** (all other shapes are a protocol error):

```json
{ "op": "eq",  "field": "<fieldName>", "value": "<string>" }
{ "op": "neq", "field": "<fieldName>", "value": "<string>" }
{ "op": "in",  "field": "<fieldName>", "values": ["<string>", ...] }
```

`field` refers to the `name` of any field in the definition (including inside sections).
`value`/`values` are strings; compared to the string `value` property of the field component.

---

## 2. Client-side predicate evaluation

When any field's value changes, `<hm-form>` re-evaluates every section's `visibleWhen`
predicate using the current in-memory field values. No network request. Sections are
shown or hidden immediately.

The client does **not** touch `required` or any other field properties when a section is
shown/hidden. The server is the authority on which fields are required; submit
validation handles this.

---

## 3. Validate event

Sent when a field with `validateOn: "blur"` loses focus (i.e., on the `blur` event).

### 3.1 Request

```http
POST /heimdal/{formId}/event
Content-Type: application/json

{
  "type":   "validate",
  "field":  "notes",
  "seq":    3,
  "values": {
    "name":              "Trek Marlin",
    "bikeType":          "MOUNTAIN",
    "suspensionTravel":  "120",
    "notes":             "too short"
  }
}
```

```
type     "validate" (the only event type in v0)
field    the name of the field that triggered this event
seq      monotonically increasing integer; the client increments this counter per request
values   snapshot of all current form field values, as strings
```

All field values are sent regardless of section visibility. The server re-derives
visibility from the values and ignores hidden fields' validation accordingly.

Values are always strings. The server converts them to the appropriate Java types using
the same Ran serialization it used to render the form initially.

Fields with no value (empty) are sent as empty string `""`, not omitted.

### 3.2 Response

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "seq":    3,
  "errors": {
    "notes": ["Must be at least 10 characters"]
  }
}
```

```
seq     echoes the request seq; see §5 for sequencing rules
errors  object keyed by field name; value is array of error message strings
        a field present with an empty array means "clear any existing error"
        a field absent means "do not touch its error state"
```

The client applies errors from the response to the named fields. It calls a method on
the web component (e.g. `field.setErrors(["..."])`) — the component owns its own error
display. Clearing: `field.setErrors([])`.

Only the fields listed in `validates` for the triggering field will appear in the
response. Other fields are not touched.

If a validate event arrives for a field that has no known errors and the server response
also has no errors for it, the response `errors` object will still include the field
with an empty array, to make clear the server ran validation and it passed.

---

## 4. Submit

### 4.1 Request

Triggered by the user clicking the submit button (or pressing Enter in a single-line field,
per normal browser behavior on a `<form>`). The client intercepts the submit event,
prevents the default, and sends:

```http
POST /heimdal/{formId}/submit
Content-Type: application/json

{
  "seq":    7,
  "values": {
    "name":             "Trek Marlin",
    "bikeType":         "MOUNTAIN",
    "suspensionTravel": "120",
    "notes":            ""
  }
}
```

Same shape as the validate event values. Same string-everything rule.
`seq` is incremented from the same counter as validate events.

### 4.2 Response — validation errors

```http
HTTP/1.1 422 Unprocessable Entity
Content-Type: application/json

{
  "errors": {
    "notes":            [],
    "name":             [],
    "bikeType":         [],
    "suspensionTravel": []
  }
}
```

On a submit response, `errors` is **exhaustive**: every field that was validated appears,
either with messages (failed) or with an empty array (passed). The client applies all of
them, which clears any stale errors from earlier validate events.

The client does not navigate. The user stays on the form and sees the errors.

### 4.3 Response — success

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "redirect": "/bikes/42"
}
```

The client performs `window.location.assign(redirect)`. Navigation is a full page load
(MPA model). In v0 there are no partial-page transitions.

---

## 5. Sequence numbering

Every request carries a `seq` integer. The client starts at `1` and increments for each
request (validate and submit share the same counter).

Every successful response echoes `seq` (for submit responses, `seq` is not included —
success redirects immediately so there's nothing to discard).

The client discards any response whose `seq` is less than the highest `seq` it has
already processed. This guards against a slow validate response arriving after a faster
subsequent one.

In practice, validate events per field are debounced or serialised naturally (blur
doesn't fire again until the user returns to the field), so actual out-of-order
responses are rare. The `seq` check is cheap insurance.

---

## 6. `<hm-form>` responsibilities (summary)

`<hm-form>` is the only custom element the framework provides. It is the entire client
runtime for v0. Field components (`<hm-text-field>` etc.) are provided by the frontend
team and are opaque to the framework.

**On `connectedCallback`:**
- Find the `<script type="application/json">` child and parse it.
- For each item in `items`:
  - If it has `component`: create the element via `document.createElement(component)`,
    set `name`, `label`, `value`, `required`, and any extra keys as properties. Append.
  - If it has `section`: create a wrapper element, evaluate `visibleWhen` with initial
    field values, set/clear `hidden`. Recursively create nested field items. Append.
- For each action, create the appropriate button. Append.
- Attach a `change` listener on every created field element.
- Attach a `blur` listener on every field whose definition has `validateOn`.
- Intercept the `submit` event on itself (or on the submit button).

**On field `change`:**
- Re-evaluate all section `visibleWhen` predicates; toggle `hidden` on affected section
  wrappers.

**On field `blur` (if `validateOn` is set):**
- Collect all field values (by reading `value` from each created field element).
- POST to `eventEndpoint` with `type: "validate"`, `field`, `seq`, `values`.
- On response: discard if `seq` is stale; otherwise call `setErrors` on each named element.

**On submit:**
- Collect all field values.
- POST to `submitEndpoint` with `seq`, `values`.
- On 422: call `setErrors` on every field in the exhaustive error map.
- On 200: `window.location.assign(redirect)`.

**Not in v0:**
- Update events (server-driven structural changes)
- Loading/spinner state during in-flight requests
- Retry on network failure
- Optimistic updates
- Morphing (partial DOM patching)

---

## 7. Web component contract

The client runtime interacts with field components through this minimal interface.
Every `<hm-*-field>` must implement it.

```
value          getter   returns current value as a string
setErrors(msg: string[])   sets or clears the field's inline error display;
                           empty array = clear errors
```

The runtime reads `value` when collecting form values for requests.
The runtime calls `setErrors` when applying error responses.

This is the complete contract for v0. The runtime does not touch internal component
state for anything else.

---

## 8. Server responsibilities (summary)

**On validate event:**
- Deserialize values using Ran mappings.
- Derive section visibility from the values (same logic used at render time).
- Run validators only for the fields listed in `validates` for the triggering field,
  and only if those fields belong to a visible section.
- Return `{ seq, errors }`.

**On submit:**
- Deserialize values using Ran mappings.
- Derive section visibility.
- Run all validators for all visible fields.
- If any errors: return 422 with exhaustive `errors` object.
- If valid: execute submit handler; return 200 with `redirect`.

**On any request:**
- The server is stateless. It re-derives everything from the submitted values.
- No session, no stored form state.

---

## 9. What is explicitly out of scope for v0

These are not "future work" items — they are excluded to keep v0 implementable and
testable quickly. Each will be a separate design document when the time comes.

| Feature | Why deferred |
|---|---|
| `update` events (server-driven structural changes) | Section visibility handled client-side in v0; server-driven updates needed only when visibility depends on server context (permissions, DB lookups) |
| Schema updates mid-session | No `update` events means no mid-session schema changes |
| Loading indicators | Adds client complexity; acceptable to skip for first prototype |
| Retry / offline handling | Same |
| Optimistic updates | Only needed for toggle/list-reorder patterns; not in the simple form case |
| `save draft` action | Second action type; the protocol can express it but the design needs a defined response shape |
| Multi-section forms with inter-section dependencies | V0 has one conditional section; multiple interacting sections add predicate complexity |
| File upload fields | Separate upload protocol; out of scope |
| CSRF protection | Must be addressed before any production use; deferred here to keep the protocol doc focused |
