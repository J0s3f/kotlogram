#!/usr/bin/env python3
"""Generate a stable JSON manifest from a Telegram TL api.tl schema.

The manifest is intentionally descriptive: it is the versioned input for a future
Kotlin/Rust raw-API code generator, and lets CI prove that the committed raw API
surface matches the schema consumed by grammers.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
from pathlib import Path


def parse_functions(schema: str) -> tuple[int, list[dict[str, str | int]]]:
    layer_match = re.search(r"^\s*//\s*LAYER\s+(\d+)\s*$", schema, re.MULTILINE)
    if layer_match is None:
        raise ValueError("The schema does not declare a TL layer")
    marker = "---functions---"
    if marker not in schema:
        raise ValueError("The schema does not contain a functions section")

    functions_source = schema.split(marker, 1)[1]
    without_comments = "\n".join(line.split("//", 1)[0] for line in functions_source.splitlines())
    functions: list[dict[str, str | int]] = []
    for candidate in without_comments.split(";"):
        declaration = " ".join(candidate.split())
        if not declaration or "=" not in declaration:
            continue
        left, result = (part.strip() for part in declaration.split("=", 1))
        name_match = re.match(r"^([A-Za-z0-9_.]+)#([0-9a-f]+)(?:\s|$)", left)
        if name_match is None:
            raise ValueError(f"Function declaration is missing a constructor ID: {declaration}")
        name = name_match.group(1)
        constructor_id = int(name_match.group(2), 16)
        if constructor_id >= 2**31:
            constructor_id -= 2**32
        parameters = left[name_match.end() :].strip()
        functions.append(
            {
                "name": name,
                "constructorId": constructor_id,
                "parameters": parameters,
                "result": result,
                "declaration": f"{declaration};",
            }
        )
    if not functions:
        raise ValueError("No functions were parsed from the schema")
    return int(layer_match.group(1)), functions


def generate(schema_path: Path) -> str:
    schema = schema_path.read_text(encoding="utf-8")
    layer, functions = parse_functions(schema)
    document = {
        "format": "kotlogram-raw-schema/v1",
        "layer": layer,
        "schemaSha256": hashlib.sha256(schema.encode("utf-8")).hexdigest(),
        "functions": functions,
    }
    return json.dumps(document, ensure_ascii=False, indent=2, sort_keys=True) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--schema", type=Path, required=True, help="Path to grammers api.tl")
    parser.add_argument("--output", type=Path, required=True, help="Generated manifest path")
    parser.add_argument("--check", action="store_true", help="Fail instead of overwriting a stale manifest")
    args = parser.parse_args()

    generated = generate(args.schema)
    if args.check:
        if not args.output.is_file() or args.output.read_text(encoding="utf-8") != generated:
            raise SystemExit(f"Raw schema manifest is stale: {args.output}")
        return 0

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(generated, encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
