package io.norselibs.heimdal;

/**
 * Returned by {@link ListBuilder#column}. Configures column-level presentation.
 *
 * <pre>
 * l -> l.column(Bike::getSuspensionTravel).label("Travel (mm)")
 * </pre>
 */
public class ColumnBuilder<T> {
    private final ListBuilder<T> list;
    private final ColumnDef<T> def;

    ColumnBuilder(ListBuilder<T> list, ColumnDef<T> def) {
        this.list = list;
        this.def = def;
    }

    public ColumnBuilder<T> label(String label) {
        def.labelOverride = label;
        return this;
    }
}
