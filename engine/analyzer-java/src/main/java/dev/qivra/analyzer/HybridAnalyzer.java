package dev.qivra.analyzer;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Problem;
import com.github.javaparser.Position;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.SwitchExpr;
import com.github.javaparser.ast.expr.TextBlockLiteralExpr;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.gradle.GradleParser;
import org.openrewrite.tree.ParseError;
import org.openrewrite.xml.XmlParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import static com.github.javaparser.ParseStart.COMPILATION_UNIT;
import static com.github.javaparser.Providers.provider;

public final class HybridAnalyzer {
    private static final String VERSION = "0.7.0";
    private static final String SCHEMA_VERSION = "1.6.0";
    private static final int MAX_FILES = 20_000;
    private static final long MAX_FILE_BYTES = 2_000_000L;
    private static final Set<String> IGNORED_DIRECTORIES = Set.of(
            ".git", ".gradle", ".idea", ".next", ".venv", "__pycache__",
            "build", "dist", "node_modules", "out", "target", "vendor"
    );

    private HybridAnalyzer() {
    }

    public static void main(String[] args) {
        try {
            Arguments arguments = Arguments.parse(args);
            Path root = arguments.root.toAbsolutePath().normalize();
            if (!Files.isDirectory(root)) {
                throw new IllegalArgumentException("Repository path is not a directory: " + root);
            }

            Map<String, Object> inventory = analyze(root);
            String json = JsonWriter.write(inventory, arguments.pretty);
            if (arguments.output == null) {
                System.out.print(json);
            } else {
                Path output = arguments.output.toAbsolutePath().normalize();
                Path parent = output.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.writeString(output, json, StandardCharsets.UTF_8);
            }
        } catch (Exception error) {
            System.err.println("qivra-hybrid-analyzer: " + error.getMessage());
            System.exit(2);
        }
    }

    private static Map<String, Object> analyze(Path root) throws IOException, NoSuchAlgorithmException {
        List<Path> candidates;
        try (Stream<Path> paths = Files.walk(root)) {
            candidates = paths
                    .filter(path -> !path.equals(root))
                    .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .filter(path -> !isIgnored(root, path))
                    .filter(HybridAnalyzer::isSupportedFile)
                    .sorted(Comparator.comparing(path -> relative(root, path)))
                    .limit(MAX_FILES + 1L)
                    .toList();
        }

        boolean fileLimitReached = candidates.size() > MAX_FILES;
        if (fileLimitReached) {
            candidates = new ArrayList<>(candidates.subList(0, MAX_FILES));
        }

        JavaParser javaParser = new JavaParser(new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE));
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        List<Map<String, Object>> javaSources = new ArrayList<>();
        List<Map<String, Object>> buildFiles = new ArrayList<>();
        List<Map<String, Object>> diagnostics = new ArrayList<>();
        int skippedLargeFiles = 0;

        // Parse catalogs first so literal libs.* references can be linked from build scripts.
        for (Path file : candidates) {
            if (!isVersionCatalog(file) || Files.size(file) > MAX_FILE_BYTES) {
                continue;
            }
            String catalogPath = relative(root, file);
            String catalogSource = Files.readString(file, StandardCharsets.UTF_8);
            buildFiles.add(BuildModelIntelligence.parseVersionCatalog(catalogPath, catalogSource, diagnostics));
        }
        Map<String, Map<String, Object>> gradleCatalogAliases =
                GradleStaticModel.catalogAliasIndex(buildFiles);

        for (Path file : candidates) {
            String path = relative(root, file);
            long size = Files.size(file);
            updateDigest(digest, path);
            if (size > MAX_FILE_BYTES) {
                updateDigest(digest, "oversize:" + size);
                skippedLargeFiles++;
                diagnostics.add(diagnostic(
                        "SOURCE_FILE_TOO_LARGE",
                        "File exceeded the static analyzer size limit of " + MAX_FILE_BYTES + " bytes.",
                        path
                ));
                continue;
            }

            byte[] bytes = Files.readAllBytes(file);
            digest.update(bytes);
            String source = new String(bytes, StandardCharsets.UTF_8);

            if (path.endsWith(".java")) {
                javaSources.add(parseJava(javaParser, path, source, diagnostics));
            } else if (isVersionCatalog(file)) {
                // Parsed in the first pass, before build scripts.
                continue;
            } else if (isGradleFile(path)) {
                buildFiles.add(parseGradle(root, path, source, gradleCatalogAliases, diagnostics));
            } else if (path.endsWith("pom.xml")) {
                buildFiles.add(parsePom(root, path, source, diagnostics));
            }
        }

