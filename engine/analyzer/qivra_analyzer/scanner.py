import hashlib
import os
import re
import xml.etree.ElementTree as ET
from pathlib import Path

from . import SCHEMA_VERSION, __version__

IGNORED_DIRS = {
    ".git", ".gradle", ".idea", ".next", ".venv", "__pycache__",
    "build", "dist", "node_modules", "out", "target", "vendor",
}
BUILD_FILES = {"pom.xml", "build.gradle", "build.gradle.kts"}
PROPERTY_PATTERN = re.compile(r"\$\{([^}]+)\}")


def _local(tag):
    return tag.rsplit("}", 1)[-1]


def _child(element, name):
    if element is None:
        return None
    return next((node for node in list(element) if _local(node.tag) == name), None)


def _children(element, name):
    if element is None:
        return []
    return [node for node in list(element) if _local(node.tag) == name]


def _text(element, name):
    child = _child(element, name)
    if child is None or child.text is None:
        return None
    value = child.text.strip()
    return value or None


def _properties(element):
    properties_node = _child(element, "properties")
    if properties_node is None:
        return {}
    return {
        _local(node.tag): (node.text or "").strip()
        for node in list(properties_node)
        if (node.text or "").strip()
    }


def _resolve(value, properties):
    if value is None:
        return None
    resolved = str(value)
    for _ in range(32):
        updated = PROPERTY_PATTERN.sub(
            lambda match: str(properties.get(match.group(1), match.group(0))),
            resolved,
        )
        if updated == resolved:
            break
        resolved = updated
    return resolved


def _relative(path, root):
    return path.relative_to(root).as_posix()


def _evidence(path, root):
    return {"file": _relative(path, root)}


def _parse_dependency_nodes(container, properties):
    dependencies = []
    for dependency in _children(container, "dependency"):
        group = _resolve(_text(dependency, "groupId"), properties)
        artifact = _resolve(_text(dependency, "artifactId"), properties)
        if not group or not artifact:
            continue
        dependencies.append({
            "group": group,
            "artifact": artifact,
            "version": _resolve(_text(dependency, "version"), properties),
            "scope": _resolve(_text(dependency, "scope"), properties),
            "type": _resolve(_text(dependency, "type"), properties),
            "classifier": _resolve(_text(dependency, "classifier"), properties),
            "optional": _resolve(_text(dependency, "optional"), properties),
        })
    return sorted(
        dependencies,
        key=lambda item: (
            item["group"], item["artifact"], item["type"] or "",
            item["classifier"] or "", item["version"] or "",
        ),
    )


def _parse_plugins(container, properties):
    plugins = []
    for plugin in _children(container, "plugin"):
        artifact = _resolve(_text(plugin, "artifactId"), properties)
        if not artifact:
            continue
        plugins.append({
            "group": _resolve(_text(plugin, "groupId"), properties) or "org.apache.maven.plugins",
            "artifact": artifact,
            "version": _resolve(_text(plugin, "version"), properties),
        })
    return sorted(plugins, key=lambda item: (item["group"], item["artifact"], item["version"] or ""))


def _parse_profiles(document, properties):
    profiles = []
    profiles_node = _child(document, "profiles")
    for profile in _children(profiles_node, "profile"):
        profile_properties = _properties(profile)
        activation_node = _child(profile, "activation")
        activation = {
            key: _text(activation_node, key)
            for key in ("activeByDefault", "jdk")
            if _text(activation_node, key) is not None
        }
        os_node = _child(activation_node, "os")
        if os_node is not None:
            activation["os"] = {
                key: _text(os_node, key)
                for key in ("name", "family", "arch", "version")
                if _text(os_node, key) is not None
            }
        property_node = _child(activation_node, "property")
        if property_node is not None:
            activation["property"] = {
                key: _text(property_node, key)
                for key in ("name", "value")
                if _text(property_node, key) is not None
            }
        file_node = _child(activation_node, "file")
        if file_node is not None:
            activation["file"] = {
                key: _text(file_node, key)
                for key in ("exists", "missing")
                if _text(file_node, key) is not None
            }

        profile_properties_for_values = {**properties, **profile_properties}
        profile_dependencies = _parse_dependency_nodes(
            _child(profile, "dependencies"), profile_properties_for_values
        )
        profile_management_node = _child(profile, "dependencyManagement")
        profile_managed = _parse_dependency_nodes(
            _child(profile_management_node, "dependencies"), profile_properties_for_values
        )
        build_node = _child(profile, "build")
        profile_plugins = _parse_plugins(_child(build_node, "plugins"), profile_properties_for_values)
        plugin_management_node = _child(build_node, "pluginManagement")

        profiles.append({
            "id": _text(profile, "id"),
            "activation": activation,
            "activation_status": "not-evaluated",
            "properties": profile_properties,
            "dependencies": profile_dependencies,
            "dependency_management": profile_managed,
            "plugins": profile_plugins,
            "plugin_management": _parse_plugins(
                _child(plugin_management_node, "plugins"), profile_properties_for_values
            ),
        })
    return sorted(profiles, key=lambda item: item["id"] or "")


