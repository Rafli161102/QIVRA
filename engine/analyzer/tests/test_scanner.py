import json
import tempfile
import unittest
from pathlib import Path

from qivra_analyzer.scanner import analyze


class AnalyzerTests(unittest.TestCase):
    def write(self, root, relative, content):
        path = root / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8")
        return path

    def test_maven_single_module_and_java_properties(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            self.write(root, "pom.xml", """<project xmlns="http://maven.apache.org/POM/4.0.0">
              <modelVersion>4.0.0</modelVersion><groupId>org.example</groupId>
              <artifactId>sample</artifactId><version>1.2</version>
              <properties><maven.compiler.release>17</maven.compiler.release></properties>
              <dependencies><dependency><groupId>org.slf4j</groupId><artifactId>slf4j-api</artifactId><version>2.0.9</version></dependency></dependencies>
            </project>""")
            result = analyze(root)
            self.assertEqual(result["summary"]["maven_count"], 1)
            self.assertEqual(result["projects"][0]["java"]["release"], "17")
            self.assertEqual(result["projects"][0]["dependencies"][0]["artifact"], "slf4j-api")

    def test_maven_multi_module_and_nested_projects(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            self.write(root, "pom.xml", "<project><artifactId>parent</artifactId><modules><module>core</module><module>service</module></modules></project>")
            self.write(root, "core/pom.xml", "<project><artifactId>core</artifactId></project>")
            self.write(root, "service/pom.xml", "<project><artifactId>service</artifactId></project>")
            result = analyze(root)
            self.assertEqual(result["summary"]["project_count"], 3)
            self.assertEqual(result["projects"][0]["modules"], ["core", "service"])

    def test_gradle_settings_java_toolchain_and_dependency(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            self.write(root, "settings.gradle.kts", 'rootProject.name = "sample"\ninclude(":app", ":lib")')
            self.write(root, "build.gradle.kts", """plugins { id("java") version "1.0" }
java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
dependencies { implementation("org.slf4j:slf4j-api:2.0.9") }
""")
            result = analyze(root)
            project = result["projects"][0]
            self.assertEqual(project["build_system"], "gradle")
            self.assertEqual(project["java"]["toolchain"], "17")
            self.assertEqual(project["modules"], [":app", ":lib"])
            self.assertEqual(project["dependencies"][0]["group"], "org.slf4j")

    def test_gradle_groovy_settings_and_java_compatibility(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            self.write(root, "settings.gradle", "rootProject.name = 'groovy-app'\ninclude 'app', 'shared'")
            self.write(root, "build.gradle", "sourceCompatibility = '11'\ntargetCompatibility = '17'\n")
            result = analyze(root)
            project = result["projects"][0]
            self.assertEqual(project["name"], "groovy-app")
            self.assertEqual(project["modules"], ["app", "shared"])
            self.assertEqual(project["java"]["source"], "11")
            self.assertEqual(project["java"]["target"], "17")

    def test_malformed_maven_is_reported(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            self.write(root, "pom.xml", "<project><broken>")
            result = analyze(root)
            self.assertEqual(result["diagnostics"][0]["code"], "MAVEN_POM_MALFORMED")
            self.assertEqual(result["diagnostics"][0]["evidence"][0]["file"], "pom.xml")

    def test_mixed_build_files_are_discovered_deterministically(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            self.write(root, "pom.xml", "<project><artifactId>maven</artifactId></project>")
            self.write(root, "gradle/build.gradle", "sourceCompatibility = '11'")
            first = analyze(root)
            second = analyze(root)
            self.assertEqual(first, second)
            self.assertEqual(first["summary"]["project_count"], 2)
            self.assertEqual(json.dumps(first, sort_keys=True), json.dumps(second, sort_keys=True))

    def test_generated_directories_are_ignored(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            self.write(root, "target/ignored/pom.xml", "<project><artifactId>ignored</artifactId></project>")
            result = analyze(root)
            self.assertEqual(result["summary"]["project_count"], 0)
            self.assertEqual(result["diagnostics"][0]["code"], "NO_SUPPORTED_BUILD_FILE")

    def test_build_scripts_are_never_executed(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            marker = root / "should-not-exist"
            self.write(root, "build.gradle", f"throw new RuntimeException('{marker}')")
            analyze(root)
            self.assertFalse(marker.exists())


if __name__ == "__main__":
    unittest.main()
