package dev.qivra.analyzer;

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

/**
 * Literal-only Gradle model extraction. This intentionally does not evaluate Gradle DSL.
 */
final class GradleStaticModel {
    private static final Set<String> DEPENDENCY_CONFIGURATIONS = Set.of(
            "api", "implementation", "compileOnly", "runtimeOnly", "testImplementation",
            "testCompileOnly", "testRuntimeOnly", "annotationProcessor", "testAnnotationProcessor",
            "compileOnlyApi", "kapt", "ksp", "developmentOnly", "providedCompile",
            "providedRuntime", "classpath"
    );

    private GradleStaticModel() {
    }

    static Map<String, Map<String, Object>> catalogAliasIndex(List<Map<String, Object>> buildFiles) {
        Map<String, Map<String, Object>> index = new TreeMap<>();
        for (Map<String, Object> buildFile : buildFiles) {
            if (!"gradle_version_catalog".equals(string(buildFile.get("build_system")))) {
                continue;
            }
            for (Map<String, Object> library : maps(buildFile.get("libraries"))) {
                String alias = string(library.get("alias"));
                if (blank(alias)) {
                    continue;
                }
                Map<String, Object> item = new LinkedHashMap<>(library);
                item.put("catalog_kind", "library");
                index.put("libs." + normalizeAccessor(alias), item);
                index.put("libs." + alias, item);
            }
            for (Map<String, Object> plugin : maps(buildFile.get("plugins"))) {
                String alias = string(plugin.get("alias"));
                if (blank(alias)) {
                    continue;
                }
                Map<String, Object> item = new LinkedHashMap<>(plugin);
                item.put("catalog_kind", "plugin");
                index.put("libs.plugins." + normalizeAccessor(alias), item);
                index.put("libs.plugins." + alias, item);
            }
        }
        return index;
    }

    static Map<String, Object> parse(
            Path workspaceRoot,
            String path,
            String source,
            Map<String, Map<String, Object>> catalogAliases
    ) {
        List<Token> tokens = lex(source);
        boolean settingsFile = isSettingsFile(path);
        boolean conventionFile = isConventionPluginFile(path);
        List<Map<String, Object>> includedProjects = new ArrayList<>();
        List<Map<String, Object>> projectMappings = new ArrayList<>();
        List<Map<String, Object>> includedBuilds = new ArrayList<>();
        List<Map<String, Object>> plugins = new ArrayList<>();
        List<Map<String, Object>> dependencies = new ArrayList<>();
        List<Map<String, Object>> unresolved = new ArrayList<>();
        List<Map<String, Object>> conventionCandidates = new ArrayList<>();
        String rootProjectName = null;

        if (settingsFile) {
            rootProjectName = rootProjectName(tokens);
            parseIncludes(tokens, path, "include", includedProjects, unresolved, false);
            parseIncludes(tokens, path, "includeBuild", includedBuilds, unresolved, true);
            parseProjectDirectoryMappings(workspaceRoot, tokens, path, projectMappings, unresolved);
        }

        parsePlugins(tokens, path, catalogAliases, plugins, unresolved);
        parseDependencies(tokens, path, catalogAliases, dependencies, unresolved);
        if (conventionFile) {
            Map<String, Object> candidate = new LinkedHashMap<>();
            candidate.put("plugin_id_candidate", conventionPluginId(path));
            candidate.put("path", path);
            candidate.put("classification", "precompiled_script_candidate");
            candidate.put("identity_status", "inferred_from_path_only");
            conventionCandidates.add(candidate);
        }

        List<String> duplicateIncludes = includedProjects.stream()
                .map(item -> string(item.get("project_path")))
                .filter(value -> !blank(value))
                .filter(value -> includedProjects.stream()
                        .filter(other -> value.equals(other.get("project_path"))).count() > 1)
                .distinct().sorted().toList();
        for (String duplicate : duplicateIncludes) {
            unresolved.add(unresolved("duplicate_project_include", duplicate, path, -1));
        }

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("model_scope", "literal_gradle_declarations_only");
        output.put("model_status", unresolved.isEmpty()
                ? "partial_static_model" : "partial_with_unresolved_inputs");
        output.put("role", settingsFile ? "settings" : conventionFile ? "convention_plugin_candidate" : "build_script");
        output.put("root_project_name", rootProjectName);
        output.put("included_projects", includedProjects);
        output.put("project_directory_mappings", projectMappings);
        output.put("included_builds", includedBuilds);
        output.put("plugins", plugins);
        output.put("dependencies", dependencies);
        output.put("convention_plugin_candidates", conventionCandidates);
        output.put("unresolved_expressions", unresolved);
        output.put("computed_declarations_evaluated", false);

        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("included_projects", includedProjects.size());
        counts.put("project_directory_mappings", projectMappings.size());
        counts.put("included_builds", includedBuilds.size());
        counts.put("plugins", plugins.size());
        counts.put("dependencies", dependencies.size());
        counts.put("project_dependencies", dependencies.stream()
                .filter(item -> "project".equals(item.get("dependency_kind"))).count());
        counts.put("catalog_dependencies", dependencies.stream()
                .filter(item -> "version_catalog".equals(item.get("dependency_kind"))).count());
        counts.put("convention_plugin_candidates", conventionCandidates.size());
        counts.put("unresolved_expressions", unresolved.size());
        output.put("counts", counts);
        return output;
    }

