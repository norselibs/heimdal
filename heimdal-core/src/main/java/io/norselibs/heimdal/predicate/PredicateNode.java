package io.norselibs.heimdal.predicate;

import java.util.Map;

public interface PredicateNode {
    Map<String, Object> toJson();

    /** Evaluates the predicate against the supplied field values (all strings). */
    boolean evaluate(Map<String, String> values);
}
