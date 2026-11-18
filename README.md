# DeepSeek Clone · 原生 Android 复刻

包名 `com.example.deepseekclone`，Kotlin + Jetpack Compose + Material 3，**不使用 WebView 做主 UI**。
视觉与交互基线来自官方 DeepSeek 2.8.0(297) 安卓版截图（小米 14 / 1200×2670 / 3.0x）。

---

## 1. 工具链（已验证可编译）

| 项 | 版本 |
|---|---|
| JDK | 17（Microsoft Build of OpenJDK） |
| Gradle | 8.14 |
| AGP | 8.13.2 |
| Kotlin | 2.1.20 |
| Compose BOM | 2025.04.01 |
| compileSdk / targetSdk / minSdk | 35 / 35 / 26 |

命令行构建（Windows，已在本机验证通过）：

```powershell
$env:JAVA_HOME = "D:\tools\jdk17\jdk-17.0.21.0.2+1"
& "$env:USERPROFILE\.gradle\wrapper\dists\gradle-8.14-all\28mvdsg72witjywy1exbbuc46\gradle-8.14\bin\gradle.bat" `
  -p "D:\深度求索\DeepSeekClone" :app:assembleDebug
```

产物：`app/build/outputs/apk/debug/app-debug.apk`

Android Studio：直接 `File > Open` 选择 `DeepSeekClone` 目录，Sync 后运行即可。

> 工程位于含中文的目录，`gradle.properties` 里已加 `android.overridePathCheck=true`。
> 如果后续遇到 aapt/kotlinc 的路径类报错，把工程移到纯 ASCII 路径（如 `D:\Projects\DeepSeekClone`）即可。

---

## 2. 目录结构

```
app/src/main/java/com/example/deepseekclone/
├── MainActivity.kt                 # 入口（临时演示宿主，接入 ViewModel 后替换）
└── ui/
    ├── theme/
    │   ├── Color.kt                # 语义色板（浅/深两套，全部来自截图取色）
    │   ├── Type.kt                 # 字号 Token（DsText）+ M3 Typography 映射
    │   ├── Shape.kt                # 圆角 / 尺寸 Token（DsShapes / DsDimen）
    │   └── Theme.kt                # DeepSeekTheme + DsTheme.colors
    ├── common/
    │   ├── Glyphs.kt               # Canvas 自绘图标：两横线菜单 / 加号圈 / 原子 / 麦克风 / 箭头 / 停止
    │   ├── CircleIconButton.kt     # 40dp 圆形图标按钮（带无障碍语义）
    │   ├── AssistChip.kt           # 深度思考 / 智能搜索 chip
    │   └── DeepSeekTopBar.kt       # 全站顶栏
    └── chat/
        ├── ChatUiState.kt          # UiState + 7 态流式状态机枚举
        ├── ChatScreen.kt           # 会话页骨架（空态 + 顶栏 + 输入卡 + 免责声明）
        └── InputBar.kt             # 输入卡（文字 / 按住说话 / 转写中 / 已转写提示 / 发送 / 停止）