def _parse_pom(path, root):
    raw = path.read_text(encoding="utf-8", errors="replace")
    try:
        document = ET.fromstring(raw)
    except ET.ParseError as error:
        return None, {
            "code": "MAVEN_POM_MALFORMED",
            "message": f"Cannot parse pom.xml: {error}",
            "severity": "error",
            "evidence": [_evidence(path, root)],
        }

    properties = _properties(document)
    parent_node = _child(document, "parent")
    parent = None
    if parent_node is not None:
        relative_path_node = _child(parent_node, "relativePath")
        if relative_path_node is None:
            relative_path = "../pom.xml"
        else:
            relative_path = (relative_path_node.text or "").strip() or None
        parent = {
            "group": _resolve(_text(parent_node, "groupId"), properties),
            "artifact": _resolve(_text(parent_node, "artifactId"), properties),
            "version": _resolve(_text(parent_node, "version"), properties),
            "relative_path": relative_path,
        }

    java_raw = {}
    for key in ("release", "source", "target"):
        value = properties.get(f"maven.compiler.{key}")
        if key == "release" and not value:
            value = properties.get("java.version")
        if value:
            java_raw[key] = value

    build_node = _child(document, "build")
    plugins_node = _child(build_node, "plugins")
    plugin_management_node = _child(build_node, "pluginManagement")
    compiler_plugins = [
        plugin for plugin in _children(plugins_node, "plugin")
        if _text(plugin, "artifactId") == "maven-compiler-plugin"
    ]
    for plugin in compiler_plugins:
        config = _child(plugin, "configuration")
        for key in ("release", "source", "target"):
            value = _text(config, key)
            if value:
                java_raw[key] = value

    modules_node = _child(document, "modules")
    modules = [(node.text or "").strip() for node in _children(modules_node, "module")]
    modules = sorted(value for value in modules if value)

    dependencies_node = _child(document, "dependencies")
    dependency_management_node = _child(document, "dependencyManagement")
    dependency_management = _parse_dependency_nodes(
        _child(dependency_management_node, "dependencies"), properties
    )
    bom_imports = [
        dependency for dependency in dependency_management
        if dependency["type"] == "pom" and dependency["scope"] == "import"
    ]

    group = _resolve(_text(document, "groupId"), properties)
    if not group and parent:
        group = parent["group"]
    artifact = _resolve(_text(document, "artifactId"), properties) or path.parent.name
    version = _resolve(_text(document, "version"), properties)

    project = {
        "build_system": "maven",
        "name": artifact,
        "group": group,
        "version": version,
        "java": {key: _resolve(value, properties) for key, value in java_raw.items()},
        "modules": modules,
        "dependencies": _parse_dependency_nodes(dependencies_node, properties),
        "dependency_management": dependency_management,
        "bom_imports": bom_imports,
        "plugins": _parse_plugins(plugins_node, properties),
        "plugin_management": _parse_plugins(
            _child(plugin_management_node, "plugins"), properties
        ),
        "properties": properties,
        "parent": parent,
        "parent_inheritance": {
            "resolution": "not-evaluated" if parent else "no-parent",
            "parent_path": None,
            "inherited_properties": {},
            "inherited_dependency_management": [],
        },
        "profiles": _parse_profiles(document, properties),
        "evidence": [_evidence(path, root)],
        "_pom_file": path.resolve(),
        "_java_raw": java_raw,
        "_local_properties": properties,
    }
    return project, None


