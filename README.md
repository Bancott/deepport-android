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

### 自绘矢量图标（实测重绘，全部在 `res/drawable`）

| 文件 | 尺寸/描边 | 说明 |
|---|---|---|
| `ic_menu_lines.xml` | 18 / 10.3 dp 长，1.8dp 粗，方头 | 上长下短两横线（非 Material 三横线） |
| `ic_plus_circle.xml` | Ø24.3，描边 1.9 | 输入卡加号（方头十字 10.6dp） |
| `ic_voice.xml` | Ø24.3，描边 1.9 | 圆圈 + Ø2.5 圆点 + 两道弧（r6 / r9） |
| `ic_deep_think.xml` | 14dp，描边 1.55 | ±45° 双椭圆轨道 + 中心点 |
| `ic_new_chat.xml` | Ø18.4，描边 1.6 | 气泡 + 加号（顶栏新建对话） |
| `ic_deepseek_whale.xml` | 24 viewBox | 鲸鱼 logo，由官方位图轮廓矢量化（nonZero + 反向孔洞） |

> 鲸鱼 logo 第一版用了第三方单路径 + evenOdd，在 Android 上 PathParser 渲染成破碎轮廓；
> 现已改为从官方 PNG 追踪轮廓生成的多环路径，白圈与眼部孔洞稳定。

## 4. 真机校准记录（小米 14 / 23127PN0CC）

`adb shell wm size` → 1200×2670，`wm density` → 480，即 **400×890dp**，与截图归一假设一致。
真机逐项比对后修正：问候语 22sp → 20sp；logo 32dp → 44dp；chip 内边距 10 → 13dp、图标 18 → 14dp；
加号/麦克风 28dp → 24dp；按钮间距 8 → 4dp；输入卡内边距 16/14 → 12·10/14·10；免责声明去掉 navbar 内边距。
剩余差异：深度思考图标花瓣弧度约 90% 接近；地球图标暂用 Material `Language`（15dp）。

## 5. 未实现（后续迭代）

1. 朗读仍走系统 `TextToSpeech`；官方 `/v1/tools/tts`（24kHz 单声道裸 PCM，`voice_id=stella|tide`）已经接好调用与解析，待换成 AudioTrack 流式播放
2. 设置里的「语音识别（可选）」三项已作废（ASR 现在走同一个 API Key，`/v1/tools/asr`），下次清理界面
3. 分享还没有做官方那种图片卡片；消息本体不能直接拖选（走「选择文本」弹窗）
4. 数据层仍是 JSON + SharedPreferences，待换 Room
5. 来源列表的真实站点图标（现在用域名首字母 + 按域名散列配色）

> 对话协议已换成官方 chat-demo 同款 Anthropic 兼容接口：
> `POST {origin}/anthropic/v1/messages`，请求体带 `thinking:{type}`、`tools:[deepseek_web_search]`、
> `safety_check`、`title`、`stream`；附件走 `/anthropic/v1/files`（`anthropic-beta: files-api-2025-04-14`），
> ASR 走 `/v1/tools/asr`。事件名与 JSON `type` 一致，来源来自 `web_search_tool_result` 与 `citations_delta`。



## 6. 分支树 / 会话管理 / 附件 / 语音（本轮）

- **分支树**：消息按 `parentId` 存成树，`activeChild` 记录每个父节点选中的子节点；重新生成会在同一父节点下建 sibling 并切过去，动作条左端出现 `‹ 2/2 ›` 可来回切；失败重试原地复用节点，不额外产生分支。整棵树落盘，重启后连选中分支一起恢复。
- **会话管理**：抽屉长按 → 重命名（弹窗，写入 `titleEdited` 后不再被首条问题覆盖）/ 置顶（置顶分组 + 行首图钉）/ 删除（二次确认）；多选的置顶与删除同样写回存储。
- **搜索页**：抽屉顶部搜索框进入独立搜索页，匹配标题与当前分支正文，命中处高亮，点结果直接打开会话。
- **附件**：`+` 展开与官方一致的三块大方块（拍照 / 相册 / 文件，114.7×84dp、间距 8dp）；相册走 Photo Picker，文件走 SAF，内容统一复制进 `files/attachments` 再挂到输入卡（缩略图 + 删除角标）；文本类文件发送时把内容拼进 prompt，图片只带文件名。
- **应用内相机**：CameraX 全屏预览 + 快门，另有闪光灯、前后摄切换、点按对焦（对焦圈淡入 0.9 秒后淡出）；拍完直接作为图片附件回到会话（权限被拒有明确重授权入口）。
- **语音**：按住说话即开录（`MediaRecorder`），整屏椭圆泛蓝光 + 「松手发送，上滑取消」+ 28 根波形条；上滑转红显示「松手取消」；松手后走 OpenAI 兼容的 `/audio/transcriptions` 转写并回填输入框（服务在「设置 → 模型设置 → 语音识别」里单独配，未配置时给出明确提示）。

## 7. 朗读 / 消息编辑 / 智能搜索 / 来源列表

- **朗读**：`Speaker` 包系统 `TextToSpeech`（Manifest 里补了 `TTS_SERVICE` 的 queries，否则 Android 11+ 绑不上引擎）；动作条喇叭读当前回答，顶栏喇叭打开后新回答生成完自动读；朗读前把 Markdown 洗成纯文本（去代码块/强调/链接语法）。没有正文的消息点了会提示"还没有可朗读的正文"。
- **消息编辑**：用户消息长按 → 编辑（多行弹窗），确认后在同一父节点下新建用户节点并重新回答，原答案保留在 `‹n/m›` 分支里。
- **智能搜索**：`搜索` 开关现在真的进请求体（`search: true`），400 时自动退回不带搜索重发一次；响应里若有 `search_results` / `references` / `sources` 就归一化成来源列表。
- **选择文本**：长按菜单 → 选择文本，弹出可自由选段的弹窗（SelectionContainer），底部有「全选并复制」。
- **来源列表**：回答下方「N 个网页」可点，底部 Sheet 列出站点徽标（域名首字母 + 按域名散列配色，离线也有稳定图标）/ 标题 / 摘要，点条目在 App 内置 WebView 里打开（标题「来源」）。
- **输出额度**：`max_tokens` 从 1024 提到 8192 —— 深度思考会把思考过程算进输出额度，1024 经常"想完就没正文"；`finish_reason=length` 时回答下方会提示被截断，正文为空时也会提示"这次没有生成正文，可点重新生成再试"。



---

## 5. 设计口径

- 视觉尺寸以 3.0x（≈400×890dp）归一，真机需 `adb shell wm density` 复核。
- 图标：Material 图标库只用于「地球 / 键盘 / 关闭 / 音量」等通用项；品牌相关图标全部 Canvas 自绘，避免形状偏差。
- 任何颜色都不得在页面里硬编码，一律走 `DsTheme.colors`。

