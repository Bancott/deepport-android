"""Extract VectorDrawable icons from the official DeepSeek Android APK.

The 2.8.0 APK keeps its resource *names* (drawable/ic_menu_outline_24) but
obfuscates the file paths (res/uI.xml).  `aapt2 dump xmltree` can still decode
each compiled vector, so this script turns that output back into ordinary
Android vector XML.

Usage (defaults match this checkout):

    python tools/icons/extract_official_icons.py ^
        --apk "D:\\接收\\DeepSeek_2.8.0.apk" ^
        --aapt2 "D:\\android\\SDK\\build-tools\\35.0.0\\aapt2.exe"

Outputs:
    tools/icons/official/<name>.xml     source copy, one file per official icon
    app/src/main/res/drawable/<name>.xml  consumable copy (same content)

Only vector XML is emitted.  Raster/9-patch/webp icons and the launcher
foreground (which would collide with this clone's own launcher) are skipped.
"""

from __future__ import annotations

import argparse
import re
import struct
import subprocess
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DEFAULT_APK = Path(r"D:\接收\DeepSeek_2.8.0.apk")
DEFAULT_AAPT2 = Path(r"D:\android\SDK\build-tools\35.0.0\aapt2.exe")
SOURCE_OUT = ROOT / "tools" / "icons" / "official"
DRAWABLE_OUT = ROOT / "app" / "src" / "main" / "res" / "drawable"
DRAWABLE_BITMAP_OUT = ROOT / "app" / "src" / "main" / "res" / "drawable-xxxhdpi"

SKIP = {
    # This clone ships its own adaptive launcher icon.
    "ic_launcher_foreground",
}

# The official app ships these settings/attachment icons only as 4x PNGs.
# They still tint correctly through Compose's ColorFilter, so keep the
# originals instead of tracing them.
BITMAP_ICONS = {
    "ic_branch_outline_20",
    "ic_copy_outline",
    "ic_data_regular",
    "ic_exit_out_regular",
    "ic_globe_language_outline_16",
    "ic_globe_language_regular",
    "ic_info_outline",
    "ic_letter_regular",
    "ic_list_pen_regular",
    "ic_mute_audio_outline_24",
    "ic_photo_outline_20",
    "ic_sun_regular",
    "ic_text_mode_outline_20",
    "ic_think_outline_16",
    "ic_type_pdf",
    "ic_type_pic",
    "ic_type_text",
    "ic_type_xls",
    "ic_wand",
}

DIMENSION_ATTRS = {
    "width",
    "height",
    "pivotX",
    "pivotY",
    "translateX",
    "translateY",
}

ENUM_ATTRS = {
    "fillType": {"0": "nonZero", "1": "evenOdd"},
    "strokeLineCap": {"0": "butt", "1": "round", "2": "square"},
    "strokeLineJoin": {"0": "miter", "1": "round", "2": "bevel"},
    "tileMode": {"0": "clamp", "1": "repeat", "2": "mirror"},
}

# Attributes that aapt2 prints as raw numbers but that Android expects as
# plain XML values (`fillType="evenOdd"`, not `fillType="1"`).
RESOURCE_LINE = re.compile(r"resource 0x[0-9a-f]+ drawable/([A-Za-z0-9_]+)$")
FILE_LINE = re.compile(r"\(\) \(file\) (res/\S+) type=XML")
NODE_LINE = re.compile(r"^(\s*)E: (\S+) \(line=\d+\)$")
ATTR_LINE = re.compile(r"^\s*A: (\S+?)\(0x[0-9a-f]+\)=(.*)$")


def run(cmd: list[str]) -> str:
    result = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8")
    if result.returncode != 0:
        raise RuntimeError(
            f"command failed ({result.returncode}): {' '.join(cmd)}\n{result.stderr}"
        )
    return result.stdout


def resource_table(apk: Path, aapt2: Path) -> list[str]:
    return run([str(aapt2), "dump", "resources", str(apk)]).splitlines()


def resource_map(lines: list[str]) -> dict[str, str]:
    """drawable/name -> res/obfuscated.xml, parsed from `aapt2 dump resources`."""
    mapping: dict[str, str] = {}
    for index, line in enumerate(lines):
        match = RESOURCE_LINE.search(line)
        if not match or index + 1 >= len(lines):
            continue
        file_match = FILE_LINE.search(lines[index + 1])
        if file_match:
            mapping[match.group(1)] = file_match.group(1)
    return mapping


