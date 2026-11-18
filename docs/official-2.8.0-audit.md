# 官方 DeepSeek Android 2.8.0(297) 审计与改进清单

审计对象：`D:\接收\DeepSeek_2.8.0.apk`
- package：`com.deepseek.chat`
- versionName / versionCode：`2.8.0` / `297`
- minSdk / targetSdk：24 / 36
- 技术栈：Kotlin + Jetpack Compose（无 WebView 主 UI），R8 混淆，MMKV 本地设置，私有 `/api/v0/*` 协议

本项目的定位是**公开 API 的原生复刻**，不是官方 APK 的破解版或代理壳。因此官方私有账号、会话云同步、分享链接、实时语音通话等能力只作为设计参考，不直接照搬协议。

---

## 1. 这次从 APK 里确认到的关键逻辑

### 1.1 设置与个性化
- 官方设置分组：`账户`（账号管理 / 数据管理）→ `应用`（语言 / 外观 / 字体大小 / 个性化）→ `语音`（朗读音色）→ `关于`（检查更新 / 服务协议）→ 帮助与反馈 → 退出登录。
- `个性化` 只有一项「思考内容自动折叠」。源码里的开关状态由**远程开关 + 本地偏好**共同决定：
  - 远程默认值：`kv_remote_settings_thinking_auto_fold_enabled`
  - 本地用户值：`key_thinking_auto_fold_enabled`，默认 `true`
  - 最终值 = 远程允许 && 本地开启（反编译类 `p000/ip8.java` 第 20–44 行）。
- 字体大小不是简单 3 档：官方 `ns4` / `js4` 定义了 7 个偏好档（-2…4），对应约 `0.8 / 0.9 / 1.0 / 1.1 / 1.2 / 1.3 / 1.45` 的缩放区间，并有独立的「跟随系统」状态（`FontSizeManager.kt`、`FontSizePreferencePage.kt`）。
- 外观是「系统 / 浅色 / 深色」三选一，确认后立即换主题；官方没有把「外观」做成独立子页。

### 1.2 模型与会话
- 2.8.0 的模型选择是 `default` / `expert` / `vision`，UI 文案为 Instant / Expert / Vision；切换模型要求**新开会话**（字符串 `session_model_name_tag_toast`）。
- 会话、消息、分支都在官方服务端；客户端只做流式渲染和本地缓存。官方协议是私有 `/api/v0/chat/completion`、`/edit_message`、`/regenerate`、`/continue`、`/chat/stop_stream`。
- 分支模型：同一父消息下多个子消息，UI 用 `‹ n/m ›` 切换；长按用户消息可「编辑并创建分支」。
- 会话管理：重命名、置顶、删除、多选、会话内全文搜索；置顶上限由远程配置下发（`pinned_session_limit`）。
- 官方的「随停随发」和「允许并发生成」是远程配置项（`interrupt_and_send_enabled`、`allow_parallel_streams`），不是写死的交互。

### 1.3 输入、附件与语音
- 输入卡：深度思考 / 智能搜索 chip + 加号 + 麦克风/键盘 + 发送/停止；加号是一个 26dp 的圆圈加号，展开后旋转 45° 变叉。
- 附件：拍照 / 相册 / 文件三块；文件上传走 `/api/v0/file/upload_file`，有文件数量、大小、token 上限，且**智能搜索开启时不能带文件**（`file_cannot_with_search_toast`、`hint_message_search_with_file_error`）。
- 语音输入走 `/api/v0/asr/ws`，朗读走 `/api/v0/chat/tts/*`；官方有自动朗读开关、音色切换、每日限额、语言不支持等细分 toast。
- 朗读音色官方有「海星 / 暗潮」等，接口实际可用档位与 App 展示档位可能不同；本项目已在 `SettingsStore.TTS_SUPPORTED_VOICES` 里做了降级。

### 1.4 消息与内容
- 官方支持消息反馈（喜欢 / 不喜欢 / 举报）、消息内搜索、表格预览与导出、Mermaid 图表渲染与保存、图片预览/裁剪/保存。
- 分享有两套：服务端分享链接（`/api/v0/share/create`、`/share/fork`、`/share/list`）和本地分享图片。前者需要官方登录态，本项目只做后者。
- 官方有实时语音通话（`/api/v0/call/*`、`CallOrbTextureView`、`CallForegroundService`），这是 2.8.0 的新增大功能，不在公开 API 范围内。