    private static void parseIncludes(
            List<Token> tokens,
            String path,
            String function,
            List<Map<String, Object>> output,
            List<Map<String, Object>> unresolved,
            boolean includedBuild
    ) {
        for (int index = 0; index < tokens.size(); index++) {
            Token token = tokens.get(index);
            if (token.kind != Kind.IDENTIFIER || !function.equals(token.text)) {
                continue;
            }
            Call call = call(tokens, index);
            if (call == null) {
                if (index + 1 < tokens.size() && tokens.get(index + 1).kind == Kind.STRING) {
                    addInclude(tokens.get(index + 1), path, output, unresolved, includedBuild);
                }
                continue;
            }
            for (List<Token> argument : call.arguments) {
                Token literal = singleString(argument);
                if (literal == null || literal.interpolated) {
                    unresolved.add(unresolved(
                            includedBuild ? "computed_include_build" : "computed_project_include",
                            expression(argument), path, token.line
                    ));
                    continue;
                }
                addInclude(literal, path, output, unresolved, includedBuild);
            }
            index = Math.max(index, call.closeIndex - 1);
        }
    }

    private static void addInclude(
            Token literal,
            String path,
            List<Map<String, Object>> output,
            List<Map<String, Object>> unresolved,
            boolean includedBuild
    ) {
        String value = literal.text.trim();
        if (value.isEmpty()) {
            unresolved.add(unresolved("empty_literal_include", value, path, literal.line));
            return;
        }
        Map<String, Object> item = new LinkedHashMap<>();
        item.put(includedBuild ? "directory" : "project_path", value);
        if (!includedBuild) {
            item.put("default_project_directory", value.startsWith(":")
                    ? value.substring(1).replace(':', '/') : value.replace(':', '/'));
        }
        item.put("declaration_kind", "literal");
        item.put("evidence", evidence(path, literal.line));
        output.add(item);
    }