        buildFiles.sort(Comparator.comparing(item -> String.valueOf(item.get("path"))));

        int parsedJavaFiles = (int) javaSources.stream()
                .filter(item -> "valid".equals(item.get("parse_status")))
                .count();
        int typeCount = javaSources.stream()
                .mapToInt(item -> ((List<?>) item.get("types")).size())
                .sum();
        int methodCount = javaSources.stream()
                .mapToInt(item -> ((List<?>) item.get("methods")).size())
                .sum();
        int parsedGradleFiles = (int) buildFiles.stream()
                .filter(item -> "gradle".equals(item.get("build_system")) && Boolean.TRUE.equals(item.get("parsed")))
                .count();
        int parsedPomFiles = (int) buildFiles.stream()
                .filter(item -> "maven".equals(item.get("build_system")) && Boolean.TRUE.equals(item.get("parsed")))
                .count();

        long catalogCount = buildFiles.stream().filter(item -> "gradle_version_catalog".equals(item.get("build_system"))).count();
        long parsedCatalogCount = buildFiles.stream().filter(item -> "gradle_version_catalog".equals(item.get("build_system")) && Boolean.TRUE.equals(item.get("parsed"))).count();
        long catalogLibraryCount = buildFiles.stream().filter(item -> "gradle_version_catalog".equals(item.get("build_system")))
                .map(item -> item.get("counts")).filter(Map.class::isInstance).map(Map.class::cast)
                .mapToLong(counts -> ((Number) counts.getOrDefault("libraries", 0)).longValue()).sum();
        long resolvedParentCount = buildFiles.stream().filter(item -> "maven".equals(item.get("build_system")))
                .map(item -> item.get("static_model")).filter(Map.class::isInstance).map(Map.class::cast)
                .map(item -> item.get("parent")).filter(Map.class::isInstance).map(Map.class::cast)
                .filter(parent -> "workspace_resolved".equals(parent.get("resolution"))).count();
        long mavenDependencyCount = buildFiles.stream().filter(item -> "maven".equals(item.get("build_system")))
                .map(item -> item.get("static_model")).filter(Map.class::isInstance).map(Map.class::cast)
                .map(item -> item.get("dependencies")).filter(List.class::isInstance).map(List.class::cast).mapToLong(List::size).sum();
        long mavenBomImportCount = buildFiles.stream().filter(item -> "maven".equals(item.get("build_system")))
                .map(item -> item.get("static_model")).filter(Map.class::isInstance).map(Map.class::cast)
                .map(item -> item.get("bom_imports")).filter(List.class::isInstance).map(List.class::cast).mapToLong(List::size).sum();
        long gradleProjectCount = gradleModelCount(buildFiles, "included_projects");
        long gradleProjectMappingCount = gradleModelCount(buildFiles, "project_directory_mappings");
        long gradleDependencyCount = gradleModelCount(buildFiles, "dependencies");
        long gradleUnresolvedCount = gradleModelCount(buildFiles, "unresolved_expressions");
        long gradleConventionPluginCount = gradleModelCount(buildFiles, "convention_plugin_candidates");

