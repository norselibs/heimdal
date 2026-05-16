package io.norselibs.heimdal;

public enum UpdateTrigger {
    /** Fire immediately when the field value changes. Good for fast server responses. */
    CHANGE,
    /** Fire when the field loses focus. Better when the round-trip is expensive. */
    BLUR
}
