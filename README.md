# NodeGraph

![Maven Central](https://img.shields.io/maven-central/v/io.github.tt432/nodegraph)
![License](https://img.shields.io/badge/license-MIT-blue)

A reusable node graph editor library and evaluation engine for Minecraft (Forge 1.20.1 / NeoForge 1.21.1 / NeoForge 26.1.2).

## Features

- **Node editor**: nodes with a header, typed input/output ports, and input widgets (text / slider / button-group / dropdown)
- **Single- and multi-input nodes**: `MultiInputNodeDefinition` ports accept multiple wires (order = connection order); plain `NodeDefinition` ports keep single-source replacement semantics
- **Type system**: per-type colors, registry, automatic conversion rules (rendered with a warning)
- **Evaluation engine**: lazy Blender-style evaluation with cycle detection and per-run caching
- **Canvas**: pan / zoom / scroll, grid, embeddable `NodeGraphWidget`
- **Node groups**: wireframe boxes, drag-to-regroup, resize handle, per-group scale
- **Editing**: box-select, right-click context menu, copy / cut / paste, full undo/redo, keyboard shortcuts (Ctrl+C/X/V, Del, Ctrl+Z / Ctrl+Shift+Z)
- **Add node**: searchable overlay; dragging a port to empty space opens a type-filtered menu with auto-connect
- **Live results**: output ports display their evaluated value each frame; widget edits recompute downstream instantly

## Installation

Gradle（消费方按自己的平台选后缀；1.20.1 Forge mod 用 `fg.deobf`，NeoForge 直接 implementation）：

```groovy
repositories {
    mavenCentral()
}

dependencies {
    // Forge 1.20.1
    implementation fg.deobf('io.github.tt432:nodegraph:1.2.0+1.20.1-forge')
    // NeoForge 1.21.1
    implementation 'io.github.tt432:nodegraph:1.2.0+1.21.1-neoforge'
    // NeoForge 26.1.2
    implementation 'io.github.tt432:nodegraph:1.2.0+26.1.2-neoforge'
}
```

### 版本化语义（Versioning）

坐标格式：`mod_version+{minecraft_version}-{loader}`。

- `mod_version`（如 `1.2.0`）遵循 [semver](https://semver.org/)：标识 API/特性层级，跨所有平台变体共享。
  同一 mod_version 的所有变体功能等价，可以互换着写业务代码。
- `+` 之后是 **semver build metadata**：标识构建目标平台（MC 版本 + loader），按 semver 规范
  **不参与版本优先级比较**。它只回答“这个 jar 为哪个平台构建”，不表达新旧。
- 不同后缀的 jar **不可互换**：类名/包名虽一致，但面向不同 MC/loader 编译（如 26.1 的 `Identifier`、
  `GuiGraphicsExtractor`），混用会在运行时 NoClassDefFoundError。
- 升级路径：跟业务语义走 mod_version（1.2.0 → 1.3.0 = 新特性），平台后缀跟随你所用的 MC/loader 固定不变。

## In-game usage

1. Obtain the **Node Graph Editor** item from the Tools creative tab.
2. Right-click to open the editor.
3. Drag from an output port to an input port to connect them — same type only, or auto-convert (shown with an orange warning).
4. Right-click the canvas → **Add Node...** to insert from the catalog; drag a port onto empty space for a type-filtered menu that auto-connects the new node.
5. Click a text input widget to edit its value; downstream results recompute live.
6. Middle-drag to pan, Ctrl+scroll to zoom, left-drag empty space to box-select.

## License

[MIT](LICENSE)
