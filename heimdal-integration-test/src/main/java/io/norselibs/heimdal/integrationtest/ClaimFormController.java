package io.norselibs.heimdal.integrationtest;

import io.norselibs.heimdal.Form;
import io.norselibs.heimdal.FormDefinition;
import io.norselibs.heimdal.VarHeimdal;
import io.norselibs.heimdal.Validators;
import io.varhttp.Controller;
import io.varhttp.ControllerClass;
import io.varhttp.HttpMethod;
import io.varhttp.PathVariable;
import io.varhttp.RequestBody;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Insurance claim form — integration test for complex form scenarios.
 *
 * GAP NOTES (features needed but not yet implemented):
 *
 * 1. Predicate validators — spec shows:
 *      .validate(q -> q.lte(Claim::getIncidentDate, LocalDate.now()), "Cannot be in the future")
 *    Currently approximated with plain Validator lambdas. Need a
 *    .validate(Consumer<Q<T>>, String) overload that captures a PredicateNode and
 *    evaluates it against deserialized form values.
 *
 * 2. Cross-field predicate validators — spec shows:
 *      .validate(q -> q.gte(Claim::getIncidentDate, Claim::getPolicy, Policy::getStartDate), "...")
 *    Requires multi-field deserialization and nested property access. Deferred.
 *
 * 3. Context predicates — spec shows:
 *      .when(ctx -> ctx.eq(user.authority(), Authority.STANDARD), amount -> ...)
 *    Server-side-only evaluation at form-build time. Deferred.
 *    Approximated here by always showing the amount field.
 *
 * 4. Actions — spec shows saveDraft / submit with enabledWhen / onError. Deferred.
 *
 * 5. File upload — spec shows Claim::getPhotos with .maxFiles(10). Deferred.
 */
@ControllerClass
public class ClaimFormController {

    private static final AtomicInteger nextId = new AtomicInteger(1);
    private static final List<Claim> CLAIMS = seedClaims();

    // -------------------------------------------------------------------------
    // List
    // -------------------------------------------------------------------------

    @Controller(path = "/claims")
    public Object list(VarHeimdal vh) throws Exception {
        return vh.list(Claim.class, CLAIMS,
                l -> l.column(Claim::getClaimantName),
                l -> l.column(Claim::getClaimType),
                l -> l.column(Claim::getIncidentDate),
                l -> l.column(Claim::getDescription).label("Description"),
                l -> l.action("New claim", "/claims/new"),
                l -> l.rowAction("Edit", claim -> "/claims/" + claim.getId() + "/edit")
        );
    }

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    @Controller(path = "/claims/new")
    public Object newClaim(VarHeimdal vh) throws Exception {
        Claim draft = seedDraft();
        return buildForm(vh, draft, "/claims/save");
    }

    @Controller(path = "/claims/save", httpMethods = HttpMethod.POST)
    public Object saveClaim(@RequestBody Claim claim, VarHeimdal vh) throws Exception {
        return vh.save(claim, actionDef(claim, "/claims/save"), this::submitClaim, "/claims");
    }

    private void submitClaim(Claim claim) throws DuplicateClaimException {
        boolean duplicate = CLAIMS.stream()
                .anyMatch(c -> c.getPolicyNumber() != null
                        && c.getPolicyNumber().equals(claim.getPolicyNumber()));
        if (duplicate) throw new DuplicateClaimException(claim.getPolicyNumber());
        claim.setId(nextId.getAndIncrement());
        CLAIMS.add(claim);
    }

    // -------------------------------------------------------------------------
    // Edit
    // -------------------------------------------------------------------------

    @Controller(path = "/claims/{id}/edit")
    public Object editClaim(@PathVariable(name = "id") int id, VarHeimdal vh) throws Exception {
        return buildForm(vh, findById(id), "/claims/" + id + "/save");
    }

    @Controller(path = "/claims/{id}/save", httpMethods = HttpMethod.POST)
    public Object updateClaim(@PathVariable(name = "id") int id,
                               @RequestBody Claim claim, VarHeimdal vh) throws Exception {
        return vh.save(claim, actionDef(claim, "/claims/" + id + "/save"),
                c -> { c.setId(id); CLAIMS.replaceAll(e -> e.getId() == id ? c : e); },  // no checked exception
                "/claims");
    }

    // -------------------------------------------------------------------------
    // Form definition
    // -------------------------------------------------------------------------

    // Simulate user authority — in a real app this comes from the request context
    private static final boolean IS_STANDARD_USER = true;

