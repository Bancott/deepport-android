"""把 tools/icons/chat-demo 里的官方 SVG 生成 Android VectorDrawable。

Android 的 res/drawable 不能建子目录，所以源文件放这里、生成物平铺到 res/drawable。
用法：
    python tools/icons/build_vectors.py
"""

import re
from pathlib import Path

ICONS = Path(__file__).resolve().parent
REPO = ICONS.parents[1]
OUT = REPO / "app" / "src" / "main" / "res" / "drawable"

# chat-demo SVG 文件 → res/drawable 文件名
MAPPING = {
    "menu.svg": "ic_menu_lines",
    "deep-think.svg": "ic_deep_think",
    "globe.svg": "ic_globe",
    "new-chat.svg": "ic_action_new_chat",
    "copy.svg": "ic_action_copy",
    "refresh.svg": "ic_action_refresh",
    "play.svg": "ic_action_volume",
    "send.svg": "ic_send",
    "stop.svg": "ic_stop",
}


def parse(svg: str):
    svg_tag = re.search(r"<svg\b[^>]*>", svg, re.S)
    if not svg_tag:
        raise ValueError("不是 SVG")
    view_box = re.search(r'viewBox="([^"]+)"', svg_tag.group(0))
    if not view_box:
        raise ValueError("缺少 viewBox")
    numbers = [float(n) for n in re.split(r"[ ,]+", view_box.group(1).strip())]
    if len(numbers) != 4:
        raise ValueError(f"viewBox 非法：{view_box.group(1)}")
    paths = []
    for raw in re.findall(r"<path\b[^>]*?/>", svg, re.S):
        d = re.search(r'\bd="([^"]+)"', raw, re.S)
        if not d:
            continue
        paths.append(
            {
                "d": re.sub(r"\s+", " ", d.group(1)).strip(),
                "even_odd": "fill-rule=\"evenodd\"" in raw,
            }
        )
    return numbers[0], numbers[1], numbers[2], numbers[3], paths


def vector(name: str, svg: str) -> str:
    min_x, min_y, width, height, paths = parse(svg)
    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        f"<!-- 官方 chat-demo 图标，源文件 tools/icons/chat-demo/{name} -->",
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
        f'    android:width="{width:g}dp"',
        f'    android:height="{height:g}dp"',
        f'    android:viewportWidth="{width:g}"',
        f'    android:viewportHeight="{height:g}">',
    ]
    indent = "    "
    if min_x != 0 or min_y != 0:
        lines.append(f'    <group android:translateX="{-min_x:g}" android:translateY="{-min_y:g}">')
        indent = "        "
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
    for svg_name, drawable in MAPPING.items():
        svg_path = ICONS / "chat-demo" / svg_name
        xml = vector(svg_name, svg_path.read_text(encoding="utf-8"))
        (OUT / f"{drawable}.xml").write_text(xml, encoding="utf-8", newline="\n")
        print(f"{drawable}.xml  <-  chat-demo/{svg_name}")


if __name__ == "__main__":
    main()
