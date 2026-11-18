"""从官方 chat-demo 的 icons.tsx 抽取 SVG path，生成 Android VectorDrawable。

用法：
    python tools/import_chatdemo_icons.py <chat-demo 仓库根目录>

例如：
    git clone --depth 1 https://github.com/deepseek-ai/chat-demo.git ../chat-demo
    python tools/import_chatdemo_icons.py ../chat-demo

只覆盖 chat-demo 里有明确等价的图标；语音输入、分享、喇叭在 demo 里没有对应图标，
继续用 res/drawable 里按安卓 3x 截图描的矢量，不受本脚本影响。
"""

import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
OUT = REPO / "app" / "src" / "main" / "res" / "drawable"

MAPPING = {
    "ic_deep_think": "IconThinkOutline14",
    "ic_globe": "IconGlobeLanguageOutline14",
    "ic_menu_lines": "IconMenuOutline20",
    "ic_action_new_chat": "IconNewChatOutline20",
    "ic_action_copy": "IconCopyOutline16",
    "ic_action_refresh": "IconRefreshOutline16",
    "ic_action_volume": "IconPlayMediaOutline16",
    "ic_send": "IconSendOutline16",
    "ic_stop": "IconStopFill16",
}


def parse_icon(body: str):
    svg = re.search(r"<svg\b[^>]*>", body, re.S)
    if not svg:
        return None
    tag = svg.group(0)
    view_box = re.search(r'viewBox="([^"]+)"', tag)
    if not view_box:
        return None
    if "clipPath" in body or "<g " in body:
        return None
    paths = []
    for raw in re.findall(r"<path\b[^>]*?/>", body, re.S):
        d = re.search(r'\bd="([^"]+)"', raw, re.S)
        if not d:
            continue
        paths.append(
            {
                "d": re.sub(r"\s+", " ", d.group(1)).strip(),
                "even_odd": ('fillRule="evenodd"' in raw or 'fill-rule="evenodd"' in raw),
            }
        )
    numbers = [float(n) for n in re.split(r"[ ,]+", view_box.group(1).strip())]
    if len(numbers) != 4:
        return None
    return numbers[0], numbers[1], numbers[2], numbers[3], paths


def make_vector(name: str, body: str) -> str:
    parsed = parse_icon(body)
    if parsed is None:
        raise SystemExit(f"{name}: 解析失败（可能含 clipPath）")
    min_x, min_y, width, height, paths = parsed
    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        f"<!-- 官方 chat-demo 图标 {name}，原样转 Android vector -->",
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
        f'    android:width="{width:g}dp"',
        f'    android:height="{height:g}dp"',
        f'    android:viewportWidth="{width:g}"',
        f'    android:viewportHeight="{height:g}">',
    ]
    if min_x != 0 or min_y != 0:
        lines.append(f'    <group android:translateX="{-min_x:g}" android:translateY="{-min_y:g}">')
        indent = "        "
    else:
        indent = "    "
    for path in paths:
        lines.append(f"{indent}<path")
        lines.append(f'{indent}    android:fillColor="#FF000000"')
        if path["even_odd"]:
            lines.append(f'{indent}    android:fillType="evenOdd"')
        lines.append(f'{indent}    android:pathData="{path["d"]}" />')
    if min_x != 0 or min_y != 0:
        lines.append("    </group>")
    lines.append("</vector>")
    lines.append("")
    return "\n".join(lines)


def main() -> None:
    if len(sys.argv) < 2:
        raise SystemExit("用法：python tools/import_chatdemo_icons.py <chat-demo 仓库根目录>")
    src = Path(sys.argv[1]) / "src" / "components" / "base" / "icons.tsx"
    if not src.exists():
        raise SystemExit(f"找不到 {src}")
    text = src.read_text(encoding="utf-8")
    parts = re.split(r"export const (\w+)", text)
    icons = {parts[i]: parts[i + 1] for i in range(1, len(parts) - 1, 2)}
    for out_name, icon_name in MAPPING.items():
        body = icons.get(icon_name)
        if body is None:
            raise SystemExit(f"找不到图标 {icon_name}")
        xml = make_vector(icon_name, body)
        (OUT / f"{out_name}.xml").write_text(xml, encoding="utf-8", newline="\n")
        print(f"{out_name}.xml  <- {icon_name}  ({len(xml)} bytes)")


if __name__ == "__main__":
    main()