    /**
     * Builds a FormDefinition carrying only the action builders and their onError handlers.
     * Used by save handlers so vh.save() can resolve domain exceptions to field errors.
     * The proxy instance is still needed (for FieldErrors getter → name resolution).
     */
    private FormDefinition<Claim> actionDef(Claim claim, String submitUrl) {
        var hm = Form.of(Claim.class, claim);
        hm.action("Submit Claim", submitUrl)
          .enabledWhen(q -> q.allRequiredFieldsValid())
          .onError(DuplicateClaimException.class, (e, err) ->
              err.field(Claim::getPolicyNumber, "A claim already exists for this policy"));
        return hm.build();
    }

    private Object buildForm(VarHeimdal vh, Claim claim, String submitUrl) throws Exception {
        return vh.form(Claim.class, claim, submitUrl,

            // --- Claimant section (always visible, no predicate) ---
            // GAP 3: spec uses a context predicate (.when(ctx -> ctx.eq(user.authority(), ...)))
            // to conditionally show fields. Here we just show them all.
            f -> f.section("Claimant", s -> {
                s.textField(Claim::getClaimantName).readonly();
                s.textField(Claim::getPolicyNumber).readonly();
                s.textField(Claim::getContactEmail).required()
                 .validate(Validators.email());
            }),

            // --- Claim type and incident date ---
            // triggersUpdate causes the server to re-evaluate all section predicates
            // when claimType changes. Useful when predicates involve server-side logic
            // (database checks, permissions) that the client can't evaluate locally.
            f -> f.field(Claim::getClaimType).required().triggersUpdate(),

            f -> f.dateField(Claim::getIncidentDate).required()
                   .validate("Cannot be in the future",
                             q -> q.lte(Claim::getIncidentDate, LocalDate.now())),
                   // GAP 2: cross-field predicate (deferred):
                   //   .validate("Cannot be before policy start date",
                   //             q -> q.gte(Claim::getIncidentDate, Claim::getPolicy, Policy::getStartDate))

            // --- Conditional sections per claim type ---
            f -> f.section("Auto Details",
                    q -> q.eq(Claim::getClaimType, ClaimType.AUTO), s -> {
                        s.textField(Claim::getVehicleVin).required();
                        s.textField(Claim::getAccidentLocation).required();
                    }),

            f -> f.section("Property Details",
                    q -> q.eq(Claim::getClaimType, ClaimType.HOME), s -> {
                        s.textField(Claim::getPropertyAddress).required();
                        s.field(Claim::getDamageType).required();
                    }),

            f -> f.section("Health Details",
                    q -> q.eq(Claim::getClaimType, ClaimType.HEALTH), s -> {
                        s.textField(Claim::getProvider).required();
                        s.textField(Claim::getDiagnosisCode).required();
                    }),

            f -> f.section("Travel Details",
                    q -> q.eq(Claim::getClaimType, ClaimType.TRAVEL), s -> {
                        s.textField(Claim::getDestination).required();
                        s.dateField(Claim::getTripStartDate).required();
                    }),

            // --- Description (always shown) ---
            f -> f.textareaField(Claim::getDescription).required()
                   .minLength(50),  // spec: .minLength(50) — now a FieldBuilder shorthand

            // GAP 5: file upload — spec shows Claim::getPhotos with .maxFiles(10). Omitted.

            // Inline editable list — witnesses
            f -> f.collectionField(Claim::getWitnesses, Witness.class, c -> {
                c.column(Witness::getName).label("Full Name");
                c.column(Witness::getPhone).label("Phone");
            }),

            // Context predicate: only shown for standard users (evaluated server-side at build time)
            f -> f.when(IS_STANDARD_USER, s -> s.decimalField(Claim::getEstimatedAmount)),

            // Actions
            f -> f.action("Save Draft", submitUrl),
            f -> f.action("Submit Claim", submitUrl)
                   .enabledWhen(q -> q.allRequiredFieldsValid())
        );
    }

    // -------------------------------------------------------------------------

    private static Claim findById(int id) {
        return CLAIMS.stream().filter(c -> c.getId() == id).findFirst()
                .orElseThrow(() -> new RuntimeException("Claim not found: " + id));
    }

    private static Claim seedDraft() {
        Claim c = new Claim();
        c.setClaimantName("Jane Smith");
        c.setPolicyNumber("POL-2024-001");
        return c;
    }

    private static List<Claim> seedClaims() {
        List<Claim> list = new ArrayList<>();
        Claim c1 = seedDraft();
        c1.setId(1);
        c1.setClaimType(ClaimType.AUTO);
        c1.setIncidentDate(LocalDate.of(2024, 3, 15));
        c1.setContactEmail("jane@example.com");
        c1.setVehicleVin("1HGCM82633A004352");
        c1.setAccidentLocation("Main St & 5th Ave");
        c1.setDescription("Rear-ended at a traffic light. Significant damage to the rear bumper and trunk.");
        list.add(c1);
        nextId.set(2);
        return list;
    }
}
