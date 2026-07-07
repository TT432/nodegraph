# NodeGraph 1.1.0

## Added

- **Connection lifecycle events & notification mechanism**: `NodeGraph` now fires
  fine-grained `ConnectionEvent`s (`CREATED` / `REMOVED`) with an observable
  `ConnectionListener` API. Covers all mutation paths including the replace
  semantics of `connect` (REMOVED old → CREATED new) and the cascade removal in
  `removeNode`.
  - `api/model/ConnectionEvent.java`
  - `api/model/ConnectionListener.java`
  - `NodeGraph#addConnectionListener / removeConnectionListener`

## Changed

- Bump version to 1.1.0.

---

Full changelog: https://github.com/TT432/nodegraph/releases
