package io.norselibs.heimdal;

import io.norselibs.heimdal.predicate.PredicateNode;

import java.util.LinkedHashMap;
import java.util.Map;

class FormActionDef {
    final String label;
    final String url;
    final boolean isLink; // true → GET <a> navigation, false → POST <button> submit
    PredicateNode enabledWhen; // null = always enabled (only meaningful for submit buttons)

    FormActionDef(String label, String url) {
        this(label, url, false);
    }

    FormActionDef(String label, String url, boolean isLink) {
        this.label = label;
        this.url = url;
        this.isLink = isLink;
    }

    Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", isLink ? "link" : "submit");
        m.put("label", label);
        m.put("url", url);
        if (!isLink && enabledWhen != null) m.put("enabledWhen", enabledWhen.toJson());
        return m;
    }
}
