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
