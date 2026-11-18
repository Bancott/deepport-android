# 图标源文件

Android 的 `res/drawable` **不能建子目录**（资源目录必须平铺，只允许 `drawable-*` 这类限定符），
所以图标源、映射和生成脚本统一放这里；`app/src/main/res/drawable/ic_*.xml` 是生成物或手工维护的矢量。

## 目录

```
tools/icons/
├── official/           # 官方 Android 2.8.0 APK 直接提取的组件图标（117 个 VectorDrawable + 19 个 4x PNG）
├── chat-demo/          # 官方 chat-demo src/components/base/icons.tsx 里的 SVG（原样 vendored，带 currentColor）
│   ├── menu.svg        # IconMenuOutline20
│   ├── deep-think.svg  # IconThinkOutline14
│   ├── globe.svg       # IconGlobeLanguageOutline14
│   ├── new-chat.svg    # IconNewChatOutline20
│   ├── copy.svg        # IconCopyOutline16
│   ├── refresh.svg     # IconRefreshOutline16
│   ├── play.svg        # IconPlayMediaOutline16（朗读）
│   ├── send.svg        # IconSendOutline16
│   └── stop.svg        # IconStopFill16
├── traced/             # 按官方安卓 3x 截图描的源 SVG（已由 official/ 取代，保留作追溯）
│   ├── voice.svg       # 语音输入：圆环 + 圆点 + 两道声波
│   ├── share.svg       # 分享箭头
│   └── speaker.svg     # 顶栏朗读开关的喇叭
├── extract_official_icons.py  # APK resources + aapt2 → official/ 与 res/drawable/
├── build_vectors.py    # chat-demo/*.svg → res/drawable/*.xml
└── README.md
```

## 官方 APK 图标（当前主用）

`official/` 和 `app/src/main/res/drawable/ic_*.xml` 里的图标名与官方资源同名，
由 `extract_official_icons.py` 从 2.8.0(297) 的 `resources.arsc` + `aapt2 dump xmltree`
还原。它解决了两类误差：

- 之前 chat-demo 的 Web SVG 与 Android 端的线宽/光学留白不同（菜单、新对话、发送、停止、复制、朗读）；
- 语音输入、加号圈、键盘、分享之前是描截图，官方 APK 里其实都有精确 VectorDrawable。

重新提取：

```powershell
python tools/icons/extract_official_icons.py `
  --apk "D:\接收\DeepSeek_2.8.0.apk" `
  --aapt2 "D:\android\SDK\build-tools\35.0.0\aapt2.exe"
```

APK 里的 `ic_data_regular`、`ic_user_regular`、`ic_wand`、`ic_think_outline_16` 等少数图标
只有 PNG；脚本会按资源名挑选最高密度版本，同时写入 `official/*.png` 与
`res/drawable-xxxhdpi/*.png`，不需要手工另存。

## 映射

| 源文件 | 生成 / 对应 drawable | 用途 |
|---|---|---|
| `chat-demo/menu.svg` | `ic_menu_lines.xml` | 顶栏会话列表 |
| `chat-demo/deep-think.svg` | `ic_deep_think.xml` | 深度思考 chip |
| `chat-demo/globe.svg` | `ic_globe.xml` | 智能搜索 chip / 来源 |
| `chat-demo/new-chat.svg` | `ic_action_new_chat.xml` | 顶栏新建对话 |
| `chat-demo/copy.svg` | `ic_action_copy.xml` | 复制 |
| `chat-demo/refresh.svg` | `ic_action_refresh.xml` | 重新生成 / 重试 |
| `chat-demo/play.svg` | `ic_action_volume.xml` | 动作条朗读 |
| `chat-demo/send.svg` | `ic_send.xml` | 发送按钮 |
| `chat-demo/stop.svg` | `ic_stop.xml` | 生成中停止 |
| `traced/voice.svg` | `ic_voice.xml` | 语音输入（手工校准，不参与自动生成） |
| `traced/share.svg` | `ic_action_share.xml` | 分享（手工校准） |
| `traced/speaker.svg` | `ic_volume_speaker.xml` | 顶栏朗读开关（手工校准） |

## 生成

```bash
python tools/icons/build_vectors.py
```

脚本只做固定映射，不会碰 `traced/` 下对应的三个手绘矢量。新增 chat-demo 图标时：
把 SVG 放进 `chat-demo/`、在 `build_vectors.py` 的 `MAPPING` 里加一行，再跑脚本。

## 约定

- 生成物统一命名 `ic_*.xml`，`fillColor` 一律 `#FF000000`，由 Compose 的 `SvgIcon` 用 `ColorFilter.tint` 上色。
- 官方 SVG 的 `viewBox` 原样保留（含负原点的用 `<group android:translateX/Y>` 平移）。
- 尺寸在调用处按 3x 截图校准（见根目录 README 的图标表），不要改 SVG 几何。
