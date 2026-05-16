package io.norselibs.heimdal.predicate;

import java.util.Map;

/**
 * Sentinel node used in action {@code enabledWhen} predicates.
 * The client evaluates whether all required fields have non-empty values;
 * server-side evaluation always returns true (actions aren't validated server-side).
 */
public class AllRequiredValidNode implements PredicateNode {

    public static final AllRequiredValidNode INSTANCE = new AllRequiredValidNode();

    private AllRequiredValidNode() {}

    @Override
    public boolean evaluate(Map<String, String> values) {
        return true; // evaluated client-side only
    }

    @Override
    public Map<String, Object> toJson() {
        return Map.of("op", "allRequiredValid");
    }
}
