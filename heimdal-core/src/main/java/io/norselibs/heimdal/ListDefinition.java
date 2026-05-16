package io.norselibs.heimdal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ListDefinition<T> {
    private final ListBuilder<T> builder;

    ListDefinition(ListBuilder<T> builder) {
        this.builder = builder;
    }

    /**
     * Produces the JSON embedded in the page shell.
     *
     * Wire format:
     * <pre>
     * {
     *   "listId": "lst-bikes",
     *   "columns": [ { "name", "label", "component" }, ... ],
     *   "rows":    [ { "name": "Trek", ..., "_rowActions": [...] }, ... ],
     *   "actions": [ { "label", "url" }, ... ],
     *   "pagination": null    // stubbed — reserved for future sort/page support
     * }
     * </pre>
     */
    public Map<String, Object> toJson(String listId) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("listId", listId);

        json.put("columns", builder.columns.stream()
                .map(c -> {
                    Map<String, Object> col = new LinkedHashMap<>();
                    col.put("name",      c.name());
                    col.put("label",     c.label());
                    col.put("component", c.component());
                    // sortable: false — reserved for future sorting support
                    col.put("sortable",  false);
                    return col;
                })
                .collect(Collectors.toList()));

        json.put("rows", builder.items.stream()
                .map(item -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (ColumnDef<T> col : builder.columns) {
                        row.put(col.name(), col.serializedValue(item));
                    }
                    if (!builder.rowActions.isEmpty()) {
                        List<Map<String, String>> links = new ArrayList<>();
                        for (RowActionDef<T> ra : builder.rowActions) {
                            links.add(Map.of("label", ra.label(), "url", ra.url(item)));
                        }
                        row.put("_rowActions", links);
                    }
                    return row;
                })
                .collect(Collectors.toList()));

        json.put("actions", builder.actions.stream()
                .map(a -> Map.of("label", a.label(), "url", a.url()))
                .collect(Collectors.toList()));

        json.put("pagination", null); // reserved

        return json;
    }
}
