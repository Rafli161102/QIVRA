package dev.qivra.analyzer;

import org.tomlj.Toml;
import org.tomlj.TomlArray;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class BuildModelIntelligence {
    private static final Pattern PROPERTY = Pattern.compile("\\$\\{([^}]+)}");
    private static final int MAX_PARENT_DEPTH = 16;
    private static final long MAX_PARENT_BYTES = 2_000_000L;

    private BuildModelIntelligence() {
    }

    static Map<String, Object> parseVersionCatalog(
            String path,
            String source,
            List<Map<String, Object>> diagnostics
    ) {
        TomlParseResult parsed = Toml.parse(source);
        List<String> errors = parsed.errors().stream().map(Object::toString).toList();
        if (parsed.hasErrors()) {
            diagnostics.add(diagnostic(
                    "GRADLE_CATALOG_TOML_ERROR",
                    String.join("; ", errors),
                    path
            ));
        }

        TomlTable versionsTable = table(parsed.get("versions"));
        TomlTable librariesTable = table(parsed.get("libraries"));
        TomlTable pluginsTable = table(parsed.get("plugins"));
        TomlTable bundlesTable = table(parsed.get("bundles"));

        Map<String, String> versions = new TreeMap<>();
        if (versionsTable != null) {
            for (String alias : sorted(versionsTable.keySet())) {
                Object value = versionsTable.get(alias);
                String version = value instanceof String text
                        ? text
                        : firstString(table(value), "require", "strictly", "prefer");
                if (version != null) {
                    versions.put(alias, version);
                }
            }
        }

        List<Map<String, Object>> libraries = new ArrayList<>();
        if (librariesTable != null) {
            for (String alias : sorted(librariesTable.keySet())) {
                Object value = librariesTable.get(alias);
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("alias", alias);
                String module = null;
                String group = null;
                String artifact = null;
                String version = null;
                String versionRef = null;
                if (value instanceof String coordinate) {
                    module = coordinate;
                    String[] parts = coordinate.split(":", 3);
                    if (parts.length >= 2) {
                        group = parts[0];
                        artifact = parts[1];
                    }
                    if (parts.length == 3) {
                        version = parts[2];
                    }
                } else {
                    TomlTable entry = table(value);
                    if (entry != null) {
                        module = safeGetString(entry, "module");
                        group = safeGetString(entry, "group");
                        artifact = safeGetString(entry, "name");
                        if (module != null) {
                            String[] parts = module.split(":", 2);
                            if (parts.length == 2) {
                                group = parts[0];
                                artifact = parts[1];
                            }
                        } else if (group != null && artifact != null) {
                            module = group + ":" + artifact;
                        }
                        Object versionValue = entry.get("version");
                        if (versionValue instanceof String text) {
                            version = text;
                        } else if (versionValue instanceof TomlTable versionTable) {
                            version = firstString(versionTable, "require", "strictly", "prefer");
                            versionRef = safeGetString(versionTable, "ref");
                        }
                    }
                }
                if (version == null && versionRef != null) {
                    version = versions.get(versionRef);
                }
                item.put("module", module);
                item.put("group", group);
                item.put("artifact", artifact);
                item.put("version", version);
                item.put("version_ref", versionRef);
                item.put("version_resolved", version != null);
                libraries.add(item);
            }
        }

        List<Map<String, Object>> plugins = new ArrayList<>();
        if (pluginsTable != null) {
            for (String alias : sorted(pluginsTable.keySet())) {
                Object value = pluginsTable.get(alias);
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("alias", alias);
                if (value instanceof String pluginId) {
                    item.put("id", pluginId);
                    item.put("version", null);
                    item.put("version_ref", null);
                } else {
                    TomlTable entry = table(value);
                    item.put("id", entry == null ? null : safeGetString(entry, "id"));
                    Object versionValue = entry == null ? null : entry.get("version");
                    String version = versionValue instanceof String text ? text : null;
                    String versionRef = versionValue instanceof TomlTable versionTable
                            ? safeGetString(versionTable, "ref") : null;
                    if (version == null && versionRef != null) {
                        version = versions.get(versionRef);
                    }
                    item.put("version", version);
                    item.put("version_ref", versionRef);
                }
                plugins.add(item);
            }
        }

        List<Map<String, Object>> bundles = new ArrayList<>();
        if (bundlesTable != null) {
            for (String alias : sorted(bundlesTable.keySet())) {
                Object value = bundlesTable.get(alias);
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("alias", alias);
                item.put("members", value instanceof TomlArray members
                        ? java.util.stream.IntStream.range(0, members.size()).mapToObj(members::getString).toList()
                        : List.of());
                bundles.add(item);
            }
        }

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("path", path);
        output.put("build_system", "gradle_version_catalog");
        output.put("parser", "Tomlj 1.1.1");
        output.put("parsed", !parsed.hasErrors());
        output.put("versions", versions);
        output.put("libraries", libraries);
        output.put("plugins", plugins);
        output.put("bundles", bundles);
        output.put("diagnostics", errors);
        output.put("counts", Map.of(
                "versions", versions.size(),
                "libraries", libraries.size(),
                "plugins", plugins.size(),
                "bundles", bundles.size()
        ));
        return output;
    }

    static Map<String, Object> parseMavenPom(
            Path workspaceRoot,
            Path pomPath,
            String relativePath,
            String source,
            List<Map<String, Object>> diagnostics
    ) {
        Set<Path> visited = new LinkedHashSet<>();
        MavenModel model;
        try {
            model = readMavenModel(
                    workspaceRoot.toAbsolutePath().normalize(),
                    pomPath.toAbsolutePath().normalize(),
                    source,
                    visited,
                    0,
                    diagnostics
            );
        } catch (Exception error) {
            diagnostics.add(diagnostic(
                    "MAVEN_MODEL_PARSE_ERROR",
                    safeMessage(error),
                    relativePath
            ));
            Map<String, Object> failed = new LinkedHashMap<>();
            failed.put("path", relativePath);
            failed.put("build_system", "maven");
            failed.put("model_status", "failed");
            failed.put("parent_resolution", "not_attempted");
            failed.put("diagnostics", List.of(safeMessage(error)));
            return failed;
        }
        Map<String, Object> output = model.output;
        output.put("path", relativePath);
        output.put("build_system", "maven");
        return output;
    }

    private static MavenModel readMavenModel(
            Path root,
            Path pomPath,
            String source,
            Set<Path> visited,
            int depth,
            List<Map<String, Object>> diagnostics
    ) throws Exception {
        if (depth > MAX_PARENT_DEPTH) {
            throw new IllegalArgumentException("Maven parent chain exceeded " + MAX_PARENT_DEPTH + " files.");
        }
        if (!pomPath.startsWith(root) || !isSafeWorkspaceFile(root, pomPath)) {
            throw new IllegalArgumentException("Maven parent path is outside the workspace or traverses a symlink.");
        }
        if (!visited.add(pomPath)) {
            throw new IllegalArgumentException("Maven parent cycle detected at " + root.relativize(pomPath));
        }

        Document document = parseSecureXml(source);
        Element project = document.getDocumentElement();
        if (project == null || !"project".equals(localName(project))) {
            throw new IllegalArgumentException("POM root element is not <project>.");
        }

        Element parentElement = child(project, "parent");
        String parentGroup = text(parentElement, "groupId");
        String parentArtifact = text(parentElement, "artifactId");
        String parentVersion = text(parentElement, "version");
        String parentResolution = parentElement == null ? "not_declared" : "external_unresolved";
        MavenModel parentModel = null;

        if (parentElement != null) {
            String relative = text(parentElement, "relativePath");
            boolean explicitlyEmpty = child(parentElement, "relativePath") != null && relative == null;
            if (!explicitlyEmpty) {
                Path parentRelative = Path.of(relative == null ? "../pom.xml" : relative);
                Path candidate = pomPath.getParent().resolve(parentRelative).normalize().toAbsolutePath();
                if (candidate.startsWith(root) && isSafeWorkspaceFile(root, candidate)) {
                    if (Files.isRegularFile(candidate, LinkOption.NOFOLLOW_LINKS)) {
                        if (visited.contains(candidate)) {
                            parentResolution = "cycle";
                            diagnostics.add(diagnostic(
                                    "MAVEN_PARENT_CYCLE",
                                    "Maven parent chain contains a cycle.",
                                    root.relativize(pomPath).toString().replace('\\', '/')
                            ));
                        } else if (Files.size(candidate) <= MAX_PARENT_BYTES) {
                            String parentSource = Files.readString(candidate, StandardCharsets.UTF_8);
                            try {
                                parentModel = readMavenModel(
                                        root, candidate, parentSource, visited, depth + 1, diagnostics
                                );
                                boolean coordinateMatch =
                                        same(parentGroup, parentModel.groupId)
                                                && same(parentArtifact, parentModel.artifactId);
                                if (coordinateMatch) {
                                    parentResolution = "workspace_resolved";
                                } else {
                                    parentResolution = "coordinate_mismatch";
                                    diagnostics.add(diagnostic(
                                            "MAVEN_PARENT_COORDINATE_MISMATCH",
                                            "The local relativePath POM does not match the declared parent groupId/artifactId.",
                                            root.relativize(pomPath).toString().replace('\\', '/')
                                    ));
                                    parentModel = null;
                                }
                            } catch (Exception error) {
                                parentResolution = "invalid_local_parent";
                                diagnostics.add(diagnostic(
                                        "MAVEN_PARENT_INVALID",
                                        safeMessage(error),
                                        root.relativize(pomPath).toString().replace('\\', '/')
                                ));
                            }
                        } else {
                            parentResolution = "parent_too_large";
                        }
                    }
                } else {
                    parentResolution = "unsafe_or_outside_workspace";
                }
            } else {
                parentResolution = "external_unresolved";
            }
            if (parentModel == null && !"workspace_resolved".equals(parentResolution)) {
                diagnostics.add(diagnostic(
                        "MAVEN_PARENT_NOT_RESOLVED",
                        "Parent was not resolved from a safe POM inside the workspace; no remote repository lookup was attempted.",
                        root.relativize(pomPath).toString().replace('\\', '/')
                ));
            }
        }

        Map<String, String> properties = new TreeMap<>();
        if (parentModel != null) {
            properties.putAll(parentModel.properties);
        }
        Element propertiesElement = child(project, "properties");
        if (propertiesElement != null) {
            for (Element property : children(propertiesElement, null)) {
                properties.put(localName(property), property.getTextContent().trim());
            }
        }

        Map<String, String> builtins = new TreeMap<>();
        // If the local POM inherits coordinates from an external parent, retain the
        // parent's declared group/version as partial inherited identity. This does not
        // resolve or claim the external parent itself.
        String rawGroup = firstNonBlank(text(project, "groupId"),
                parentModel == null ? parentGroup : parentModel.groupId);
        String rawArtifact = text(project, "artifactId");
        String rawVersion = firstNonBlank(text(project, "version"),
                parentModel == null ? parentVersion : parentModel.version);
        builtins.put("project.groupId", nullToEmpty(rawGroup));
        builtins.put("pom.groupId", nullToEmpty(rawGroup));
        builtins.put("project.artifactId", nullToEmpty(rawArtifact));
        builtins.put("pom.artifactId", nullToEmpty(rawArtifact));
        builtins.put("project.version", nullToEmpty(rawVersion));
        builtins.put("pom.version", nullToEmpty(rawVersion));
        builtins.put("project.parent.groupId", nullToEmpty(parentGroup));
        builtins.put("project.parent.artifactId", nullToEmpty(parentArtifact));
        builtins.put("project.parent.version", nullToEmpty(parentVersion));
        builtins.put("parent.groupId", nullToEmpty(parentGroup));
        builtins.put("parent.artifactId", nullToEmpty(parentArtifact));
        builtins.put("parent.version", nullToEmpty(parentVersion));
        properties.putAll(builtins);
        resolvePropertyMap(properties);

        String groupId = resolve(firstNonBlank(text(project, "groupId"),
                parentModel == null ? parentGroup : parentModel.groupId), properties);
        String artifactId = resolve(rawArtifact, properties);
        String version = resolve(firstNonBlank(text(project, "version"),
                parentModel == null ? parentVersion : parentModel.version), properties);

        Map<String, Map<String, Object>> managed = new LinkedHashMap<>();
        if (parentModel != null) {
            managed.putAll(parentModel.managedDependencies);
        }
        Element management = child(child(project, "dependencyManagement"), "dependencies");
        for (Element dependency : children(management, "dependency")) {
            Map<String, Object> item = dependency(dependency, properties);
            String key = dependencyKey(item);
            if (key != null) {
                managed.put(key, item);
            }
        }

        List<Map<String, Object>> declaredDependencies = new ArrayList<>();
        Element dependenciesElement = child(project, "dependencies");
        for (Element dependency : children(dependenciesElement, "dependency")) {
            Map<String, Object> item = dependency(dependency, properties);
            String key = dependencyKey(item);
            Map<String, Object> managedItem = key == null ? null : managed.get(key);
            if (blank(string(item.get("version"))) && managedItem != null) {
                item.put("version", managedItem.get("version"));
                item.put("version_source", "workspace_dependency_management");
            } else if (!blank(string(item.get("version")))) {
                item.put("version_source", "declared");
            } else {
                item.put("version_source", "not_declared");
            }
            declaredDependencies.add(item);
        }

        List<Map<String, Object>> managedOutput = new ArrayList<>(managed.values());
        List<String> modules = textChildren(child(project, "modules"), "module");
        List<Map<String, Object>> profiles = parseProfiles(project, properties);
        List<Map<String, Object>> plugins = parsePlugins(child(project, "build"), properties);

        List<String> unresolved = new ArrayList<>();
        collectUnresolved(groupId, unresolved);
        collectUnresolved(artifactId, unresolved);
        collectUnresolved(version, unresolved);
        for (Map<String, Object> dependency : declaredDependencies) {
            collectUnresolved(string(dependency.get("group_id")), unresolved);
            collectUnresolved(string(dependency.get("artifact_id")), unresolved);
            collectUnresolved(string(dependency.get("version")), unresolved);
        }
        for (Map<String, Object> item : managedOutput) {
            collectUnresolved(string(item.get("version")), unresolved);
        }
        unresolved = unresolved.stream().distinct().sorted().toList();

        Map<String, Object> coordinates = new LinkedHashMap<>();
        coordinates.put("group_id", groupId);
        coordinates.put("artifact_id", artifactId);
        coordinates.put("version", version);

        Map<String, Object> parent = new LinkedHashMap<>();
        parent.put("group_id", resolve(parentGroup, properties));
        parent.put("artifact_id", resolve(parentArtifact, properties));
        parent.put("version", resolve(parentVersion, properties));
        parent.put("resolution", parentResolution);

        List<Map<String, Object>> bomImports = managedOutput.stream()
                .filter(item -> "pom".equals(item.get("type")) && "import".equals(item.get("scope")))
                .toList();

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("model_scope", "static_declarations_plus_workspace_relative_parent");
        output.put("model_status", unresolved.isEmpty() && (parentElement == null || "workspace_resolved".equals(parentResolution))
                ? "partial_static_model" : "partial_with_unresolved_inputs");
        output.put("coordinates", coordinates);
        output.put("parent", parent);
        output.put("modules", modules);
        output.put("properties", Map.of(
                "declared_keys", properties.keySet().stream()
                        .filter(key -> !key.startsWith("project.") && !key.startsWith("pom.") && !key.startsWith("parent."))
                        .sorted().toList(),
                "count", properties.keySet().stream()
                        .filter(key -> !key.startsWith("project.") && !key.startsWith("pom.") && !key.startsWith("parent."))
                        .count()
        ));
        output.put("dependencies", declaredDependencies);
        output.put("dependency_management", managedOutput);
        output.put("bom_imports", bomImports);
        output.put("plugins", plugins);
        output.put("profiles", profiles);
        output.put("unresolved_property_references", unresolved);
        output.put("remote_resolution_attempted", false);
        output.put("build_execution_attempted", false);

        MavenModel result = new MavenModel();
        result.output = output;
        result.groupId = groupId;
        result.artifactId = artifactId;
        result.version = version;
        result.properties = properties;
        result.managedDependencies = managed;
        return result;
    }

    private static List<Map<String, Object>> parseProfiles(Element project, Map<String, String> properties) {
        List<Map<String, Object>> profiles = new ArrayList<>();
        for (Element profile : children(child(project, "profiles"), "profile")) {
            Element activation = child(profile, "activation");
            Element dependencies = child(profile, "dependencies");
            Element managed = child(child(profile, "dependencyManagement"), "dependencies");
            List<Map<String, Object>> profileDependencies = new ArrayList<>();
            for (Element dependency : children(dependencies, "dependency")) {
                profileDependencies.add(dependency(dependency, properties));
            }
            List<Map<String, Object>> managedDependencies = new ArrayList<>();
            for (Element dependency : children(managed, "dependency")) {
                managedDependencies.add(dependency(dependency, properties));
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", text(profile, "id"));
            item.put("active_by_default", Boolean.parseBoolean(text(activation, "activeByDefault")));
            item.put("activation_jdk", text(child(activation, "jdk"), null));
            item.put("activation_property_name", text(child(activation, "property"), "name"));
            item.put("activation_property_value", text(child(activation, "property"), "value"));
            item.put("activation_os_family", text(child(activation, "os"), "family"));
            item.put("activation_os_name", text(child(activation, "os"), "name"));
            item.put("activation_os_arch", text(child(activation, "os"), "arch"));
            item.put("activation_file_exists", text(child(activation, "file"), "exists"));
            item.put("activation_file_missing", text(child(activation, "file"), "missing"));
            item.put("dependencies", profileDependencies);
            item.put("dependency_management", managedDependencies);
            item.put("activated", false);
            profiles.add(item);
        }
        return profiles;
    }

    private static List<Map<String, Object>> parsePlugins(Element build, Map<String, String> properties) {
        List<Map<String, Object>> plugins = new ArrayList<>();
        addPlugins(plugins, child(build, "plugins"), properties, "build");
        addPlugins(plugins, child(child(build, "pluginManagement"), "plugins"), properties, "plugin_management");
        return plugins;
    }

    private static void addPlugins(
            List<Map<String, Object>> output,
            Element container,
            Map<String, String> properties,
            String source
    ) {
        for (Element plugin : children(container, "plugin")) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("group_id", resolve(firstNonBlank(text(plugin, "groupId"), "org.apache.maven.plugins"), properties));
            item.put("artifact_id", resolve(text(plugin, "artifactId"), properties));
            item.put("version", resolve(text(plugin, "version"), properties));
            item.put("source", source);
            output.add(item);
        }
    }

    private static Map<String, Object> dependency(Element dependency, Map<String, String> properties) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("group_id", resolve(text(dependency, "groupId"), properties));
        item.put("artifact_id", resolve(text(dependency, "artifactId"), properties));
        item.put("version", resolve(text(dependency, "version"), properties));
        item.put("scope", resolve(text(dependency, "scope"), properties));
        item.put("type", firstNonBlank(resolve(text(dependency, "type"), properties), "jar"));
        item.put("classifier", resolve(text(dependency, "classifier"), properties));
        item.put("optional", Boolean.parseBoolean(text(dependency, "optional")));
        return item;
    }

    private static String dependencyKey(Map<String, Object> item) {
        String group = string(item.get("group_id"));
        String artifact = string(item.get("artifact_id"));
        String type = string(item.get("type"));
        String classifier = string(item.get("classifier"));
        if (blank(group) || blank(artifact)) {
            return null;
        }
        return group + ":" + artifact + ":" + (blank(type) ? "jar" : type) + ":" + nullToEmpty(classifier);
    }

    private static Document parseSecureXml(String source) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(source)));
    }

    private static boolean isSafeWorkspaceFile(Path root, Path file) {
        if (!file.startsWith(root)) {
            return false;
        }
        Path current = root;
        Path relative = root.relativize(file);
        for (Path component : relative) {
            current = current.resolve(component);
            if (Files.isSymbolicLink(current)) {
                return false;
            }
        }
        return Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS);
    }

    private static void resolvePropertyMap(Map<String, String> properties) {
        for (int pass = 0; pass < 8; pass++) {
            boolean changed = false;
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                String resolved = resolve(entry.getValue(), properties);
                if (!resolved.equals(entry.getValue())) {
                    entry.setValue(resolved);
                    changed = true;
                }
            }
            if (!changed) {
                break;
            }
        }
    }

    private static String resolve(String value, Map<String, String> properties) {
        if (value == null) {
            return null;
        }
        String current = value;
        for (int pass = 0; pass < 8; pass++) {
            Matcher matcher = PROPERTY.matcher(current);
            StringBuffer result = new StringBuffer();
            boolean changed = false;
            while (matcher.find()) {
                String replacement = properties.get(matcher.group(1));
                if (replacement != null && !replacement.equals(matcher.group())) {
                    matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
                    changed = true;
                } else {
                    matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
                }
            }
            matcher.appendTail(result);
            current = result.toString();
            if (!changed) {
                break;
            }
        }
        return current;
    }

    private static void collectUnresolved(String value, List<String> output) {
        if (value == null) {
            return;
        }
        Matcher matcher = PROPERTY.matcher(value);
        while (matcher.find()) {
            output.add(matcher.group(1));
        }
    }

    private static List<String> textChildren(Element container, String name) {
        List<String> values = new ArrayList<>();
        for (Element element : children(container, name)) {
            String value = element.getTextContent().trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return values;
    }

    private static List<Element> children(Element parent, String wantedName) {
        List<Element> result = new ArrayList<>();
        if (parent == null) {
            return result;
        }
        NodeList nodes = parent.getChildNodes();
        for (int index = 0; index < nodes.getLength(); index++) {
            Node node = nodes.item(index);
            if (node instanceof Element element
                    && (wantedName == null || wantedName.equals(localName(element)))) {
                result.add(element);
            }
        }
        return result;
    }

    private static Element child(Element parent, String name) {
        for (Element element : children(parent, name)) {
            return element;
        }
        return null;
    }

    private static String text(Element parent, String childName) {
        if (parent == null) {
            return null;
        }
        if (childName == null) {
            String value = parent.getTextContent().trim();
            return value.isEmpty() ? null : value;
        }
        Element element = child(parent, childName);
        if (element == null) {
            return null;
        }
        String value = element.getTextContent().trim();
        return value.isEmpty() ? null : value;
    }

    private static String localName(Element element) {
        return element.getLocalName() == null ? element.getTagName() : element.getLocalName();
    }

    private static TomlTable table(Object value) {
        return value instanceof TomlTable tomlTable ? tomlTable : null;
    }

    private static String safeGetString(TomlTable table, String key) {
        if (table == null) {
            return null;
        }
        try {
            return table.getString(key);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String firstString(TomlTable table, String... keys) {
        if (table == null) {
            return null;
        }
        for (String key : keys) {
            String value = safeGetString(table, key);
            if (!blank(value)) {
                return value;
            }
        }
        return null;
    }

    private static List<String> sorted(Set<String> values) {
        return values.stream().sorted().toList();
    }

    private static boolean same(String expected, String actual) {
        return !blank(expected) && !blank(actual) && expected.equals(actual);
    }

    private static String firstNonBlank(String first, String second) {
        return blank(first) ? second : first;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String safeMessage(Exception error) {
        String message = error.getMessage();
        return error.getClass().getSimpleName() + (blank(message) ? "" : ": " + message);
    }

    private static Map<String, Object> diagnostic(String code, String message, String path) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("code", code);
        output.put("message", message);
        output.put("evidence", Map.of("file", path));
        return output;
    }

    private static final class MavenModel {
        private Map<String, Object> output;
        private String groupId;
        private String artifactId;
        private String version;
        private Map<String, String> properties;
        private Map<String, Map<String, Object>> managedDependencies;
    }
}