        Map<String, Object> javaApiSummary = JavaApiIntelligence.enrich(root, javaSources, diagnostics, MAX_FILE_BYTES);
        Map<String, Object> dependencyGraph = DependencyGraph.build(javaSources, buildFiles);
        Map<String, Object> dependencyGraphSummary = (Map<String, Object>) dependencyGraph.get("summary");

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("files_scanned", candidates.size());
        summary.put("java_files", javaSources.size());
        summary.put("parsed_java_files", parsedJavaFiles);
        summary.put("gradle_files", buildFiles.stream().filter(item -> "gradle".equals(item.get("build_system"))).count());
        summary.put("parsed_gradle_files", parsedGradleFiles);
        summary.put("maven_files", buildFiles.stream().filter(item -> "maven".equals(item.get("build_system"))).count());
        summary.put("parsed_maven_files", parsedPomFiles);
        summary.put("gradle_version_catalogs", catalogCount);
        summary.put("parsed_gradle_version_catalogs", parsedCatalogCount);
        summary.put("gradle_catalog_libraries", catalogLibraryCount);
        summary.put("gradle_projects_declared", gradleProjectCount);
        summary.put("gradle_project_dir_mappings", gradleProjectMappingCount);
        summary.put("gradle_dependencies", gradleDependencyCount);
        summary.put("gradle_unresolved_declarations", gradleUnresolvedCount);
        summary.put("gradle_convention_plugin_candidates", gradleConventionPluginCount);
        summary.put("maven_parent_resolved", resolvedParentCount);
        summary.put("maven_dependency_declarations", mavenDependencyCount);
        summary.put("maven_bom_imports", mavenBomImportCount);
        summary.put("java_type_declarations", typeCount);
        summary.put("java_method_declarations", methodCount);
        summary.put("java_api_references", javaApiSummary.get("resolved_references") instanceof Number
                ? ((Number) javaApiSummary.get("resolved_references")).longValue()
                        + ((Number) javaApiSummary.get("unresolved_references")).longValue() : 0L);
        summary.put("java_api_resolved_references", javaApiSummary.get("resolved_references"));
        summary.put("java_api_unresolved_references", javaApiSummary.get("unresolved_references"));
        summary.put("java_api_workspace_source_roots", javaApiSummary.get("workspace_source_roots"));
        summary.put("dependency_graph_nodes", dependencyGraphSummary.get("node_count"));
        summary.put("dependency_graph_edges", dependencyGraphSummary.get("edge_count"));
        summary.put("dependency_graph_unresolved_edges", dependencyGraphSummary.get("unresolved_edges"));
        summary.put("diagnostics", diagnostics.size());
        summary.put("file_limit_reached", fileLimitReached);
        summary.put("skipped_large_files", skippedLargeFiles);

        Map<String, Object> analyzer = new LinkedHashMap<>();
        analyzer.put("name", "qivra-hybrid-source-analyzer");
        analyzer.put("version", VERSION);
        analyzer.put("java_parser", "com.github.javaparser:javaparser-core:3.28.2");
        analyzer.put("java_symbol_solver", "com.github.javaparser:javaparser-symbol-solver-core:3.28.2");
        analyzer.put("openrewrite", "org.openrewrite:rewrite-gradle,rewrite-xml:8.90.4");
        analyzer.put("toml_parser", "org.tomlj:tomlj:1.1.1");

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("recognized_files_sha256", toHex(digest.digest()));

