import argparse
import json
import sys
from pathlib import Path

from . import __version__
from .scanner import analyze


def main() -> int:
    parser = argparse.ArgumentParser(
        prog="qivra-analyzer",
        description="Inspect Maven and Gradle project metadata without executing build scripts.",
    )
    parser.add_argument("path", nargs="?", default=".", help="Repository root to inspect")
    parser.add_argument("--output", "-o", help="Write JSON to this path instead of stdout")
    parser.add_argument("--pretty", action="store_true", help="Indent JSON output")
    args = parser.parse_args()

    root = Path(args.path).expanduser().resolve()
    if not root.exists() or not root.is_dir():
        parser.error(f"project path is not a directory: {root}")

    result = analyze(root)
    result["analyzer"]["version"] = __version__
    payload = json.dumps(result, indent=2 if args.pretty else None, sort_keys=True) + "\n"

    if args.output:
        destination = Path(args.output).expanduser()
        destination.write_text(payload, encoding="utf-8")
    else:
        sys.stdout.write(payload)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
