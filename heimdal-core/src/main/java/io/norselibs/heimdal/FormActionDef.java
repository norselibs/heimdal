package io.norselibs.heimdal;

import io.norselibs.heimdal.predicate.PredicateNode;

import java.util.LinkedHashMap;
import java.util.Map;

class FormActionDef {
    final String label;
    final String url;
    PredicateNode enabledWhen; // null = always enabled

    FormActionDef(String label, String url) {
        this.label = label;
        this.url = url;
    }

    Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "submit");
        m.put("label", label);
        m.put("url", url);
        if (enabledWhen != null) m.put("enabledWhen", enabledWhen.toJson());
        return m;
    }
}