### 1.5 组件图标（本次重点）
APK 的资源名没有混淆，只有文件路径被混淆（例如 `drawable/ic_menu_outline_24` → `res/uI.xml`）。因此可以用
`aapt2 dump resources` 拿到「资源名 → 文件」，再用 `aapt2 dump xmltree` 还原 VectorDrawable。

本次提取：
- 117 个 VectorDrawable：菜单、新对话、语音、键盘、加号圈、发送、停止、复制、刷新、分享、播放/停止朗读、分支箭头、搜索、关闭、勾选、置顶、删除、编辑、文件类型等。
- 19 个 4x PNG：`ic_think_outline_16`、`ic_globe_language_outline_16`、`ic_user_regular`、`ic_data_regular`、`ic_wand`、`ic_info_outline`、`ic_copy_outline`、`ic_type_pdf/xls/text/pic` 等（官方只提供了位图）。

提取脚本与源文件：
- `tools/icons/extract_official_icons.py`
- `tools/icons/official/`（源）
- `app/src/main/res/drawable/ic_*.xml` + `app/src/main/res/drawable-xxxhdpi/*.png`（App 使用）

真机验收截图（小米 14 / 1200×2670）：
- 首页与输入卡：`docs/screenshots/home-official-icons.png`
- 设置页：`docs/screenshots/settings-official-icons.png`
- 深色「外观」弹窗：`docs/screenshots/appearance-dialog-dark.png`

---

## 2. 这次已经落地的改进

| 项 | 状态 | 说明 |
|---|---|---|
| 官方组件图标 | ✅ | 顶栏、输入卡、动作条、长按菜单、抽屉、附件面板、相机、搜索、来源页、设置页全部换为 APK 同名图标 |
| 图标提取脚本 | ✅ | 可重复从 APK 还原；跳过启动图标避免覆盖本项目自适应图标 |
| 设置页对齐 | ✅ | 增加「账号管理」「帮助与反馈」，行高 48→56dp，图标/箭头/字号按官方截图校准 |
| 思考自动折叠 | ✅ | 开关持久化到 `SettingsStore`，并真正影响流式回答开始后的折叠行为 |
| 文件类型图标 | ✅ | PDF / Excel / 文本 / 图片分别使用官方 `ic_type_*` |
| 账号管理诚实空态 | ✅ | 去掉硬编码手机号/邮箱/微信，改为「当前账号 / 本地复刻版 / 未登录」 |
| 外观/语言弹窗深色 | ✅ | `Dialog` 是独立窗口，显式再提供 `DeepSeekTheme`，不再在深色模式弹白窗 |
| 顶栏朗读三态 | ✅ | 关闭 `ic_mute_audio_outline_24`、开启 `ic_play_media_outline_24`、朗读中 `ic_playing_fill_24`，去掉手绘斜线/声波 |
| 菜单图标与分隔线 | ✅ | 「选择文本」改官方 `ic_select_outline`；功能组之间加 0.6dp 细分隔线 |
| 外观太阳/月亮 | ✅ | 改为跟随最终生效主题（`isDarkTheme = dark`），不再只跟系统 |
| Chip 点击动画 | ✅ | 图标 90° 旋转 + 颜色/底色 200–240ms 过渡 |
| 多选平移动效 | ✅ | `AnimatedContent` 横向 slide + fade，替换原来的硬切 |
| 选择文本粒度 | ✅ | 长按点按词边界进入选区，不再整块选中 |
| 附件面板闪动 | ✅ | 面板收起期间锁存键盘高度，避免输入栏第一帧下坠 |
| 死代码清理 | ✅ | 移除长按菜单里无实际动作的「朗读」项 |
| 设置页返回/箭头 | ✅ | 全部改为官方 `ic_chevron_*` |
| 构建验证 | ✅ | `:app:assembleDebug` 通过，真机安装启动正常 |

仍未做：
- 官方 7 档字体尚未完全对齐（当前 6 档 0.85–1.3）。
- 官方模型三档（Instant / Expert / Vision）尚未映射到公开 API。

---

## 3. 改进清单（按优先级）