def _resolve_local_maven_parents(projects, root):
    by_path = {
        project["_pom_file"]: project
        for project in projects
        if project["build_system"] == "maven" and project.get("_pom_file")
    }
    diagnostics = []
    resolving = set()
    resolved = set()

    def resolve_project(project):
        current_path = project["_pom_file"]
        if current_path in resolved:
            return project.get("_resolved_properties", {})
        if current_path in resolving:
            project["parent_inheritance"]["resolution"] = "circular-local-parent"
            return {}

        resolving.add(current_path)
        parent = project.get("parent")
        inherited_properties = {}
        inherited_management = []
        resolution = "no-parent" if parent is None else "not-found-locally"
        parent_path_value = None
        parent_project = None

        if parent is not None:
            relative_path = parent.get("relative_path", "../pom.xml")
            if relative_path is None:
                resolution = "relative-path-disabled"
            else:
                candidate = (current_path.parent / relative_path).resolve()
                if candidate.is_dir():
                    candidate = (candidate / "pom.xml").resolve()
                try:
                    candidate.relative_to(root)
                    inside_root = True
                except ValueError:
                    inside_root = False

                if inside_root and candidate in by_path:
                    if candidate in resolving:
                        resolution = "circular-local-parent"
                    else:
                        parent_project = by_path[candidate]
                        parent_properties = resolve_project(parent_project)
                        inherited_properties = dict(parent_properties)
                        inherited_management = list(parent_project.get("dependency_management", []))
                        resolution = "resolved-local"
                        parent_path_value = _relative(candidate, root)
                else:
                    resolution = "not-found-locally"
                    diagnostics.append({
                        "code": "MAVEN_PARENT_NOT_FOUND_LOCALLY",
                        "message": (
                            "Parent POM is not present at the declared local relativePath; "
                            "QIVRA did not access a remote repository."
                        ),
                        "severity": "warning",
                        "evidence": [
                            _evidence(current_path, root),
                            {"parent_coordinates": {
                                key: parent.get(key) for key in ("group", "artifact", "version")
                            }},
                        ],
                    })

        local_properties = dict(project.get("_local_properties", {}))
        combined_properties = {**inherited_properties, **local_properties}
        project["parent_inheritance"] = {
            "resolution": resolution,
            "parent_path": parent_path_value,
            "inherited_properties": inherited_properties,
            "inherited_dependency_management": inherited_management,
        }
        project["_resolved_properties"] = combined_properties

        project["properties"] = local_properties
        java_raw = dict(project.get("_java_raw", {}))
        for key in ("release", "source", "target"):
            inherited_value = combined_properties.get(f"maven.compiler.{key}")
            if key == "release" and not inherited_value:
                inherited_value = combined_properties.get("java.version")
            if key not in java_raw and inherited_value:
                java_raw[key] = inherited_value
        project["java"] = {
            key: _resolve(value, combined_properties)
            for key, value in java_raw.items()
        }
        project["dependencies"] = [
            {
                **dependency,
                **{
                    key: _resolve(dependency.get(key), combined_properties)
                    for key in ("group", "artifact", "version", "scope", "type", "classifier", "optional")
                },
            }
            for dependency in project.get("dependencies", [])
        ]
        project["dependency_management"] = [
            {
                **dependency,
                **{
                    key: _resolve(dependency.get(key), combined_properties)
                    for key in ("group", "artifact", "version", "scope", "type", "classifier", "optional")
                },
            }
            for dependency in project.get("dependency_management", [])
        ]
        project["bom_imports"] = [
            dependency for dependency in project["dependency_management"]
            if dependency["type"] == "pom" and dependency["scope"] == "import"
        ]
        project["plugins"] = [
            {
                **plugin,
                "group": _resolve(plugin.get("group"), combined_properties),
                "artifact": _resolve(plugin.get("artifact"), combined_properties),
                "version": _resolve(plugin.get("version"), combined_properties),
            }
            for plugin in project.get("plugins", [])
        ]
        project["plugin_management"] = [
            {
                **plugin,
                "group": _resolve(plugin.get("group"), combined_properties),
                "artifact": _resolve(plugin.get("artifact"), combined_properties),
                "version": _resolve(plugin.get("version"), combined_properties),
            }
            for plugin in project.get("plugin_management", [])
        ]
        if parent_project is not None:
            if not project["group"]:
                project["group"] = parent_project.get("group")
            if not project["version"]:
                project["version"] = parent_project.get("version")

        resolving.remove(current_path)
        resolved.add(current_path)
        return combined_properties

    for project in projects:
        if project["build_system"] == "maven":
            resolve_project(project)

    for project in projects:
        if project["build_system"] == "maven":
            project.pop("_pom_file", None)
            project.pop("_java_raw", None)
            project.pop("_local_properties", None)
            project.pop("_resolved_properties", None)
    return diagnostics


