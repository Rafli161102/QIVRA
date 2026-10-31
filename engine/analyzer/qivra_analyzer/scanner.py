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


def _text(element, name):
    child = next((node for node in list(element) if _local(node.tag) == name), None)
    return (child.text or "").strip() if child is not None and child.text else None


def _local(tag):
    return tag.rsplit("}", 1)[-1]


def _relative(path, root):
    return path.relative_to(root).as_posix()


def _evidence(path, root):
    return {"file": _relative(path, root)}


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

    properties_node = next((node for node in list(document) if _local(node.tag) == "properties"), None)
    props = {_local(node.tag): (node.text or "").strip() for node in list(properties_node)} if properties_node is not None else {}

    def resolve(value):
        if not value:
            return None
        previous = None
        while value != previous:
            previous = value
            value = re.sub(r"\$\{([^}]+)\}", lambda match: props.get(match.group(1), match.group(0)), value)
        return value

    java = {}
    for key in ("release", "source", "target"):
        value = props.get(f"maven.compiler.{key}")
        if key == "release" and not value:
            value = props.get("java.version")
        if value:
            java[key] = resolve(value)

    for node in document.iter():
        if _local(node.tag) == "maven-compiler-plugin":
            config = next((child for child in list(node) if _local(child.tag) == "configuration"), None)
            if config is not None:
                for key in ("release", "source", "target"):
                    value = _text(config, key)
                    if value:
                        java[key] = resolve(value)

    modules_node = next((node for node in list(document) if _local(node.tag) == "modules"), None)
    modules = [(node.text or "").strip() for node in list(modules_node)] if modules_node is not None else []
    modules = [value for value in modules if value]

    dependencies = []
    for dep in document.iter():
        if _local(dep.tag) != "dependency":
            continue
        group, artifact = _text(dep, "groupId"), _text(dep, "artifactId")
        if group and artifact:
            dependencies.append({
                "group": resolve(group), "artifact": resolve(artifact),
                "version": resolve(_text(dep, "version")), "scope": _text(dep, "scope"),
            })

    plugins = []
    for plugin in document.iter():
        if _local(plugin.tag) != "plugin":
            continue
        artifact = _text(plugin, "artifactId")
        group = _text(plugin, "groupId") or "org.apache.maven.plugins"
        if artifact:
            plugins.append({"group": group, "artifact": artifact, "version": resolve(_text(plugin, "version"))})

    artifact = _text(document, "artifactId") or path.parent.name
    group = _text(document, "groupId")
    parent = next((node for node in list(document) if _local(node.tag) == "parent"), None)
    if not group and parent is not None:
        group = _text(parent, "groupId")

    return {
        "build_system": "maven", "name": artifact, "group": group,
        "version": _text(document, "version"), "java": java, "modules": modules,
        "dependencies": sorted(dependencies, key=lambda item: (item["group"], item["artifact"], item["version"] or "")),
        "plugins": sorted(plugins, key=lambda item: (item["group"], item["artifact"], item["version"] or "")),
        "evidence": [_evidence(path, root)],
    }, None


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
    }, None


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
        project, diagnostic = _parse_pom(path, root) if path.name == "pom.xml" else _parse_gradle(path, root)
        if diagnostic:
            diagnostics.append(diagnostic)
        elif project:
            project["path"] = _relative(path.parent, root) or "."
            projects.append(project)

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