        Map<String, Object> inventory = new LinkedHashMap<>();
        inventory.put("schema_version", SCHEMA_VERSION);
        inventory.put("analyzer", analyzer);
        inventory.put("snapshot", snapshot);
        inventory.put("summary", summary);
        inventory.put("java_sources", javaSources);
        inventory.put("build_files", buildFiles);
        inventory.put("dependency_graph", dependencyGraph);
        inventory.put("diagnostics", diagnostics);
        return inventory;
    }

    private static Map<String, Object> parseJava(
            JavaParser parser,
            String path,
            String source,
            List<Map<String, Object>> diagnostics
    ) {
        ParseResult<CompilationUnit> result = parser.parse(COMPILATION_UNIT, provider(source));
        CompilationUnit unit = result.getResult().orElse(null);
        boolean valid = result.isSuccessful() && unit != null;

        if (!valid) {
            for (Problem problem : result.getProblems()) {
                diagnostics.add(diagnostic("JAVA_PARSE_ERROR", problem.getMessage(), path));
            }
        }

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("path", path);
        output.put("parse_status", valid ? "valid" : unit == null ? "failed" : "partial");
        output.put("package", unit == null ? null : unit.getPackageDeclaration()
                .map(declaration -> declaration.getNameAsString()).orElse(null));

        List<Map<String, Object>> imports = new ArrayList<>();
        List<Map<String, Object>> types = new ArrayList<>();
        List<Map<String, Object>> methods = new ArrayList<>();
        Map<String, Integer> methodCalls = new TreeMap<>();
        Map<String, Object> features = new LinkedHashMap<>();

        if (unit != null) {
            unit.getImports().forEach(importDeclaration -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name", importDeclaration.getNameAsString());
                item.put("static", importDeclaration.isStatic());
                item.put("wildcard", importDeclaration.isAsterisk());
                item.put("line", line(importDeclaration));
                imports.add(item);
            });

            unit.findAll(ClassOrInterfaceDeclaration.class).forEach(type ->
                    types.add(typeInfo(type.getNameAsString(), type.isInterface() ? "interface" : "class", type)));
            unit.findAll(EnumDeclaration.class).forEach(type ->
                    types.add(typeInfo(type.getNameAsString(), "enum", type)));
            unit.findAll(AnnotationDeclaration.class).forEach(type ->
                    types.add(typeInfo(type.getNameAsString(), "annotation", type)));
            unit.findAll(RecordDeclaration.class).forEach(type ->
                    types.add(typeInfo(type.getNameAsString(), "record", type)));

            unit.findAll(MethodDeclaration.class).forEach(method -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name", method.getNameAsString());
                item.put("return_type", method.getTypeAsString());
                item.put("parameter_count", method.getParameters().size());
                item.put("public", method.isPublic());
                item.put("static", method.isStatic());
                item.put("line", line(method));
                methods.add(item);
            });
            unit.findAll(MethodCallExpr.class).forEach(call ->
                    methodCalls.merge(call.getNameAsString(), 1, Integer::sum));

            features.put("records", unit.findAll(RecordDeclaration.class).size());
            features.put("text_blocks", unit.findAll(TextBlockLiteralExpr.class).size());
            features.put("lambdas", unit.findAll(LambdaExpr.class).size());
            features.put("switch_expressions", unit.findAll(SwitchExpr.class).size());
            features.put("module_declaration", unit.getModule().isPresent());
        } else {
            features.put("records", 0);
            features.put("text_blocks", 0);
            features.put("lambdas", 0);
            features.put("switch_expressions", 0);
            features.put("module_declaration", false);
        }

        types.sort(Comparator.comparingInt(item -> (Integer) item.get("line")));
        methods.sort(Comparator.comparingInt(item -> (Integer) item.get("line")));
        output.put("imports", imports);
        output.put("types", types);
        output.put("methods", methods);
        output.put("method_call_counts", methodCalls);
        output.put("language_features", features);
        return output;
    }

    private static Map<String, Object> typeInfo(String name, String kind, Node node) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("kind", kind);
        item.put("line", line(node));
        return item;
    }

    private static Map<String, Object> parseGradle(
            Path root,
            String path,
            String source,
            Map<String, Map<String, Object>> catalogAliases,
            List<Map<String, Object>> diagnostics
    ) {
        List<String> errors = new ArrayList<>();
        InMemoryExecutionContext context = new InMemoryExecutionContext(error ->
                errors.add(error.getClass().getSimpleName() + ": " + String.valueOf(error.getMessage())));
        Parser.Input input = Parser.Input.fromString(Path.of(path), source);
        SourceFile parsed = null;
        try {
            parsed = GradleParser.builder().build()
                    .parseInputs(List.of(input), null, context)
                    .findFirst()
                    .orElse(null);
        } catch (RuntimeException error) {
            errors.add(error.getClass().getSimpleName() + ": " + String.valueOf(error.getMessage()));
        }
        boolean valid = parsed != null && !(parsed instanceof ParseError);
        if (!valid) {
            diagnostics.add(diagnostic("GRADLE_PARSE_ERROR", joinErrors(errors, "OpenRewrite could not parse the Gradle script."), path));
        }

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("path", path);
        output.put("build_system", "gradle");
        output.put("parser", "OpenRewrite GradleParser 8.90.4");
        output.put("parsed", valid);
        output.put("diagnostics", errors);
        output.put("static_model", GradleStaticModel.parse(root, path, source, catalogAliases));
        return output;
    }

    private static long gradleModelCount(List<Map<String, Object>> buildFiles, String countName) {
        return buildFiles.stream()
                .filter(item -> "gradle".equals(item.get("build_system")))
                .map(item -> item.get("static_model"))
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(item -> item.get("counts"))
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .mapToLong(counts -> ((Number) counts.getOrDefault(countName, 0)).longValue())
                .sum();
    }

    private static Map<String, Object> parsePom(
            Path root,
            String path,
            String source,
            List<Map<String, Object>> diagnostics
    ) {
        List<String> errors = new ArrayList<>();
        InMemoryExecutionContext context = new InMemoryExecutionContext(error ->
                errors.add(error.getClass().getSimpleName() + ": " + String.valueOf(error.getMessage())));
        Parser.Input input = Parser.Input.fromString(Path.of(path), source);
        SourceFile parsed = null;
        try {
            parsed = new XmlParser().parseInputs(List.of(input), null, context).findFirst().orElse(null);
        } catch (RuntimeException error) {
            errors.add(error.getClass().getSimpleName() + ": " + String.valueOf(error.getMessage()));
        }
        boolean valid = parsed != null && !(parsed instanceof ParseError);
        if (!valid) {
            diagnostics.add(diagnostic("MAVEN_XML_PARSE_ERROR", joinErrors(errors, "OpenRewrite could not parse the POM XML."), path));
        }

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("path", path);
        output.put("build_system", "maven");
        output.put("parser", "OpenRewrite XmlParser 8.90.4");
        output.put("parsed", valid);
        output.put("diagnostics", errors);
        output.put("static_model", BuildModelIntelligence.parseMavenPom(root, root.resolve(path), path, source, diagnostics));
        return output;
    }

    private static Map<String, Object> diagnostic(String code, String message, String path) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("code", code);
        output.put("message", message);
        output.put("evidence", Map.of("file", path));
        return output;
    }

    private static String joinErrors(List<String> errors, String fallback) {
        return errors.isEmpty() ? fallback : String.join("; ", errors);
    }

    private static int line(Node node) {
        return node.getBegin().map(position -> position.line).orElse(-1);
    }

    private static boolean isVersionCatalog(Path path) {
        Path parent = path.getParent();
        return parent != null && "gradle".equals(parent.getFileName().toString())
                && path.getFileName().toString().endsWith(".versions.toml");
    }

    private static boolean isSupportedFile(Path path) {
        String name = path.getFileName().toString();
        return name.endsWith(".java")
                || name.equals("pom.xml")
                || name.equals("build.gradle")
                || name.equals("build.gradle.kts")
                || name.equals("settings.gradle")
                || name.equals("settings.gradle.kts")
                || (name.endsWith(".versions.toml") && path.getParent() != null
                && "gradle".equals(path.getParent().getFileName().toString()))
                || isConventionPluginFile(path);
    }

    private static boolean isGradleFile(String path) {
        return path.endsWith("build.gradle")
                || path.endsWith("build.gradle.kts")
                || path.endsWith("settings.gradle")
                || path.endsWith("settings.gradle.kts")
                || isConventionPluginFile(Path.of(path));
    }

    private static boolean isConventionPluginFile(Path path) {
        return GradleStaticModel.isConventionPluginFile(path.toString().replace('\\', '/'));
    }

    private static boolean isIgnored(Path root, Path file) {
        Path relative = root.relativize(file);
        for (Path component : relative) {
            String name = component.toString();
            if (IGNORED_DIRECTORIES.contains(name) || name.startsWith(".")) {
                return true;
            }
        }
        return false;
    }

    private static String relative(Path root, Path file) {
        return root.relativize(file).toString().replace('\\', '/');
    }

    private static void updateDigest(MessageDigest digest, String value) {
        digest.update(value.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
    }

    private static String toHex(byte[] bytes) {
        StringBuilder output = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            output.append(String.format("%02x", value));
        }
        return output.toString();
    }

    private static final class Arguments {
        private final Path root;
        private final Path output;
        private final boolean pretty;

        private Arguments(Path root, Path output, boolean pretty) {
            this.root = root;
            this.output = output;
            this.pretty = pretty;
        }

        private static Arguments parse(String[] args) {
            Path root = Path.of(".");
            Path output = null;
            boolean pretty = false;
            int positional = 0;
            for (int index = 0; index < args.length; index++) {
                switch (args[index]) {
                    case "--pretty" -> pretty = true;
                    case "--output", "-o" -> {
                        if (++index >= args.length) {
                            throw new IllegalArgumentException("Expected a file path after " + args[index - 1]);
                        }
                        output = Path.of(args[index]);
                    }
                    default -> {
                        if (args[index].startsWith("-")) {
                            throw new IllegalArgumentException("Unknown option: " + args[index]);
                        }
                        if (positional++ > 0) {
                            throw new IllegalArgumentException("Only one repository root path may be provided.");
                        }
                        root = Path.of(args[index]);
                    }
                }
            }
            return new Arguments(root, output, pretty);
        }
    }
}
