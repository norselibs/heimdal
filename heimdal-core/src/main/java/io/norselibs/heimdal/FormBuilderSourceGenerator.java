package io.norselibs.heimdal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FormBuilderSourceGenerator {

    // Language-agnostic type name → Java info
    record TypeMapping(String simpleType, String fqType, String deserializer) {}

    static final Map<String, TypeMapping> TYPES = new LinkedHashMap<>();
    static {
        TYPES.put("string",   new TypeMapping("String",        "java.lang.String",       null));
        TYPES.put("integer",  new TypeMapping("Integer",       "java.lang.Integer",      "Integer::parseInt"));
        TYPES.put("long",     new TypeMapping("Long",          "java.lang.Long",         "Long::parseLong"));
        TYPES.put("decimal",  new TypeMapping("BigDecimal",    "java.math.BigDecimal",   "java.math.BigDecimal::new"));
        TYPES.put("boolean",  new TypeMapping("Boolean",       "java.lang.Boolean",      "Boolean::parseBoolean"));
        TYPES.put("date",     new TypeMapping("LocalDate",     "java.time.LocalDate",    "java.time.LocalDate::parse"));
        TYPES.put("datetime", new TypeMapping("LocalDateTime", "java.time.LocalDateTime","java.time.LocalDateTime::parse"));
    }

    // One entry per (component, type) pair derived from a JS file
    record ComponentEntry(String componentName, String heimdalType, String methodName,
                          boolean multiline, boolean register) {}

    // -------------------------------------------------------------------------
    // Entry point
    // -------------------------------------------------------------------------

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: FormBuilderSourceGenerator <outputDir> [extraJsDir...]");
            System.exit(1);
        }
        Path outputDir = Paths.get(args[0]);
        List<Path> extraDirs = new ArrayList<>();
        for (int i = 1; i < args.length; i++) extraDirs.add(Paths.get(args[i]));
        generate(outputDir, extraDirs);
    }

    public static void generate(Path outputDir, List<Path> extraDirs) throws Exception {
        List<ComponentEntry> entries = new ArrayList<>();

        // Scan all *.js files under static/heimdal/ from every entry on the classpath.
        // Covers both JARs (e.g. heimdal-core.jar) and resource directories.
        for (String cpEntry : System.getProperty("java.class.path", "").split(File.pathSeparator)) {
            if (cpEntry.isEmpty()) continue;
            Path p = Paths.get(cpEntry);
            if (cpEntry.endsWith(".jar") && Files.exists(p)) {
                entries.addAll(scanJar(p));
            } else if (Files.isDirectory(p)) {
                entries.addAll(scanDir(p.resolve("static/heimdal")));
            }
        }

        // Scan any extra project JS source directories passed as arguments.
        for (Path dir : extraDirs) {
            entries.addAll(scanDir(dir));
        }

        if (entries.isEmpty()) return;

        Path outFile = outputDir.resolve("io/norselibs/heimdal/Hm.java");
        Files.createDirectories(outFile.getParent());
        Files.writeString(outFile, buildSource(entries));
    }

    // -------------------------------------------------------------------------
    // JS parsing
    // -------------------------------------------------------------------------

    private static final Pattern CLASS_DECL     = Pattern.compile("\\bclass\\s+(\\w+)");
    private static final Pattern HEIMDAL_META   = Pattern.compile("static\\s+heimdal\\s*=\\s*\\{([^}]+)\\}");
    private static final Pattern TYPE_SINGLE    = Pattern.compile("\\btype\\s*:\\s*'([^']+)'");
    private static final Pattern TYPES_ARRAY    = Pattern.compile("\\btypes\\s*:\\s*\\[([^\\]]+)\\]");
    private static final Pattern MULTILINE_FLAG = Pattern.compile("\\bmultiline\\s*:\\s*true");
    private static final Pattern DEFAULT_TRUE   = Pattern.compile("\\bdefault\\s*:\\s*true");
    private static final Pattern ARRAY_ITEM     = Pattern.compile("'([^']+)'");
    private static final Pattern CUSTOM_ELEM    = Pattern.compile(
            "customElements\\.define\\s*\\(\\s*'([^']+)'\\s*,\\s*(\\w+)\\s*\\)");

    static List<ComponentEntry> parseJs(String content) {
        // Class name → heimdal metadata
        Map<String, List<String>> classTypes    = new LinkedHashMap<>();
        Map<String, Boolean>      classMultiline = new LinkedHashMap<>();
        Map<String, Boolean>      classDefault   = new LinkedHashMap<>();

        // Split on class declarations so metadata stays near its class
        String[] parts = content.split("(?=\\bclass\\s+\\w+)");
        for (String part : parts) {
            Matcher cls = CLASS_DECL.matcher(part);
            if (!cls.find()) continue;
            String className = cls.group(1);

            Matcher meta = HEIMDAL_META.matcher(part);
            if (!meta.find()) continue;
            String metaBody = meta.group(1);

            List<String> types = new ArrayList<>();
            Matcher arr = TYPES_ARRAY.matcher(metaBody);
            if (arr.find()) {
                Matcher item = ARRAY_ITEM.matcher(arr.group(1));
                while (item.find()) types.add(item.group(1));
            } else {
                Matcher single = TYPE_SINGLE.matcher(metaBody);
                if (single.find()) types.add(single.group(1));
            }
            if (types.isEmpty()) continue;

            classTypes.put(className, types);
            classMultiline.put(className, MULTILINE_FLAG.matcher(metaBody).find());
            // default: true means "register as the canonical type mapping" — opt-in
            classDefault.put(className, DEFAULT_TRUE.matcher(metaBody).find());
        }

        // Map class name → component name
        Map<String, String> classToComponent = new LinkedHashMap<>();
        Matcher elem = CUSTOM_ELEM.matcher(content);
        while (elem.find()) classToComponent.put(elem.group(2), elem.group(1));

        // Build entries
        List<ComponentEntry> result = new ArrayList<>();
        for (var e : classTypes.entrySet()) {
            String className     = e.getKey();
            List<String> types   = e.getValue();
            String componentName = classToComponent.get(className);
            if (componentName == null) continue;
            boolean multiline = classMultiline.getOrDefault(className, false);
            boolean register  = classDefault.getOrDefault(className, true);

            if (types.size() == 1) {
                // Single type: method name from component name
                result.add(new ComponentEntry(componentName, types.get(0),
                        componentNameToMethodName(componentName), multiline, register));
            } else {
                // Multiple types: method name from type name to avoid ambiguity
                for (String type : types) {
                    result.add(new ComponentEntry(componentName, type, type + "Field", false, register));
                }
            }
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Source generation
    // -------------------------------------------------------------------------

    private static String buildSource(List<ComponentEntry> entries) {
        StringBuilder registrations = new StringBuilder();
        StringBuilder methods       = new StringBuilder();

        for (ComponentEntry e : entries) {
            TypeMapping tm = TYPES.get(e.heimdalType());
            if (tm == null) continue;  // unknown type — skip

            // Registration: skip for multiline variants (share primary type's registration)
            // and for default:false components (they're alternatives, not defaults).
            if (!e.multiline() && e.register()) {
                registrations.append("        ComponentRegistry.register(")
                        .append("ComponentRegistration.forType(").append(tm.fqType()).append(".class)")
                        .append(".component(\"").append(e.componentName()).append("\")");
                if (tm.deserializer() != null) {
                    registrations.append(".deserialize(").append(tm.deserializer()).append(")");
                }
                registrations.append(".build());\n");
            }

            // Typed method
            methods.append("\n    public FieldBuilder<T> ").append(e.methodName())
                    .append("(Function<T, ").append(tm.fqType()).append("> getter) {\n")
                    .append("        return field(getter)");
            if (e.multiline()) methods.append(".multiline()");
            // Non-default components must explicitly set their component name since they
            // aren't registered as the default for their Java type.
            else if (!e.register()) methods.append(".component(\"").append(e.componentName()).append("\")");
            methods.append(";\n    }\n");
        }

        return "package io.norselibs.heimdal;\n\n"
                + "import io.ran.Clazz;\n"
                + "import io.norselibs.heimdal.definition.SectionDefinition;\n"
                + "import java.util.function.Consumer;\n"
                + "import java.util.function.Function;\n\n"
                + "public class Hm<T> extends FormBuilder<T> {\n\n"
                + "    static {\n"
                + registrations
                + "    }\n\n"
                + "    public Hm(Clazz<T> clazz, T initialValue) {\n"
                + "        super(clazz, initialValue);\n"
                + "    }\n\n"
                + "    Hm(FormBuilder<T> parent) {\n"
                + "        super(parent);\n"
                + "    }\n\n"
                + "    public static <T> Hm<T> of(Class<T> clazz, T initialValue) {\n"
                + "        return new Hm<>(Clazz.of(clazz), initialValue);\n"
                + "    }\n\n"
                // Single-consumer overload (not varargs) — erasure is section(Consumer,Consumer)
                // which differs from FormBuilder's section(Consumer,Consumer[]), so no clash.
                // Java prefers non-varargs when both match, so f.section(pred, s -> ...) uses this
                // overload and types s as Hm<T>.
                + "    public Hm<T> section(\n"
                + "            Consumer<Q<T>> predicateConsumer,\n"
                + "            Consumer<Hm<T>> body) {\n"
                + "        return section(null, predicateConsumer, body);\n"
                + "    }\n\n"
                + "    public Hm<T> section(\n"
                + "            String label,\n"
                + "            Consumer<Q<T>> predicateConsumer,\n"
                + "            Consumer<Hm<T>> body) {\n"
                + "        var predicate = new Q<>(proxyInstance, queryWrapper);\n"
                + "        predicateConsumer.accept(predicate);\n"
                + "        var bodyBuilder = new Hm<>(this);\n"
                + "        body.accept(bodyBuilder);\n"
                + "        String sectionId = \"s\" + sectionCounter.getAndIncrement();\n"
                + "        items.add(new SectionDefinition(sectionId, label, predicate.build(), bodyBuilder.fieldItems()));\n"
                + "        return this;\n"
                + "    }\n"
                + methods
                + "}\n";
    }

    // "hm-price-field"  → "priceField"
    // "hm-photo-upload" → "photoUploadField"
    static String componentNameToMethodName(String componentName) {
        String s = componentName.startsWith("hm-") ? componentName.substring(3) : componentName;
        if (s.endsWith("-field")) s = s.substring(0, s.length() - 6);
        String[] parts = s.split("-");
        StringBuilder sb = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty())
                sb.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
        }
        return sb + "Field";
    }

    private static List<ComponentEntry> scanJar(Path jarPath) {
        List<ComponentEntry> result = new ArrayList<>();
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            jar.entries().asIterator().forEachRemaining(entry -> {
                if (entry.getName().startsWith("static/heimdal/") && entry.getName().endsWith(".js")) {
                    try (InputStream in = jar.getInputStream(entry)) {
                        result.addAll(parseJs(new String(in.readAllBytes(), StandardCharsets.UTF_8)));
                    } catch (IOException ignored) {}
                }
            });
        } catch (IOException ignored) {}
        return result;
    }

    private static List<ComponentEntry> scanDir(Path dir) {
        if (!Files.exists(dir)) return List.of();
        List<ComponentEntry> result = new ArrayList<>();
        try (var walk = Files.walk(dir)) {
            walk.filter(p -> p.toString().endsWith(".js"))
                    .forEach(p -> {
                        try { result.addAll(parseJs(Files.readString(p))); }
                        catch (IOException ignored) {}
                    });
        } catch (IOException ignored) {}
        return result;
    }
}
