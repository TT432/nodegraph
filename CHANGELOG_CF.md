# NodeGraph 1.4.2

## Changed

- **连线渲染 float 化**：斜线段改为 pose 旋转 + 轴对齐填充（float 端点/厚度），
  采样点补缝方块，替换包围盒近似；`screenHalf` 返回 double（下限 0.5px=厚度 1px），
  不再 int 取整。
- **选中描边亚像素贴合**：`NodeRenderer.renderSelectionOutline` 改 4 边 float
  translate+fill，替换屏幕坐标 int floor 描边。

---

# NodeGraph 1.4.1

## Added

- **节点移除监听**：`NodeGraph.removeNode` 在连接级联清理后触发
  `NodeRemoveListener`，宿主可在节点消失前结算连接派生状态；
  与连接/widget 监听一致的同步派发 + 异常隔离。

---

# NodeGraph 1.4.0

## Added

- **子图导航（Blender Group 风格）**：`Node.setSubgraph(NodeGraph)` 挂载子图；双击子图节点进入，
  ESC 或画布顶部面包屑（可点击跳层）返回；每层视角（平移/缩放）独立保存恢复。`NodeGraphWidget`
  新增 `pushSubgraph` / `popSubgraph` / `popToDepth` / `depth` / `canPopSubgraph` / `breadcrumbLabels` /
  `setRootLabel`；`graph()` 语义为当前查看/编辑的图（导航栈顶），`rootGraph()` 返回栈底根图。
  剪贴板编码跳过携带子图的节点（子图为宿主派生视图，复制会静默丢失嵌套内容）。
- **CUSTOM / DISPLAY widget 种类**：`InputWidgetKind.CUSTOM` 行的渲染交给经
  `NodeGraphWidget.registerWidgetRenderer(key, renderer)` 注册的宿主回调（屏幕坐标矩形，
  预览缩略图等）；`InputWidgetKind.DISPLAY` 渲染只读文本（不响应点击编辑）。
  `InputWidgetSpec` / `InputWidget` 新增 `height`（世界单位行高，默认 14），布局按行高累计；
  `InputWidget.setKind` 支持 per 实例只读化覆盖。
- **节点状态描边色**：`Node.setStatusColor(int argb)`（0 = 默认描边），宿主表达派生状态
  （未解析红、初始金等）；视觉属性，不入撤销/剪贴板。
- **Widget 值监听**：`NodeGraph.addWidgetListener(WidgetValueListener)`，命令路径
  （`SetWidgetValueCommand` execute/undo/redo）触发并携带新旧值；直接
  `InputWidget.setCurrentValue`（宿主建图填充）不触发；监听器异常隔离。

## Changed

- **连线线宽改为世界单位**：`ConnectionRenderer.THICKNESS`（2.0）与 `PREVIEW_THICKNESS`（1.0）
  现在是世界单位，随画布缩放等比变化，与节点保持恒定视觉比例（此前为屏幕固定 2px，缩小时
  相对节点过粗）；任何缩放下保底下限 1px 可见。

---

# NodeGraph 1.3.0

## Added

- **连线中点标签**：`Connection.label`（不入 equals/hashCode）与
  `NodeGraph.connect(from, out, to, in, label)` 重载（多输入幂等保留原标签）；
  `ConnectionRenderer.renderLabel` 于连线中点绘制暗底标签（截断 48 字符），
  供宿主承载引用条件（Molang 原文等）展示。

---

# NodeGraph 1.2.0

## Added

- **多输入节点**：新节点类型 `MultiInputNodeDefinition`（区别于仅单输入的 `NodeDefinition`），
  其输入端口可同时接多条 wire，不再触发单源替换；求值函数 `MultiInputNodeFunction` 按键收到
  `List<Object>`（顺序 = 连接创建序，auto-conversion 逐 wire 生效）。`NodeGraph.inputConnections`
  返回端口的全部入线。撤销/重做、剪贴板、环检测语义对两类节点各自成立。

## Changed

- **跨版本**：单一源码经 Stonecutter 产出 Minecraft 1.20.1 (Forge) / 1.21.1 (NeoForge) / 26.1.2 (NeoForge)
  三个变体。版本号改为 `mod_version+{mc_version}-{loader}`（如 `1.2.0+1.21.1-neoforge`），
  后缀为 semver build metadata，标识构建目标平台，不参与优先级比较；同一 mod_version 的各变体功能等价。

---

# NodeGraph 1.1.2

## Added

- **Non-throwing findNode/findGroup lookups**: `NodeGraph.findNode`, `NodeGraph.findGroup`
  now return `null` instead of throwing `IllegalArgumentException` when the key is
  not found, matching the pre-1.1.0 API contract.

---

# NodeGraph 1.1.1

## Fixed

- **CurseForge 发布的 jar 现已正确 reobf**：1.1.0 的 CurseForge 件误传了未 reobf
  的 dev jar（`jar` 任务产物），游戏无法直接加载。本次修正发布流程，改用 `reobfJar`
  产物（运行时可加载 jar）。Maven Central 上的件是编译期库形态，不受影响。

---

Full changelog: https://github.com/TT432/nodegraph/releases