    private static void parseProjectDirectoryMappings(
            Path workspaceRoot,
            List<Token> tokens,
            String path,
            List<Map<String, Object>> output,
            List<Map<String, Object>> unresolved
    ) {
        Path root = workspaceRoot.toAbsolutePath().normalize();
        for (int index = 0; index < tokens.size(); index++) {
            Token token = tokens.get(index);
            if (token.kind != Kind.IDENTIFIER || !"project".equals(token.text)) {
                continue;
            }
            Call call = call(tokens, index);
            if (call == null || call.arguments.size() != 1) {
                continue;
            }
            Token projectPath = singleString(call.arguments.get(0));
            int cursor = call.closeIndex + 1;
            if (projectPath == null || !symbol(tokens, cursor, ".")
                    || !symbol(tokens, cursor + 1, "projectDir")
                    || !symbol(tokens, cursor + 2, "=")) {
                continue;
            }

            int rhsStart = cursor + 3;
            int rhsEnd = expressionEnd(tokens, rhsStart);
            List<Token> rhs = tokens.subList(rhsStart, rhsEnd);
            Token directory = literalFileArgument(rhs);
            Map<String, Object> mapping = new LinkedHashMap<>();
            mapping.put("project_path", projectPath.text);
            mapping.put("evidence", evidence(path, token.line));
            if (directory == null || directory.interpolated) {
                mapping.put("declared_expression", expression(rhs));
                mapping.put("resolution", "computed_or_unsupported");
                unresolved.add(unresolved("computed_project_directory", expression(rhs), path, token.line));
            } else {
                mapping.put("declared_directory", directory.text);
                Path resolved = root.resolve(directory.text).normalize();
                if (!resolved.startsWith(root) || hasSymlinkComponent(root, resolved)) {
                    mapping.put("resolution", "unsafe_or_outside_workspace");
                    unresolved.add(unresolved("unsafe_project_directory", directory.text, path, token.line));
                } else {
                    mapping.put("workspace_path", root.relativize(resolved).toString().replace('\\', '/'));
                    mapping.put("directory_exists", Files.isDirectory(resolved, LinkOption.NOFOLLOW_LINKS));
                    mapping.put("resolution", Files.isDirectory(resolved, LinkOption.NOFOLLOW_LINKS)
                            ? "workspace_path_resolved" : "workspace_path_not_found");
                }
            }
            output.add(mapping);
            index = Math.max(index, rhsEnd - 1);
        }
    }

    private static void parsePlugins(
            List<Token> tokens,
            String path,
            Map<String, Map<String, Object>> catalogAliases,
            List<Map<String, Object>> output,
            List<Map<String, Object>> unresolved
    ) {
        for (Range block : blocks(tokens, "plugins")) {
            for (int index = block.start + 1; index < block.end; index++) {
                Token token = tokens.get(index);
                if (token.kind != Kind.IDENTIFIER || (!"id".equals(token.text) && !"alias".equals(token.text))) {
                    continue;
                }
                Call call = call(tokens, index);
                if (call == null || call.arguments.isEmpty()) {
                    continue;
                }
                List<Token> argument = call.arguments.get(0);
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("evidence", evidence(path, token.line));
                if ("id".equals(token.text)) {
                    Token literal = singleString(argument);
                    if (literal == null || literal.interpolated) {
                        item.put("plugin_id", null);
                        item.put("version", null);
                        item.put("resolution", "computed_or_unsupported");
                        unresolved.add(unresolved("computed_plugin_id", expression(argument), path, token.line));
                    } else {
                        item.put("plugin_id", literal.text);
                        Token version = followingVersion(tokens, call.closeIndex, block.end);
                        item.put("version", version == null ? null : version.text);
                        item.put("resolution", version != null && version.interpolated
                                ? "dynamic_version" : "literal_plugin_id");
                        if (version != null && version.interpolated) {
                            unresolved.add(unresolved("dynamic_plugin_version", version.text, path, version.line));
                        }
                    }
                } else {
                    String expression = expressionPath(argument);
                    Map<String, Object> catalog = catalogAliases.get(expression);
                    item.put("catalog_expression", expression);
                    if (catalog == null || !"plugin".equals(catalog.get("catalog_kind"))) {
                        item.put("plugin_id", null);
                        item.put("version", null);
                        item.put("resolution", "catalog_alias_unresolved");
                        unresolved.add(unresolved("unresolved_plugin_catalog_alias", expression, path, token.line));
                    } else {
                        item.put("plugin_id", catalog.get("id"));
                        item.put("version", catalog.get("version"));
                        item.put("catalog_alias", catalog.get("alias"));
                        item.put("resolution", "version_catalog_alias");
                    }
                }
                output.add(item);
                index = Math.max(index, call.closeIndex);
            }
        }

        parseApplyPlugin(tokens, path, output, unresolved);
    }