def bitmap_files(lines: list[str], wanted: set[str]) -> dict[str, list[str]]:
    """drawable/name -> candidate res/...png paths (all densities)."""
    mapping: dict[str, list[str]] = {}
    current: str | None = None
    for line in lines:
        match = RESOURCE_LINE.search(line)
        if match:
            current = match.group(1) if match.group(1) in wanted else None
            continue
        if current is None:
            continue
        file_match = re.search(r"\(file\) (res/\S+) type=(PNG|WEBP)", line)
        if file_match:
            mapping.setdefault(current, []).append(file_match.group(1))
    return mapping


def png_area(data: bytes) -> int:
    """Cheap width*height for PNG; unknown formats sort last."""
    if data[:8] != b"\x89PNG\r\n\x1a\n" or len(data) < 24:
        return 0
    width, height = struct.unpack(">II", data[16:24])
    return width * height


def strip_raw(value: str) -> tuple[str, str | None]:
    """Return (display value, raw value). aapt2 appends `(Raw: "...")` for strings."""
    raw = None
    raw_match = re.search(r'\s+\(Raw: "(.*)"\)$', value, re.S)
    if raw_match:
        raw = raw_match.group(1)
        value = value[: raw_match.start()]
    return value.strip(), raw


def xml_escape(value: str) -> str:
    return (
        value.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace('"', "&quot;")
    )


def convert_attr(name: str, raw_value: str) -> str:
    value, raw = strip_raw(raw_value)
    if raw is not None:
        # aapt2 prints enum labels and string values inside Raw.
        value = raw
    if value.startswith('"') and value.endswith('"'):
        value = value[1:-1]

    # aapt2 prints compiled references as ids (`?0x01010429`, `@0x0106000b`).
    # Those belong to the official package/theme and cannot be linked here.
    # The clone tints every component icon with ColorFilter, so a flat black
    # (or white where the original used android:color/white) is equivalent.
    if value.startswith("?") or value.startswith("@0x") or value.startswith("?"):
        return "#FFFFFFFF" if value == "@0x0106000b" else "#FF000000"

    if name in DIMENSION_ATTRS:
        value = re.sub(r"\.0+(?=dp$)", "", value)
        if re.fullmatch(r"-?\d+(?:\.\d+)?", value):
            value += "dp"
    elif name in ENUM_ATTRS:
        value = ENUM_ATTRS[name].get(value, value)
    return xml_escape(value)


def decode_vector(xmltree: str, name: str) -> str | None:
    """Convert `aapt2 dump xmltree` text for one drawable into vector XML."""
    roots: list[dict] = []
    stack: list[tuple[int, dict]] = []
    uses_aapt = False

    for line in xmltree.splitlines():
        node_match = NODE_LINE.match(line)
        if node_match:
            indent = len(node_match.group(1))
            tag = node_match.group(2)
            node = {"tag": tag, "attrs": [], "children": []}
            # aapt2 does not emit closing tags: a sibling/ancestor is implied by
            # indentation. Pop everything at the same or deeper level first.
            while stack and indent <= stack[-1][0]:
                stack.pop()
            if stack:
                stack[-1][1]["children"].append(node)
            else:
                roots.append(node)
            stack.append((indent, node))
            if tag.startswith("aapt:"):
                uses_aapt = True
            continue

        attr_match = ATTR_LINE.match(line)
        if attr_match and stack:
            full_name, raw_value = attr_match.group(1), attr_match.group(2)
            attr_name = qualified_name(full_name)
            stack[-1][1]["attrs"].append(
                (attr_name, convert_attr(attr_name.split(":", 1)[-1], raw_value))
            )

    if not roots or roots[0]["tag"] != "vector":
        return None

    # If the root carries a non-default alpha etc. we keep it. Build XML.
    lines = ['<?xml version="1.0" encoding="utf-8"?>']
    lines.append(
        f"<!-- Extracted from official DeepSeek Android 2.8.0: drawable/{name} -->"
    )
    root = roots[0]
    attr_text = " ".join(f'{key}="{value}"' for key, value in root["attrs"])
    ns = 'xmlns:android="http://schemas.android.com/apk/res/android"'
    if uses_aapt:
        ns += ' xmlns:aapt="http://schemas.android.com/aapt"'
    lines.append(f"<vector {ns}")
    # Keep one attribute per line for readable diffs.
    for key, value in root["attrs"]:
        lines.append(f'    {key}="{value}"')
    if root["attrs"]:
        lines[-1] += ">"
    else:
        lines[-1] = lines[-1].rstrip() + ">"
    emit_children(root["children"], 1, lines)
    lines.append("</vector>")
    lines.append("")
    return "\n".join(lines)


