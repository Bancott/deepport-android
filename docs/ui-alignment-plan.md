# 与官方 DeepSeek 2.8.0 APK 对齐的修改计划（不含 Q3 右滑抽屉）

以 APK 实测/反编译结果为准；每条独立提交，提交信息前缀 `feat(ui)` / `fix(ui)` / `style(ui)`。
每一轮改完都跑 `:app:assembleDebug` 验证。

| # | 项目 | APK 基准 | 本仓库改动 | 涉及文件 |
|---|---|---|---|---|
| 1 | 一、chip 点击动画 | Lottie 形变：think 433ms（26f@60fps）、globe 500ms（30f@60fps）；选中正放、取消倒放、从当前进度续播；颜色瞬切；图标 16dp、字 14sp/500、间距 4dp | 引入 lottie-compose，打包 APK 里的 `raw/think`、`raw/globe` 两份 JSON；`DsAssistChip` 用 Lottie 取代 90° 旋转；去掉三色 200ms 过渡（瞬切） | `AssistChip.kt`、`build.gradle.kts`、`res/raw/*.json` |
| 2 | 二、选择文本 | 打开「选择文字」模态面板：手机 ModalBottomSheet / 宽屏 Dialog；原生 TextView `setTextIsSelectable(true)`，不预选；17sp/28sp；内边距 24/4/24/(72+inset)；系统手柄 | 新增 `TextSelectionSheet.kt`（AndroidView+TextView）；菜单「选择文本」改为打开该面板；内联选择手柄改为系统 `text_select_handle` 位图 + SrcIn 染色 #6799FE（尺寸取 drawable 固有值） | 新 `TextSelectionSheet.kt`、`MessageContextMenu.kt`、`ChatScreen.kt`、`MessageStream.kt` |
| 3 | 四、修改并创建分支 | 在底部输入卡改；原消息不隐藏，滚到输入框上方 −8dp；其它消息 alpha 0.9、点击取消；输入条变「更新」；发送后建兄弟分支 | 删除居中 `EditMessageDialog` 流程，改为 `ChatViewModel` 的 editing 状态 + 输入卡回填/取消 + 发送建分支；消息列表滚动到编辑消息 | `ChatViewModel.kt`、`ChatScreen.kt`、`InputBar.kt`、`MessageStream.kt`、`ChatUiState.kt` |
| 4 | 五、会话标题 | 16sp；当前 500 / 其余 400；行高 24sp；单行；行高 44dp；左右 16dp；圆角 16dp | 调数值 | `ConversationDrawer.kt` |
| 5 | 六、多选动画 | 头部 AnimatedContent：fade 200ms ease-in-out + SizeTransform；行内 checkbox alpha 200ms、标题右移 30dp 200ms，无 stagger；头 48dp、标题 17sp/500；底栏 61dp | 去掉整块 slide；改为头淡入+尺寸补间；行内 checkbox/标题动画 | `ConversationDrawer.kt` |
| 6 | 七、会话长按菜单 | 208dp 宽、16dp 圆角、内距 12/16/12/16、项高 48dp、字号 16sp、图标 20dp、12dp 间距、1dp×3 分隔线；锚定行：默认贴行上方（需 ≥180px），否则下方；右缘对齐并 clamp；缩放原点贴行边；进入 scale 0.8→1 spring(0.75,3000)，退出 spring(1.0,10000) | 自绘菜单替换 M3 DropdownMenu；实现 PopupPositionProvider + 缩放动画 | `ConversationDrawer.kt`（或新 `SessionMenu.kt`） |
| 7 | 八、三点动效 | ⋯ 无动效（仅 ripple）；动效在菜单块（同第 6 条） | 三点保持静态；菜单动画随第 6 条实现 | `ConversationDrawer.kt` |
| 8 | 九、思考区 | 正文 15sp/24sp（Markdown 全槽位同号）；头 17sp/26sp SemiBold；竖线 1dp@x=8dp，上下缩 3dp，色 #2AFFFFFF/#14000000；块间距 6dp | 思考正文改用 15/24 的 Markdown 渲染；头加 SemiBold；竖线位置/颜色对齐 | `MessageStream.kt`、`Type.kt` |
| 9 | 十、分支切换 | 文本「1」+4dp+「/」+4dp+「2」，15sp/500/18sp；行高 ≥30dp；两侧 20dp chevron，首/末禁用 | 拆三段 Text + Spacer；改字号/行高/禁用态 | `MessageStream.kt` |
| 10 | 十一、加号与附件面板 | 加号 45° 用默认 spring；触摸盒 64dp；面板入场 expandIn 150 + scaleIn 0.6→1 150 + fadeIn 110/40，退场 110/110/50，curve cubic-bezier(0,0,0.58,1)；按钮 3 等宽、间距 8dp、padding 20/20/20/10、字 15/22 | 改旋转 spec；面板动画参数；面板内距/字号 | `InputBar.kt`、`ChatScreen.kt`、`AttachmentPanel.kt` |
| 11 | 十二、朗读音色 | ModalBottomSheet：顶角 24dp、spring(0.9,800)、内容高 586dp、顶栏 56dp；胶囊 180×100dp 流体（AGSL/GLSL）；名称 17sp/400/22、副标题 14sp/400/22、间距 16dp；页点 6dp、颜色 250ms | 改为 ModalBottomSheet；胶囊 180×100 + 流体着色器（API33+ RuntimeShader，低版本退化为动画渐变）；字号/间距/页点 | `VoiceToneDialog.kt`、`res/raw/*.frag`（可选） |
| 12 | 十三、返回动画 | pop fadeIn/fadeOut 700ms；顶栏左键 = 开/关侧边栏（44dp） | 页面切换加 700ms 交叉淡入淡出；顶栏键语义改为 toggle + 44dp | `MainActivity.kt`、`DeepSeekTopBar.kt` |

---

## 完成情况（自动生成）

| # | 项目 | 状态 | commit |
|---|---|---|---|
| 1 | chip Lottie | 完成 | c1d71ed |
| 2 | 选择文字面板 | 完成 | b58622e / 7c69bfd |
| 3 | 输入卡内编辑建分支 | 完成 | e865e16 |
| 4 | 会话标题 16sp/44dp | 完成 | cc1d374 |
| 5 | 多选动画 | 完成 | 4682296 |
| 6 | 会话长按菜单 | 完成 | 2257694 |
| 7 | 三点/菜单动效 | 完成（并入 2257694） | 2257694 |
| 8 | 思考区 15/24 | 完成 | 7d2d2cb |
| 9 | 分支切换 15sp/4dp | 完成 | 44b5ba1 |
| 10 | 加号/附件面板 | 完成 | e8bfcf2 |
| 11 | 朗读音色面板 | 进行中 | — |
| 12 | 返回 700ms 淡入淡出 | 完成 | 4839d79 |

补充：第 11 项「朗读音色」完成于 3b635db。
- 载体 ModalBottomSheet（24dp 顶角 / 586dp 总高 = 56dp 顶栏 + 530dp 内容 / 顶栏 56dp）
- 180×100dp 流体胶囊（Compose Canvas 三团径向渐变循环，12s 周期；官方为 AGSL 着色器）
- 名称 17sp/400/22、副标题 14sp/400/22（紧贴，无额外间距）、页点 6dp 仅颜色 250ms、确认 48dp/左右 24dp/底 32dp
