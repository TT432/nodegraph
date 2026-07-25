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
