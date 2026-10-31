#!/usr/bin/env python3
"""Validate required QIVRA documents and their relative Markdown links."""

from __future__ import annotations

import re
import sys
from pathlib import Path
from urllib.parse import unquote, urlsplit

ROOT = Path(__file__).resolve().parents[1]

REQUIRED_FILES = (
    "README.md",
    "CHANGELOG.md",
    "LICENSE",
    "CONTRIBUTING.md",
    "SECURITY.md",
    "docs/PRODUCT_SCOPE.md",
    "docs/ARCHITECTURE.md",
    "docs/LICENSING.md",
    "docs/ROADMAP.md",
    "docs/DECISIONS/0001-platform-foundation.md",
)

# Captures the destination from inline links and image links.
MARKDOWN_LINK = re.compile(r"!?\\[[^\\]]*\\]\\(([^)]+)\\)")


def markdown_files() -> list[Path]:
    return sorted(
        path
        for path in ROOT.rglob("*.md")
        if ".git" not in path.parts and "node_modules" not in path.parts
    )


def clean_target(raw_target: str) -> str:
    target = raw_target.strip()
    if target.startswith("<") and ">" in target:
        target = target[1 : target.index(">")]
    else:
        target = target.split(maxsplit=1)[0] if target else ""
    return target


def check_required_files(errors: list[str]) -> None:
    for relative in REQUIRED_FILES:
        if not (ROOT / relative).is_file():
            errors.append(f"Missing required document: {relative}")


def check_markdown_links(errors: list[str]) -> int:
    checked = 0
    for markdown_file in markdown_files():
        source = markdown_file.relative_to(ROOT)
        content = markdown_file.read_text(encoding="utf-8")

        for match in MARKDOWN_LINK.finditer(content):
            target = clean_target(match.group(1))
            if not target:
                continue

            parsed = urlsplit(target)
            if parsed.scheme or parsed.netloc or not parsed.path:
                continue

            relative_path = Path(unquote(parsed.path))
            if relative_path.is_absolute():
                continue

            resolved = (markdown_file.parent / relative_path).resolve()
            try:
                resolved.relative_to(ROOT)
            except ValueError:
                errors.append(f"{source}: link escapes repository: {target}")
                continue

            if not resolved.exists():
                errors.append(f"{source}: broken local link: {target}")
                continue

            if resolved.is_dir() and not any(
                (resolved / index_name).is_file()
                for index_name in ("README.md", "index.md")
            ):
                errors.append(
                    f"{source}: directory link has no README.md or index.md: {target}"
                )
                continue

            checked += 1

    return checked


def main() -> int:
    errors: list[str] = []
    check_required_files(errors)
    checked_links = check_markdown_links(errors)

    if errors:
        print("Documentation integrity check failed:")
        for error in errors:
            print(f" - {error}")
        return 1

    print(f"Documentation integrity check passed; verified {checked_links} local links.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
