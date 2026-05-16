package io.norselibs.heimdal;

/**
 * A navigation menu item.
 *
 * <pre>
 * VarHeimdal.addMenuItem("Bikes",  "/bikes");
 * VarHeimdal.addMenuItem("Claims", "/claims", "&lt;span&gt;📋&lt;/span&gt;");
 * </pre>
 *
 * {@code iconHtml} is arbitrary HTML injected before the label — use an emoji,
 * an {@code <img>}, a Material Icons {@code <i>} element, or any inline element.
 */
public record MenuItem(String label, String url, String iconHtml) {

    public static MenuItem of(String label, String url) {
        return new MenuItem(label, url, null);
    }

    public static MenuItem of(String label, String url, String iconHtml) {
        return new MenuItem(label, url, iconHtml);
    }

    boolean isActive(String currentPath) {
        return currentPath.equals(url)
                || currentPath.startsWith(url + "/");
    }
}