```

资源：

```
res/drawable/ic_deepseek_whale.xml      # 鲸鱼矢量 logo（evenOdd，可 tint）
res/drawable/ic_launcher_foreground.xml # 启动图标前景
res/mipmap-anydpi-v26/ic_launcher.xml   # 自适应图标
```

---

## 3. 已实现（本轮）

- 完整 Theme 层：语义色、字号、圆角、尺寸 Token，浅/深自动切换。
- 顶栏：两横线菜单 + 左对齐标题（空态隐藏标题，与官方一致）+ 朗读开关 + 新建对话（气泡加号）。
- 空态：鲸鱼 44dp + 问候语 20sp。
- 输入卡：1dp 描边、28dp 圆角、376dp 宽、左右 12/10dp 内边距；chip 32dp / 图标 14dp / 内边距 13dp；加号与麦克风 24.3dp、间距 4dp；语音模式「按住说话」；转写中；转写回填提示条。
- 无障碍：所有图标按钮带 `contentDescription`，触控区 ≥40dp。
- Preview：浅色空态 / 深色空态 / 深色输入中 / 浅色语音模式。

### 矢量图标（`res/drawable`）

| 文件 | 来源 | 说明 |
|---|---|---|
| `ic_menu_lines.xml` | chat-demo `IconMenuOutline20` | 上长下短两横线（非 Material 三横线） |
| `ic_deep_think.xml` | chat-demo `IconThinkOutline14` | 14dp 原子轨道 + 中心点 |
| `ic_globe.xml` | chat-demo `IconGlobeLanguageOutline14` | 14dp 地球（智能搜索 / 来源） |
| `ic_action_new_chat.xml` | chat-demo `IconNewChatOutline20` | 顶栏新建对话（气泡 + 加号） |
| `ic_action_copy.xml` | chat-demo `IconCopyOutline16` | 复制 |
| `ic_action_refresh.xml` | chat-demo `IconRefreshOutline16` | 重新生成 / 失败重试 |
| `ic_action_volume.xml` | chat-demo `IconPlayMediaOutline16` | 朗读（动作条播放按钮） |
| `ic_send.xml` | chat-demo `IconSendOutline16` | 发送箭头 |
| `ic_stop.xml` | chat-demo `IconStopFill16` | 生成中停止的圆角方块 |
| `ic_plus_circle.xml` | 截图实测重绘 | 输入卡加号（方头十字 10.6dp） |
| `ic_voice.xml` | 截图实测重绘 | 语音输入：圆圈 + 圆点 + 两道弧（chat-demo 没有同款） |
| `ic_action_share.xml` | 用户素材描矢量 | 分享箭头（chat-demo 没有分享按钮） |
| `ic_volume_speaker.xml` | 截图实测重绘 | 顶栏自动朗读开关的喇叭（开/关斜线） |
| `ic_deepseek_whale.xml` | 官方位图轮廓矢量化 | 鲸鱼 logo（nonZero + 反向孔洞） |

> 前 9 个图标由 `tools/import_chatdemo_icons.py` 从 chat-demo 的 `src/components/base/icons.tsx` 直接抽 `path` 生成，
> 几何与官方 Web demo 一致；语音输入、分享、喇叭在 chat-demo 里没有对应图标，继续按 3x 截图描。
> 鲸鱼 logo 第一版用了第三方单路径 + evenOdd，在 Android 上 PathParser 渲染成破碎轮廓；
> 现已改为从官方 PNG 追踪轮廓生成的多环路径，白圈与眼部孔洞稳定。

## 4. 真机校准记录（小米 14 / 23127PN0CC）

`adb shell wm size` → 1200×2670，`wm density` → 480，即 **400×890dp**，与截图归一假设一致。
真机逐项比对后修正：问候语 22sp → 20sp；logo 32dp → 44dp；chip 内边距 10 → 13dp、图标 18 → 14dp；
加号/麦克风 28dp → 24dp；按钮间距 8 → 4dp；输入卡内边距 16/14 → 12·10/14·10；免责声明去掉 navbar 内边距。
剩余差异：深度思考图标花瓣弧度约 90% 接近；地球图标暂用 Material `Language`（15dp）。

## 5. 未实现（后续迭代）

1. 分享还没有做官方那种图片卡片；消息本体不能直接拖选（走「选择文本」弹窗）
2. 数据层仍是 JSON + SharedPreferences，待换 Room
3. 来源列表的真实站点图标（现在用域名首字母 + 按域名散列配色）
4. 朗读暂不处理音频焦点（后台播放/与视频 App 混音未接管）

> 对话协议已换成官方 chat-demo 同款 Anthropic 兼容接口：
> `POST {origin}/anthropic/v1/messages`，请求体带 `system`（助手身份说明）、`thinking:{type}`、
> `tools`（`deepseek_read_url` 常开；智能搜索再追加 `deepseek_web_search`）、`safety_check`、
> `title`（只在会话还没有标题的第一轮请求）、`stream`；
> 标题从 `message_delta.delta.title` 回填，`stop_reason` 与 `usage` 同在该事件里（`max_tokens` → 截断提示）。
> 附件走 `/anthropic/v1/files`（`anthropic-beta: files-api-2025-04-14`，图片不传 `purpose`，
> PDF/Word/PPT 用 `rich_layout_recognition`，其余 `user_data`）；ASR 走 `/v1/tools/asr`，
> TTS 走 `/v1/tools/tts`（返回裸 PCM，边收边播），两者用 `Authorization: Bearer`。对话/文件用 `x-api-key`。
> 事件名与 JSON `type` 一致，来源来自 `web_search_tool_result` 与 `citations_delta`；流内错误帧（`error`）按 JSON 里的 message 抛出。



## 6. 分支树 / 会话管理 / 附件 / 语音（本轮）

- **分支树**：消息按 `parentId` 存成树，`activeChild` 记录每个父节点选中的子节点；重新生成会在同一父节点下建 sibling 并切过去，动作条左端出现 `‹ 2/2 ›` 可来回切；失败重试原地复用节点，不额外产生分支。整棵树落盘，重启后连选中分支一起恢复。
- **会话管理**：抽屉长按 → 重命名（弹窗，写入 `titleEdited` 后不再被首条问题覆盖）/ 置顶（置顶分组 + 行首图钉）/ 删除（二次确认）；多选的置顶与删除同样写回存储。
- **搜索页**：抽屉顶部搜索框进入独立搜索页，匹配标题与当前分支正文，命中处高亮，点结果直接打开会话。
- **附件**：`+` 展开与官方一致的三块大方块（拍照 / 相册 / 文件，114.7×84dp、间距 8dp）；相册走 Photo Picker，文件走 SAF，内容统一复制进 `files/attachments` 再挂到输入卡（缩略图 + 删除角标）；发送时先走 files API 换 `file_id`，图片发 `image` 块、其余发 `document` 块；上传失败会明确报错，不再静默丢附件。
- **应用内相机**：CameraX 全屏预览 + 快门，另有闪光灯、前后摄切换、点按对焦（对焦圈淡入 0.9 秒后淡出）；拍完直接作为图片附件回到会话（权限被拒有明确重授权入口）。
- **语音**：按住说话即开录（`MediaRecorder`），整屏椭圆泛蓝光 + 「松手发送，上滑取消」+ 28 根波形条；上滑转红显示「松手取消」；松手后走 `/v1/tools/asr`（Bearer 鉴权，和对话共用 API Key）转写并回填输入框，失败给出明确提示。

## 7. 朗读 / 消息编辑 / 智能搜索 / 来源列表

- **朗读**：动作条播放按钮 → `/v1/tools/tts` 合成裸 PCM（24kHz / 单声道 / 16bit），用 `AudioTrack` 流式播放：收到首块就出声，边下边写；播放中按钮换成四根波形条（Loading 呼吸、Playing 波动），再点一次立刻静音停止；正常读完会等缓冲播完再释放音轨，尾音不截断。顶栏喇叭打开后新回答生成完自动读；朗读前把 Markdown 洗成纯文本。失败（未填 Key / 合成报错）用 toast 提示。
- **朗读音色**：设置 → 语音 → 朗读音色，底部弹层横向切卡（海星 stella / 暗潮 tide），确认后写入本地，下一次朗读生效（官方接口目前只开放这两档）。
- **设置清理**：删掉已作废的「语音识别（可选）」三项（ASR 现在与对话共用同一个 Key，走 `/v1/tools/asr`），模型设置页只留 API Key / Base URL / 模型列表，并补一句两个语音接口的说明；系统 TTS 相关的 Manifest `queries` 一并移除。
- **消息编辑**：用户消息长按 → 编辑（多行弹窗），确认后在同一父节点下新建用户节点并重新回答，原答案保留在 `‹n/m›` 分支里。
- **智能搜索**：`智能搜索` 开关注入 `tools[deepseek_web_search]`（`deepseek_read_url` 常开）；来源从 `web_search_tool_result` 与正文 `citations_delta` 合并去重。
- **会话标题**：首轮请求带 `title:{enabled:true}`，服务端从提问生成标题经 `message_delta` 下发后回填；用户手动重命名过就不再请求/覆盖。
- **协议真机验证**（2026-11-18，小米 14 + 真实 Key）：服务端标题、深度思考、智能搜索、23 个网页来源与来源 Sheet 全部跑通；files API 的图片/文档块也用同一 Key 验证过（`image` 200、`document` 200）。
- **朗读真机验证**（2026-11-18）：605 字回答合成 4.7MB PCM（约 98 秒），边收边播完整播完并由 `playbackHeadPosition` 确认播完再释放；短回答自然结束、播放中再点立即停止都跑通；「暗潮」音色同样可用。
- **选择文本**：长按菜单 → 选择文本，弹出可自由选段的弹窗（SelectionContainer），底部有「全选并复制」。
- **来源列表**：回答下方「N 个网页」可点，底部 Sheet 列出站点徽标（域名首字母 + 按域名散列配色，离线也有稳定图标）/ 标题 / 摘要，点条目在 App 内置 WebView 里打开（标题「来源」）。
- **输出额度**：新协议不再传 `max_tokens`，由服务端决定上限；思考过程也算输出额度。`stop_reason=max_tokens` 映射成 `length`，回答下方提示被截断，正文为空时也会提示"这次没有生成正文，可点重新生成再试"。



---

## 8. 设计口径

- 视觉尺寸以 3.0x（≈400×890dp）归一，真机需 `adb shell wm density` 复核。
- 图标：Material 图标库只用于「地球 / 键盘 / 关闭 / 音量」等通用项；品牌相关图标全部 Canvas 自绘，避免形状偏差。
- 任何颜色都不得在页面里硬编码，一律走 `DsTheme.colors`。

