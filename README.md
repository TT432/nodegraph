# NodeGraph

![Maven Central](https://img.shields.io/maven-central/v/io.github.tt432/nodegraph)
![License](https://img.shields.io/badge/license-MIT-blue)

A reusable node graph editor library and evaluation engine for Minecraft Forge 1.20.1.

## Features

- **Node editor**: nodes with a header, typed input/output ports, and input widgets (text / slider / button-group / dropdown)
- **Type system**: per-type colors, registry, automatic conversion rules (rendered with a warning)
- **Evaluation engine**: lazy Blender-style evaluation with cycle detection and per-run caching
- **Canvas**: pan / zoom / scroll, grid, embeddable `NodeGraphWidget`
- **Node groups**: wireframe boxes, drag-to-regroup, resize handle, per-group scale
- **Editing**: box-select, right-click context menu, copy / cut / paste, full undo/redo, keyboard shortcuts (Ctrl+C/X/V, Del, Ctrl+Z / Ctrl+Shift+Z)
- **Add node**: searchable overlay; dragging a port to empty space opens a type-filtered menu with auto-connect
- **Live results**: output ports display their evaluated value each frame; widget edits recompute downstream instantly

## Installation

Gradle (Forge 1.20.1 mod consumers use `fg.deobf`):

```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation fg.deobf('io.github.tt432:nodegraph:1.0.0')
}
```

## In-game usage

1. Obtain the **Node Graph Editor** item from the Tools creative tab.
2. Right-click to open the editor.
3. Drag from an output port to an input port to connect them — same type only, or auto-convert (shown with an orange warning).
4. Right-click the canvas → **Add Node...** to insert from the catalog; drag a port onto empty space for a type-filtered menu that auto-connects the new node.
5. Click a text input widget to edit its value; downstream results recompute live.
6. Middle-drag to pan, Ctrl+scroll to zoom, left-drag empty space to box-select.

## License

[MIT](LICENSE)