### P0：直接影响当前可用性/正确性
1. **模型选择与思考开关解耦**
   - 现状：`ModelSettingsScreen` 让用户手填模型 id，输入框/接口配置对普通用户过重。
   - 建议：提供「Instant / Expert」两个预设（映射 `deepseek-chat` / `deepseek-reasoner`），高级设置里再保留自定义 model id；「Vision」在公开 API 不支持前置灰并说明原因。
   - 影响：降低配置成本，贴近官方 2.8 的三档模型认知。

3. **字体档位对齐官方 7 档**
   - 现状：6 档，0.85–1.3，无独立「跟随系统」布尔值。
   - 建议：采用官方 `-2…4` 七档（约 0.8 / 0.9 / 1.0 / 1.1 / 1.2 / 1.3 / 1.45），`fontScale=0` 继续表示跟随系统；预览区和正文共用同一档位。
   - 影响：长文阅读和小屏可用性；官方截图可直接对照验收。

4. **附件限制前置校验**
   - 现状：附件可无限添加，发送时才上传；搜索开启时仍可带文件。
   - 建议：按官方规则在选文件时就限制数量/大小/类型；`smartSearch=true` 时禁用附件并提示「智能搜索开启时不能发送文件」。
   - 影响：减少无效上传和接口报错。

### P1：明显提升复刻度/可用性
5. **会话内搜索**
   - 官方有 `message_search_button` / `message_search_page_ttitle`；本项目只有会话标题搜索。
   - 建议：在会话页顶栏或长按菜单加入「搜索当前对话」，命中后滚动到对应消息并高亮关键词。

6. **消息反馈（喜欢/不喜欢）**
   - 官方动作条/长按菜单有 feedback（`message_feedback`、`message_feedback_tag_*`）。
   - 建议：即使没有官方后端，也可以先做本地「有帮助/没帮助」记录 + 反馈文本导出；以后接后端只换 Repository。

7. **表格与 Mermaid**
   - 官方有 `TablePreviewActivity`、`MermaidViewerWebView`、`markdown_table_export_failed` 等。
   - 建议：先做 Markdown 表格的横向滚动 + 复制/导出 CSV；Mermaid 可用 WebView 渲染，注意离线 asset 与安全策略。

8. **分享图片增强**
   - 现状：Canvas 生成 1080 宽 PNG，纯文本。
   - 建议：支持选择消息范围、包含来源/思考折叠状态、长文分页；官方还有「保存到相册」而不是只有系统分享。

9. **朗读状态与错误细分**
   - 官方有 `read_aloud_daily_limit_reached_toast`、`read_aloud_language_not_supported_toast`、`read_aloud_low_volume_toast` 等。
   - 建议：把当前统一的 toast 拆成「限额 / 网络 / 音色不支持 / 被其他 App 打断」四类，并在播放条上显示可恢复操作。

### P2：锦上添花/需要后端
10. **服务端分享链接**：`/api/v0/share/create`、`/share/fork`、`/share/list`；需要官方登录态，公开 API 不可用。
11. **实时语音通话**：`/api/v0/call/*`、`CallOrbTextureView`；工作量大，建议单独立项。
12. **多设备登录管理**：官方 `login_devices_*`；需要账号体系。
13. **桌面快捷方式/小组件**：官方有 `CameraAskTileService`、`PictureAskTileService`、`ChatTileService`；可先做「新建对话 / 拍照提问 / 图片提问」三个快捷方式。
14. **检查更新**：官方走 `/api/v0/check_client_update`；本项目可改成 GitHub Release/自建版本接口，不再显示假的 `0.1.0(1)`。

---

## 4. 验收建议

- 图标：在浅色/深色下各截一张首页、输入卡、动作条、设置页，与 `reference/official-screenshots/` 逐项比对线宽和光学尺寸。
- 个性化：开启/关闭「思考内容自动折叠」→ 发一条会触发思考的问题 → 正文开始后观察思考块是否保持展开；杀进程重启后开关状态应保留。
- 字体：7 档逐档检查输入卡不溢出、消息气泡不裁切、设置页不换行。
- 附件：开启智能搜索 → 点加号 → 应无法选文件并给出明确提示；关闭后恢复。
- 构建：`gradlew :app:assembleDebug` + 真机安装，确认无资源链接错误和运行时崩溃。
