package io.norselibs.heimdal;

import java.util.Map;

/** Config terminal for a single column in a collection field. */
public class CollectionColumnDef<I> {
    private final Map<String, Object> col;

    CollectionColumnDef(Map<String, Object> col) {
        this.col = col;
    }

    public CollectionColumnDef<I> label(String label) {
        col.put("label", label);
        return this;
    }

    public CollectionColumnDef<I> required() {
        col.put("required", true);
        return this;
    }
}