    private static void parseApplyPlugin(
            List<Token> tokens,
            String path,
            List<Map<String, Object>> output,
            List<Map<String, Object>> unresolved
    ) {
        for (int index = 0; index < tokens.size(); index++) {
            if (tokens.get(index).kind != Kind.IDENTIFIER || !"apply".equals(tokens.get(index).text)) {
                continue;
            }
            int pluginIndex = index + 1;
            if (symbol(tokens, pluginIndex, "(")) {
                Call apply = call(tokens, index);
                if (apply == null) {
                    continue;
                }
                List<Token> argument = apply.arguments.stream()
                        .filter(values -> values.size() >= 3 && "plugin".equals(values.get(0).text))
                        .findFirst().orElse(List.of());
                Token literal = valueAfterEquals(argument);
                addAppliedPlugin(literal, argument, path, tokens.get(index).line, output, unresolved);
                index = Math.max(index, apply.closeIndex);
            } else if (pluginIndex < tokens.size() && "plugin".equals(tokens.get(pluginIndex).text)) {
                int valueIndex = pluginIndex + 1;
                if (symbol(tokens, valueIndex, ":") && valueIndex + 1 < tokens.size()) {
                    Token literal = tokens.get(valueIndex + 1);
                    addAppliedPlugin(literal, tokens.subList(pluginIndex, Math.min(tokens.size(), valueIndex + 2)),
                            path, tokens.get(index).line, output, unresolved);
                }
            }
        }
    }

