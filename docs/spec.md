# Heimdal — Framework Specification

## What it is

Heimdal is a Java UI framework for building typed, declarative forms, lists, and detail pages. It is the UI layer in an opinionated stack alongside Ran (model introspection) and Valqueries (persistence). The three together are a "Spring alternative" — a full backend-to-frontend stack for Java teams who want to build line-of-business applications without investing in a separate frontend discipline.

## The problem it solves

In most companies building internal tools, B2B SaaS, or line-of-business apps, 90% of the team are backend developers who understand the domain model and can articulate what a page should do. The remaining 10% are frontend developers who spend most of their time on individual pages rather than building reusable components.

Heimdal inverts this: frontend developers build and own a typed component vocabulary (as web components). Backend developers compose pages from that vocabulary using the same fluent, method-reference-driven style they already use in Valqueries. Neither team has to work in the other's domain.

## What it is not for

- Consumer products where the UI itself is the differentiator (Instagram, Figma, Linear)
- Applications that need sub-100ms interactions without any server involvement
- Pages that aggregate data across many entities in complex custom ways (heavy reporting dashboards)
- Real-time collaborative editing

## Architecture

### Runtime model

Heimdal uses an MPA-shaped baseline with selective server-driven partial updates, not a client-side SPA.

**What lives on the server:**
- All visibility logic (which sections/fields are shown)
- All validation logic
- Cross-component state coordination
- Action enablement
- Business logic on submit

**What lives on the client:**
- Current input values — what the user has typed, before submit
- Knowing which interactions to notify the server about (declared in the initial form schema)
- Applying DOM patches when the server responds
- Local evaluation of simple predicates (where possible — see hybrid evaluation below)

**What this avoids:**
- Per-user server sessions (stateless server — each event request is independent)
- Hydration (no client-side component re-initialization)
- Shipping Java validation logic to the browser as compiled JS

### Integration with Ran

Heimdal is built directly on Ran's primitives (`Property`, `Token`, proxy interception). It is a sibling of Valqueries, not an extension. A team using Hibernate or hand-rolled JDBC can use Heimdal on their existing Ran-annotated POJOs. Neither layer requires the other.

The same `Property` objects that describe database columns describe form fields. The same `Token` system that produces `snake_case` column names produces `camelCase` JSON keys and `kebab-case` HTML attribute names. There are no duplicate registrations, no mapping layers, no string field names.

### Wire protocol

#### Initial page load

The server responds with HTML containing the rendered form and an embedded JSON schema:

```html
<form data-heimdal-form id="claim-42">
  <select-input name="claimType" data-triggers="update"></select-input>
  <date-input name="incidentDate" data-triggers="validate"></date-input>
  <textarea-input name="description"></textarea-input>

  <script type="application/json" data-form-schema>
  {
    "formId": "claim-42",
    "endpoint": "/heimdal/claim-42/event",
    "fields": {
      "claimType":    { "triggers": ["update"] },
      "incidentDate": { "triggers": ["validate-on-blur"], "validates": ["incidentDate"] },
      "contactEmail": { "triggers": ["validate-on-blur"], "validates": ["contactEmail"] },
      "description":  { "triggers": [] },
      "estimatedAmount": {
        "triggers": [],
        "visibleWhen": { "type": "in", "field": "claimType", "values": ["AUTO", "HOME"] }
      }
    },
    "submitEndpoint": "/heimdal/claim-42/submit"
  }
  </script>
</form>
```

Key points:
- Most fields have no triggers — the client never bothers the server about them until submit.
- `update` is the expensive event (structural changes: visibility, dynamic options). Rare per form.
- `validate` is the cheap event (error recomputation only). Returns only error messages.
- Simple visibility predicates are included as serialized JSON so the client can evaluate them locally without a round trip.

#### Event request

```http
POST /heimdal/claim-42/event
Content-Type: application/json

{
  "event": "update",
  "field": "claimType",
  "values": {
    "claimType": "AUTO",
    "incidentDate": "",
    "description": "",
    ...
  }
}
```