def qualified_name(full_name: str) -> str:
    """Turn aapt2's `http://.../android:width` into `android:width`."""
    android_uri = "http://schemas.android.com/apk/res/android:"
    aapt_uri = "http://schemas.android.com/aapt:"
    if full_name.startswith(android_uri):
        return "android:" + full_name[len(android_uri) :]
    if full_name.startswith(aapt_uri):
        return "aapt:" + full_name[len(aapt_uri) :]
    return full_name


def emit_children(children: list[dict], depth: int, lines: list[str]) -> None:
    indent = "    " * depth
    for child in children:
        tag = child["tag"]
        attrs = child["attrs"]
        if tag == "aapt:attr":
            name = next((v for k, v in attrs if k == "name"), "")
            lines.append(f'{indent}<aapt:attr name="{xml_escape(name)}">')
            emit_children(child["children"], depth + 1, lines)
            lines.append(f"{indent}</aapt:attr>")
            continue
        if attrs:
            lines.append(f"{indent}<{tag}")
            for key, value in attrs:
                lines.append(f'{indent}    {key}="{value}"')
            if child["children"]:
                lines[-1] += ">"
                emit_children(child["children"], depth + 1, lines)
                lines.append(f"{indent}</{tag}>")
            else:
                lines[-1] += " />"
        else:
            lines.append(f"{indent}<{tag} />")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--apk", type=Path, default=DEFAULT_APK)
    parser.add_argument("--aapt2", type=Path, default=DEFAULT_AAPT2)
    parser.add_argument("--only", nargs="*", help="Optional icon names (without .xml)")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    if not args.apk.exists():
        print(f"APK not found: {args.apk}", file=sys.stderr)
        return 1
    if not args.aapt2.exists():
        print(f"aapt2 not found: {args.aapt2}", file=sys.stderr)
        return 1

    table = resource_table(args.apk, args.aapt2)
    mapping = resource_map(table)
    wanted = sorted(mapping)
    bitmap_map = bitmap_files(table, BITMAP_ICONS)
    if args.only:
        chosen = set(args.only)
        wanted = [name for name in wanted if name in chosen]
        bitmap_map = {name: files for name, files in bitmap_map.items() if name in chosen}
    if not args.dry_run:
        SOURCE_OUT.mkdir(parents=True, exist_ok=True)
        DRAWABLE_OUT.mkdir(parents=True, exist_ok=True)
        DRAWABLE_BITMAP_OUT.mkdir(parents=True, exist_ok=True)

    extracted = 0
    skipped: list[str] = []
    for name in wanted:
        if name in SKIP or not (name.startswith("ic_") or name.startswith("chat_welcome") or name.startswith("deepseek_logo")):
            continue
        try:
            tree = run([str(args.aapt2), "dump", "xmltree", str(args.apk), "--file", mapping[name]])
        except RuntimeError:
            skipped.append(name)
            continue
        xml = decode_vector(tree, name)
        if xml is None:
            skipped.append(name)
            continue
        if not args.dry_run:
            (SOURCE_OUT / f"{name}.xml").write_text(xml, encoding="utf-8", newline="\n")
            (DRAWABLE_OUT / f"{name}.xml").write_text(xml, encoding="utf-8", newline="\n")
        print(f"{'would extract' if args.dry_run else 'extracted'}  {name}")
        extracted += 1

    bitmap_count = 0
    with zipfile.ZipFile(args.apk) as apk_zip:
        for name in sorted(bitmap_map):
            candidates = []
            for path in set(bitmap_map[name]):
                data = apk_zip.read(path)
                candidates.append((png_area(data), data))
            if not candidates:
                continue
            _, data = max(candidates, key=lambda item: item[0])
            if not args.dry_run:
                (SOURCE_OUT / f"{name}.png").write_bytes(data)
                (DRAWABLE_BITMAP_OUT / f"{name}.png").write_bytes(data)
            print(f"{'would extract' if args.dry_run else 'extracted'}  {name} (png)")
            bitmap_count += 1

    print(
        f"\n{extracted} vector icons + {bitmap_count} bitmap icons; "
        f"skipped {len(skipped)} non-vector entries"
    )
    if skipped:
        print("skipped: " + ", ".join(skipped))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