    private static void addAppliedPlugin(
            Token literal,
            List<Token> expression,
            String path,
            int line,
            List<Map<String, Object>> output,
            List<Map<String, Object>> unresolved
    ) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("plugin_declaration_kind", "apply_plugin");
        item.put("evidence", evidence(path, line));
        if (literal == null || literal.kind != Kind.STRING || literal.interpolated) {
            item.put("plugin_id", null);
            item.put("resolution", "computed_or_unsupported");
            unresolved.add(unresolved("computed_applied_plugin", expression(expression), path, line));
        } else {
            item.put("plugin_id", literal.text);
            item.put("resolution", "literal_plugin_id");
        }
        output.add(item);
    }

    private static void parseDependencies(
            List<Token> tokens,
            String path,
            Map<String, Map<String, Object>> catalogAliases,
            List<Map<String, Object>> output,
            List<Map<String, Object>> unresolved
    ) {
        for (Range block : blocks(tokens, "dependencies")) {
            for (int index = block.start + 1; index < block.end; index++) {
                Token token = tokens.get(index);
                if (token.kind != Kind.IDENTIFIER || !DEPENDENCY_CONFIGURATIONS.contains(token.text)) {
                    continue;
                }

                List<Token> argument;
                int endIndex;
                Call call = call(tokens, index);
                if (call != null && !call.arguments.isEmpty()) {
                    argument = call.arguments.get(0);
                    endIndex = call.closeIndex;
                } else if (index + 1 < block.end && tokens.get(index + 1).kind == Kind.STRING) {
                    argument = List.of(tokens.get(index + 1));
                    endIndex = index + 1;
                } else {
                    continue;
                }

                Map<String, Object> dependency = dependency(
                        token.text, argument, path, token.line, catalogAliases, unresolved
                );
                output.add(dependency);
                index = Math.max(index, endIndex);
            }
        }
    }

    private static Map<String, Object> dependency(
            String configuration,
            List<Token> argument,
            String path,
            int line,
            Map<String, Map<String, Object>> catalogAliases,
            List<Map<String, Object>> unresolved
    ) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("configuration", configuration);
        item.put("evidence", evidence(path, line));
        item.put("declared_expression", expression(argument));

        if (argument.isEmpty()) {
            item.put("dependency_kind", "unknown");
            item.put("resolution", "empty_dependency_declaration");
            unresolved.add(unresolved("empty_dependency_declaration", "", path, line));
            return item;
        }

        if ("project".equals(argument.get(0).text) && argument.size() >= 4
                && symbol(argument, 1, "(") && argument.get(2).kind == Kind.STRING
                && symbol(argument, 3, ")") && !argument.get(2).interpolated) {
            item.put("dependency_kind", "project");
            item.put("project_path", argument.get(2).text);
            item.put("resolution", "literal_project_dependency");
            return item;
        }

        boolean platform = false;
        List<Token> valueTokens = argument;
        if (("platform".equals(argument.get(0).text) || "enforcedPlatform".equals(argument.get(0).text))
                && argument.size() >= 4 && symbol(argument, 1, "(")) {
            platform = true;
            Call nested = call(argument, 0);
            if (nested != null && !nested.arguments.isEmpty()) {
                valueTokens = nested.arguments.get(0);
            }
        }
        item.put("platform", platform);

        Token coordinate = singleString(valueTokens);
        if (coordinate != null) {
            item.put("dependency_kind", platform ? "platform" : "external_module");
            item.put("coordinate", coordinate.text);
            String[] parts = coordinate.text.split(":", 4);
            item.put("group", parts.length > 0 ? parts[0] : null);
            item.put("artifact", parts.length > 1 ? parts[1] : null);
            item.put("version", parts.length > 2 ? parts[2] : null);
            item.put("classifier_or_extension", parts.length > 3 ? parts[3] : null);
            String resolution = coordinate.interpolated ? "dynamic_string_interpolation"
                    : parts.length < 2 ? "malformed_coordinate"
                    : parts.length == 2 ? "version_not_declared"
                    : dynamicVersion(parts[2]) ? "dynamic_version_not_resolved" : "literal_coordinate";
            item.put("resolution", resolution);
            if (!"literal_coordinate".equals(resolution)) {
                unresolved.add(unresolved("gradle_dependency_" + resolution,
                        coordinate.text, path, line));
            }
            return item;
        }

        String catalogExpression = expressionPath(valueTokens);
        Map<String, Object> catalog = catalogAliases.get(catalogExpression);
        if (catalog != null && "library".equals(catalog.get("catalog_kind"))) {
            item.put("dependency_kind", "version_catalog");
            item.put("catalog_expression", catalogExpression);
            item.put("catalog_alias", catalog.get("alias"));
            item.put("group", catalog.get("group"));
            item.put("artifact", catalog.get("artifact"));
            item.put("version", catalog.get("version"));
            item.put("resolution", Boolean.TRUE.equals(catalog.get("version_resolved"))
                    ? "version_catalog_alias_resolved" : "version_catalog_alias_without_version");
            if (!Boolean.TRUE.equals(catalog.get("version_resolved"))) {
                unresolved.add(unresolved("version_catalog_alias_without_version",
                        catalogExpression, path, line));
            }
            return item;
        }

        if (catalogExpression.startsWith("libs.")) {
            item.put("dependency_kind", "version_catalog");
            item.put("catalog_expression", catalogExpression);
            item.put("resolution", "catalog_alias_unresolved");
            unresolved.add(unresolved("unresolved_dependency_catalog_alias",
                    catalogExpression, path, line));
            return item;
        }

        item.put("dependency_kind", "computed_or_unsupported");
        item.put("resolution", "computed_or_unsupported");
        unresolved.add(unresolved("computed_dependency_expression", expression(argument), path, line));
        return item;
    }

    private static String rootProjectName(List<Token> tokens) {
        for (int index = 0; index + 4 < tokens.size(); index++) {
            if (tokens.get(index).kind == Kind.IDENTIFIER
                    && "rootProject".equals(tokens.get(index).text)
                    && symbol(tokens, index + 1, ".")
                    && "name".equals(tokens.get(index + 2).text)
                    && symbol(tokens, index + 3, "=")
                    && tokens.get(index + 4).kind == Kind.STRING
                    && !tokens.get(index + 4).interpolated) {
                return tokens.get(index + 4).text;
            }
        }
        return null;
    }

    private static List<Range> blocks(List<Token> tokens, String name) {
        List<Range> ranges = new ArrayList<>();
        for (int index = 0; index + 1 < tokens.size(); index++) {
            if (tokens.get(index).kind != Kind.IDENTIFIER || !name.equals(tokens.get(index).text)
                    || !symbol(tokens, index + 1, "{")) {
                continue;
            }
            int close = matching(tokens, index + 1, "{", "}");
            if (close > index + 1) {
                ranges.add(new Range(index + 1, close));
            }
        }
        return ranges;
    }

    private static Call call(List<Token> tokens, int nameIndex) {
        if (!symbol(tokens, nameIndex + 1, "(")) {
            return null;
        }
        int close = matching(tokens, nameIndex + 1, "(", ")");
        if (close < 0) {
            return null;
        }
        List<List<Token>> arguments = new ArrayList<>();
        int depthParen = 0;
        int depthBrace = 0;
        int depthBracket = 0;
        int start = nameIndex + 2;
        for (int index = start; index < close; index++) {
            String value = tokens.get(index).text;
            if ("(".equals(value)) {
                depthParen++;
            } else if (")".equals(value)) {
                depthParen--;
            } else if ("{".equals(value)) {
                depthBrace++;
            } else if ("}".equals(value)) {
                depthBrace--;
            } else if ("[".equals(value)) {
                depthBracket++;
            } else if ("]".equals(value)) {
                depthBracket--;
            } else if (",".equals(value) && depthParen == 0 && depthBrace == 0 && depthBracket == 0) {
                arguments.add(new ArrayList<>(tokens.subList(start, index)));
                start = index + 1;
            }
        }
        if (start < close || !arguments.isEmpty()) {
            arguments.add(new ArrayList<>(tokens.subList(start, close)));
        }
        return new Call(arguments, close);
    }

    private static int matching(List<Token> tokens, int openIndex, String open, String close) {
        if (!symbol(tokens, openIndex, open)) {
            return -1;
        }
        int depth = 0;
        for (int index = openIndex; index < tokens.size(); index++) {
            if (symbol(tokens, index, open)) {
                depth++;
            } else if (symbol(tokens, index, close) && --depth == 0) {
                return index;
            }
        }
        return -1;
    }

    private static int expressionEnd(List<Token> tokens, int start) {
        if (start >= tokens.size()) {
            return start;
        }
        int line = tokens.get(start).line;
        int paren = 0;
        int brace = 0;
        int bracket = 0;
        for (int index = start; index < tokens.size(); index++) {
            Token token = tokens.get(index);
            if (index > start && token.line > line && paren == 0 && brace == 0 && bracket == 0) {
                return index;
            }
            if (";".equals(token.text) && paren == 0 && brace == 0 && bracket == 0) {
                return index;
            }
            switch (token.text) {
                case "(" -> paren++;
                case ")" -> paren--;
                case "{" -> brace++;
                case "}" -> {
                    if (brace == 0 && paren == 0 && bracket == 0) {
                        return index;
                    }
                    brace--;
                }
                case "[" -> bracket++;
                case "]" -> bracket--;
                default -> {
                }
            }
        }
        return tokens.size();
    }

    private static Token followingVersion(List<Token> tokens, int start, int limit) {
        for (int index = start + 1; index + 1 < limit; index++) {
            if (tokens.get(index).kind == Kind.IDENTIFIER && "version".equals(tokens.get(index).text)
                    && tokens.get(index + 1).kind == Kind.STRING) {
                return tokens.get(index + 1);
            }
            if (tokens.get(index).kind == Kind.IDENTIFIER
                    && ("id".equals(tokens.get(index).text) || "alias".equals(tokens.get(index).text))) {
                break;
            }
        }
        return null;
    }

    private static Token valueAfterEquals(List<Token> tokens) {
        for (int index = 0; index + 1 < tokens.size(); index++) {
            if (symbol(tokens, index, "=") && tokens.get(index + 1).kind == Kind.STRING) {
                return tokens.get(index + 1);
            }
        }
        return null;
    }

    private static Token literalFileArgument(List<Token> rhs) {
        if (rhs.size() == 4 && "file".equals(rhs.get(0).text)
                && symbol(rhs, 1, "(") && rhs.get(2).kind == Kind.STRING && symbol(rhs, 3, ")")) {
            return rhs.get(2);
        }
        if (rhs.size() >= 5 && "new".equals(rhs.get(0).text) && "File".equals(rhs.get(1).text)) {
            Call constructor = call(rhs, 1);
            if (constructor != null && constructor.arguments.size() == 2) {
                List<Token> base = constructor.arguments.get(0);
                Token value = singleString(constructor.arguments.get(1));
                boolean knownBase = base.size() == 1
                        && ("rootDir".equals(base.get(0).text) || "settingsDir".equals(base.get(0).text));
                boolean rootProjectBase = base.size() == 3 && "rootProject".equals(base.get(0).text)
                        && symbol(base, 1, ".") && "projectDir".equals(base.get(2).text);
                if ((knownBase || rootProjectBase) && value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    private static Token singleString(List<Token> tokens) {
        return tokens.size() == 1 && tokens.get(0).kind == Kind.STRING ? tokens.get(0) : null;
    }

    private static String expressionPath(List<Token> tokens) {
        if (tokens.isEmpty()) {
            return "";
        }
        StringBuilder value = new StringBuilder();
        for (Token token : tokens) {
            if (token.kind != Kind.IDENTIFIER && !".".equals(token.text)) {
                return "";
            }
            value.append(token.text);
        }
        return value.toString();
    }

    private static String expression(List<Token> tokens) {
        StringBuilder value = new StringBuilder();
        for (Token token : tokens) {
            if (value.length() > 0 && token.kind == Kind.IDENTIFIER) {
                value.append(' ');
            }
            value.append(token.text);
        }
        return value.toString();
    }

    private static Map<String, Object> unresolved(String kind, String expression, String path, int line) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("kind", kind);
        item.put("expression", expression == null ? "" : expression);
        item.put("evidence", line > 0 ? evidence(path, line) : Map.of("file", path));
        return item;
    }

    private static Map<String, Object> evidence(String path, int line) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("file", path);
        if (line > 0) {
            evidence.put("line", line);
        }
        return evidence;
    }

    private static boolean hasSymlinkComponent(Path root, Path target) {
        Path current = root;
        for (Path component : root.relativize(target)) {
            current = current.resolve(component);
            if (Files.isSymbolicLink(current)) {
                return true;
            }
        }
        return false;
    }

    private static boolean dynamicVersion(String version) {
        return version == null || version.contains("+") || version.startsWith("[") || version.startsWith("(")
                || version.startsWith("$") || version.contains("latest.") || version.contains("*");
    }

    private static boolean isSettingsFile(String path) {
        return path.endsWith("/settings.gradle") || path.endsWith("/settings.gradle.kts")
                || "settings.gradle".equals(path) || "settings.gradle.kts".equals(path);
    }

    static boolean isConventionPluginFile(String path) {
        boolean gradleScript = path.endsWith(".gradle") || path.endsWith(".gradle.kts");
        return gradleScript && (path.startsWith("buildSrc/src/main/groovy/")
                || path.startsWith("buildSrc/src/main/kotlin/")
                || path.startsWith("build-logic/src/main/groovy/")
                || path.startsWith("build-logic/src/main/kotlin/")
                || path.contains("/buildSrc/src/main/groovy/")
                || path.contains("/buildSrc/src/main/kotlin/")
                || path.contains("/build-logic/src/main/groovy/")
                || path.contains("/build-logic/src/main/kotlin/"));
    }

    private static String conventionPluginId(String path) {
        String normalized = path.replace('\\', '/');
        String[] markers = {
                "/buildSrc/src/main/groovy/", "/buildSrc/src/main/kotlin/",
                "/build-logic/src/main/groovy/", "/build-logic/src/main/kotlin/"
        };
        String relative = normalized;
        for (String marker : markers) {
            int position = normalized.indexOf(marker);
            if (position >= 0) {
                relative = normalized.substring(position + marker.length());
                break;
            }
        }
        if (relative.equals(normalized)) {
            String[] rootMarkers = {
                    "buildSrc/src/main/groovy/", "buildSrc/src/main/kotlin/",
                    "build-logic/src/main/groovy/", "build-logic/src/main/kotlin/"
            };
            for (String marker : rootMarkers) {
                if (normalized.startsWith(marker)) {
                    relative = normalized.substring(marker.length());
                    break;
                }
            }
        }
        if (relative.endsWith(".gradle.kts")) {
            relative = relative.substring(0, relative.length() - ".gradle.kts".length());
        } else if (relative.endsWith(".gradle")) {
            relative = relative.substring(0, relative.length() - ".gradle".length());
        }
        return relative.replace('/', '.');
    }

    private static String normalizeAccessor(String alias) {
        return alias.replace('-', '.').replace('_', '.');
    }

    private static boolean symbol(List<Token> tokens, int index, String value) {
        return index >= 0 && index < tokens.size() && value.equals(tokens.get(index).text);
    }

    private static List<Token> lex(String source) {
        List<Token> tokens = new ArrayList<>();
        int index = 0;
        int line = 1;
        while (index < source.length()) {
            char current = source.charAt(index);
            if (Character.isWhitespace(current)) {
                if (current == '\n') {
                    line++;
                }
                index++;
                continue;
            }
            if (current == '/' && index + 1 < source.length() && source.charAt(index + 1) == '/') {
                index += 2;
                while (index < source.length() && source.charAt(index) != '\n') {
                    index++;
                }
                continue;
            }
            if (current == '/' && index + 1 < source.length() && source.charAt(index + 1) == '*') {
                index += 2;
                while (index + 1 < source.length() && !(source.charAt(index) == '*' && source.charAt(index + 1) == '/')) {
                    if (source.charAt(index) == '\n') {
                        line++;
                    }
                    index++;
                }
                index = Math.min(source.length(), index + 2);
                continue;
            }
            if (current == '"' || current == '\'') {
                char quote = current;
                boolean triple = index + 2 < source.length()
                        && source.charAt(index + 1) == quote && source.charAt(index + 2) == quote;
                int delimiter = triple ? 3 : 1;
                index += delimiter;
                int tokenLine = line;
                StringBuilder value = new StringBuilder();
                boolean interpolated = false;
                boolean closed = false;
                while (index < source.length()) {
                    if (triple && index + 2 < source.length()
                            && source.charAt(index) == quote
                            && source.charAt(index + 1) == quote
                            && source.charAt(index + 2) == quote) {
                        index += 3;
                        closed = true;
                        break;
                    }
                    char character = source.charAt(index++);
                    if (!triple && character == quote) {
                        closed = true;
                        break;
                    }
                    if (character == '\\' && index < source.length()) {
                        char escaped = source.charAt(index++);
                        if (escaped == '\n') {
                            line++;
                        }
                        value.append(escaped);
                        continue;
                    }
                    if (character == '\n') {
                        line++;
                    }
                    if (quote == '"' && character == '$') {
                        interpolated = true;
                    }
                    value.append(character);
                }
                tokens.add(new Token(Kind.STRING, value.toString(), tokenLine, interpolated || !closed));
                continue;
            }
            if (Character.isJavaIdentifierStart(current) || current == '$') {
                int start = index++;
                while (index < source.length()
                        && (Character.isJavaIdentifierPart(source.charAt(index)) || source.charAt(index) == '$')) {
                    index++;
                }
                tokens.add(new Token(Kind.IDENTIFIER, source.substring(start, index), line, false));
                continue;
            }
            if (Character.isDigit(current)) {
                int start = index++;
                while (index < source.length()
                        && (Character.isLetterOrDigit(source.charAt(index)) || source.charAt(index) == '.')) {
                    index++;
                }
                tokens.add(new Token(Kind.NUMBER, source.substring(start, index), line, false));
                continue;
            }
            tokens.add(new Token(Kind.SYMBOL, String.valueOf(current), line, false));
            index++;
        }
        return tokens;
    }

    private static List<Map<String, Object>> maps(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> output = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> copy = new LinkedHashMap<>();
                map.forEach((key, entry) -> copy.put(String.valueOf(key), entry));
                output.add(copy);
            }
        }
        return output;
    }

    private static String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private enum Kind {
        IDENTIFIER,
        STRING,
        NUMBER,
        SYMBOL
    }

    private record Token(Kind kind, String text, int line, boolean interpolated) {
    }

    private record Call(List<List<Token>> arguments, int closeIndex) {
    }

    private record Range(int start, int end) {
    }
}