def _parse_gradle(path, root):
    raw = path.read_text(encoding="utf-8", errors="replace")
    java = {}
    patterns = {
        "source": r"sourceCompatibility\s*=\s*['\"]?([A-Za-z0-9_.]+)",
        "target": r"targetCompatibility\s*=\s*['\"]?([A-Za-z0-9_.]+)",
        "release": r"(?:options\.)?release\s*(?:\.set\s*\()?\s*(?:JavaLanguageVersion\.of\()?\s*['\"]?(\d+)",
        "toolchain": r"languageVersion\s*(?:\.set\s*\()?\s*(?:JavaLanguageVersion\.of\()?\s*['\"]?(\d+)",
    }
    for key, pattern in patterns.items():
        match = re.search(pattern, raw)
        if match:
            value = match.group(1)
            legacy = re.search(r"VERSION_1_(\d+)$", value)
            modern = re.search(r"(\d+)$", value)
            java[key] = f"1.{legacy.group(1)}" if legacy else modern.group(1) if modern else value

    dependencies = []
    dep_pattern = re.compile(
        r"\b(implementation|api|compileOnly|runtimeOnly|testImplementation|testRuntimeOnly|annotationProcessor)\s*\(?\s*['\"]([^:'\"]+):([^:'\"]+)(?::([^'\"]+))?['\"]",
        re.MULTILINE,
    )
    for match in dep_pattern.finditer(raw):
        dependencies.append({
            "configuration": match.group(1), "group": match.group(2),
            "artifact": match.group(3), "version": match.group(4),
        })

    plugins = [
        {"id": match.group(1), "version": match.group(2)}
        for match in re.finditer(r"id\s*\(?\s*['\"]([^'\"]+)['\"](?:\s+version\s+['\"]([^'\"]+)['\"])?", raw)
    ]

    settings_path = next(
        (candidate for candidate in (path.parent / "settings.gradle", path.parent / "settings.gradle.kts") if candidate.is_file()),
        None,
    )
    settings_text = settings_path.read_text(encoding="utf-8", errors="replace") if settings_path else ""
    include_match = re.search(r"include\s*\(?([^\n]+)", settings_text)
    modules = re.findall(r"['\"](:[^'\"]+)['\"]", include_match.group(1)) if include_match else []
    if not modules and include_match:
        modules = re.findall(r"['\"]([^'\"]+)['\"]", include_match.group(1))
    name_match = re.search(r"rootProject\.name\s*=\s*['\"]([^'\"]+)", settings_text)

    return {
        "build_system": "gradle", "name": name_match.group(1) if name_match else path.parent.name,
        "group": None, "version": None, "java": java, "modules": sorted(modules),
        "dependencies": sorted(dependencies, key=lambda item: (item["group"], item["artifact"], item["version"] or "")),
        "plugins": sorted(plugins, key=lambda item: (item["id"], item["version"] or "")),
        "evidence": [_evidence(path, root)] + ([_evidence(settings_path, root)] if settings_path else []),
    }


def analyze(root):
    root = root.resolve()
    build_paths = []
    for directory, dirs, filenames in os.walk(root):
        dirs[:] = sorted(name for name in dirs if name not in IGNORED_DIRS and not name.startswith("."))
        for filename in filenames:
            if filename in BUILD_FILES:
                build_paths.append(Path(directory) / filename)
    build_paths.sort(key=lambda path: _relative(path, root))

    projects, diagnostics = [], []
    for path in build_paths:
        project, diagnostic = _parse_pom(path, root) if path.name == "pom.xml" else (_parse_gradle(path, root), None)
        if diagnostic:
            diagnostics.append(diagnostic)
        elif project:
            project["path"] = _relative(path.parent, root) or "."
            projects.append(project)

    diagnostics.extend(_resolve_local_maven_parents(projects, root))

    if not projects:
        diagnostics.append({
            "code": "NO_SUPPORTED_BUILD_FILE",
            "message": "No Maven pom.xml or Gradle build file was found.",
            "severity": "warning", "evidence": [],
        })

    digest = hashlib.sha256()
    for path in build_paths:
        digest.update(_relative(path, root).encode())
        digest.update(path.read_bytes())

    return {
        "schema_version": SCHEMA_VERSION,
        "analyzer": {"name": "qivra-static-analyzer", "version": __version__},
        "snapshot": {"build_files_sha256": digest.hexdigest()},
        "summary": {
            "project_count": len(projects),
            "maven_count": sum(item["build_system"] == "maven" for item in projects),
            "gradle_count": sum(item["build_system"] == "gradle" for item in projects),
            "diagnostic_count": len(diagnostics),
        },
        "projects": projects,
        "diagnostics": sorted(diagnostics, key=lambda item: (item["code"], item["message"])),
    }