All current form values are sent (server is stateless). A sequence number guards against out-of-order responses.

#### Event response

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "patches": [
    { "op": "show", "selector": "[data-section='auto']" },
    { "op": "hide", "selector": "[data-section='home']" },
    { "op": "replace", "selector": "[data-section='auto']",
      "html": "<section data-section='auto'>...</section>" }
  ],
  "schemaUpdates": {
    "fields": {
      "vehicleVin": { "triggers": ["validate-on-blur"], "validates": ["vehicleVin"] }
    }
  },
  "errors": {}
}
```

For `validate` events the patches array is empty — only `errors` changes.

#### Hybrid predicate evaluation

Simple predicates (field equals a constant, field is in a set) are serialized in the schema and evaluated client-side. The client updates visibility without a round trip. Complex predicates that depend on external context (database lookups, user authority, related entities) stay server-side. The framework decides per-predicate by walking the predicate tree at form-build time. Developers see no difference in the DSL.

#### DOM morphing

Partial re-renders use a morphing library (idiomorph or equivalent) rather than innerHTML replacement. This preserves the user's typed-but-not-blurred values, focus state, and scroll position during updates.

### Web components

Frontend developers build the component vocabulary as custom elements. Rich widgets (date pickers, file uploaders, rich text) implement the Form-Associated Custom Elements API so the framework treats them as standard fields with a typed value. The Java side never touches component internals.

## Developer-facing API

### Form definition

```java
public PageDefinition<Claim> definition(Claim draft, User user) {
    return Page.form(Claim.class, draft)

        .section("Claimant", section -> section
            .field(Claim::getClaimantName).readonly()
            .field(Claim::getPolicyNumber).readonly()
            .field(Claim::getContactEmail)
                .required()
                .validate(Validators.email())
        )

        .section("Claim Details", section -> section
            .field(Claim::getClaimType)
                .required()
                .triggersUpdate()

            .field(Claim::getIncidentDate)
                .required()
                .validate(q -> q.lte(Claim::getIncidentDate, LocalDate.now()),
                          "Cannot be in the future")
                .validate(q -> q.gte(Claim::getIncidentDate,
                                     Claim::getPolicy, Policy::getStartDate),
                          "Cannot be before policy start date")

            .when(q -> q.eq(Claim::getClaimType, ClaimType.AUTO), auto -> auto
                .field(Claim::getVehicleVin).required()
                .field(Claim::getAccidentLocation).required()
            )
            .when(q -> q.eq(Claim::getClaimType, ClaimType.HOME), home -> home
                .field(Claim::getPropertyAddress).required()
                .field(Claim::getDamageType).required()
            )
            .when(q -> q.eq(Claim::getClaimType, ClaimType.HEALTH), health -> health
                .field(Claim::getProvider).required()
                .field(Claim::getDiagnosisCode).required()
            )
            .when(q -> q.eq(Claim::getClaimType, ClaimType.TRAVEL), travel -> travel
                .field(Claim::getDestination).required()
                .field(Claim::getTripStartDate).required()
            )

            .field(Claim::getDescription)
                .required()
                .multiline()
                .minLength(50)

            .when(q -> q.in(Claim::getClaimType, ClaimType.AUTO, ClaimType.HOME),
                  photos -> photos
                .field(Claim::getPhotos)
                    .maxFiles(10)
                    .accept("image/*")
            )

            // ctx-based predicate: evaluated server-side at form-build time, never round-trips
            .when(ctx -> ctx.eq(user.authority(), Authority.STANDARD),
                  amount -> amount
                .field(Claim::getEstimatedAmount).currency("USD")
            )
        )

        .actions(actions -> actions
            .saveDraft(draft -> claimService.saveDraft(draft))

            .submit(draft -> claimService.submit(draft))
                .enabledWhen(q -> q.allRequiredFieldsValid())
                .onError(DuplicateClaimException.class, (e, errors) ->
                    errors.field(Claim::getPolicyNumber,
                                 "A claim already exists for this incident"))
        );
}
```

### Routing

```java
@Page(path = "/claims/new")
public PageDefinition<Claim> newClaim(@Authenticated User user,
                                       @RequestParam UUID policyId) {
    Policy policy = policyService.findById(policyId);
    Claim draft = new Claim();
    draft.setPolicyId(policy.getId());
    draft.setPolicyNumber(policy.getNumber());
    draft.setClaimantId(user.getId());
    draft.setClaimantName(user.getName());
    return claimForm.definition(draft, user);
}
```

### Predicate algebra

Conditions for visibility, validation, and action enablement use the same `q -> q.eq(...)` shape as Valqueries queries. Two contexts exist:

**Form-data predicates** (`q -> q.eq(Claim::getField, value)`) — the field reference is recorded via Ran's proxy interception. The framework walks the resulting typed tree to:
1. Derive the dependency graph for the wire schema (which field changes trigger re-evaluation)
2. Serialize the predicate to JSON for optional client-side evaluation
3. Evaluate the predicate server-side when needed

**Context predicates** (`ctx -> ctx.eq(user.authority(), Authority.STANDARD)`) — accept values from the surrounding Java scope. Evaluated once at form-build time and baked into the rendered HTML. Never generate wire-protocol entries because there is no client-side equivalent.

Both types compose with `or`, `and`, `not`, `in`, nested closures — the same algebra as Valqueries.

### Dependency derivation

The framework derives the schema's trigger/validates dependency graph automatically by walking the predicate trees. No `.dependsOn()` declarations required. When a validator reads `Claim::getIncidentDate` and `Policy::getStartDate`, the framework knows that changing `incidentDate` should re-run that validator. When a section is `visibleWhen(q -> q.eq(Claim::getClaimType, AUTO))`, the framework knows `claimType` triggering `update` must re-evaluate that section.

The `.triggersUpdate()` call remains for fields whose change should cause a full structural update (visibility of sections). For most fields the framework can derive this; for explicitly consequential fields, the declaration makes intent visible.

## Key design decisions

See `docs/decisions/` for detailed rationale. Summary:

- **Stateless server over sessions**: No sticky sessions, no server memory per user. Each event request carries full form state. Scales horizontally.
- **Server-owns-logic over client predicates**: Validation rules, visibility logic, and permissions stay in Java. No Java-to-JS compiler needed. One source of truth.
- **Predicate algebra over lambdas**: Typed predicate objects (introspectable, serializable) instead of opaque Java lambdas. Enables hybrid client/server evaluation and automatic dependency derivation.
- **Web components over proprietary component model**: Frontend team owns component internals. Framework-agnostic rendering target. Components outlive the framework.
- **Method references over string field names**: Refactor-safe. IDE autocompletion. Type-driven renderer dispatch. Same as Valqueries.
- **Ran as substrate, not Valqueries**: Works on any POJO. No forced persistence layer. Valqueries users get natural alignment as a benefit, not a requirement.

## Scope for v0

A working v0 should demonstrate the full request-to-DOM loop for:
1. A simple form with a few fields and one conditional section
2. A list page with typed column projection
3. Navigation between them with typed page references

Explicitly deferred:
- A polished component library / design system
- Optimistic updates (toggling, drag-drop reorder)
- File upload progress tracking
- Multi-step wizards
- Real-time collaboration

## Build order recommendation

1. **Wire protocol first** — define the event/response format before the DSL. The DSL should be a function of what it has to produce, not the other way around.
2. **End-to-end loop on one simple form** — definition → schema → HTML → user event → patch response → DOM update. Validate the whole loop works before adding DSL surface area.
3. **Complex form second** — apply the insurance claim form to find where the DSL strains. Fix the strains before generalizing.
4. **List and navigation third** — extend the loop to lists and typed page links.
5. **Component library last** — only after the architecture is proven. Start with native HTML inputs.
