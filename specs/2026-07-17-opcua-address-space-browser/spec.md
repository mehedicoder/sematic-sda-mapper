# Feature Spec: OPC UA Address Space Browser

## Overview

Replace the flat browsed-nodes grid with a proper OPC UA address space tree browser that mirrors the standard OPC UA hierarchy (OPC-10000-5 §8.2). Users can navigate from Root → Objects → Tank1 → Level, etc., expand/collapse folders, and view node details in a side panel.

## Reference

- OPC UA Part 5 §8.2: Standard AddressSpace structure (Root → Objects/Types/Views)
- OPC UA Part 5 §11: HierarchicalReferences (Organizes, HasComponent, HasProperty)

## Acceptance Criteria

- [ ] Server card click opens a tree browser view (not a flat grid)
- [ ] Tree starts at ObjectsFolder and shows immediate children (Tank1, Server, etc.)
- [ ] Expanding a node lazily loads its children via OPC UA Browse
- [ ] Each tree node shows: icon (folder/variable/method), browse name, node class
- [ ] Clicking a variable node shows detail panel: NodeId, BrowseName, DisplayName, Value, DataType, NodeClass
- [ ] Clicking an object/folder node shows detail panel without value
- [ ] "Back to Servers" button returns to server card list
- [ ] Lazy loading: children loaded on expand, not upfront
- [ ] Handles servers with no custom nodes gracefully (shows Server object only)
- [ ] Unit tests for BrowseTreeNode model and tree browse service methods

## Technical Design

### Data Model

```java
public record BrowseTreeNode(
    String nodeId,           // "ns=2;Tank1"
    String browseName,       // "Tank1"
    String displayName,      // "Tank1"
    String nodeClass,        // "Object", "Variable", "Method"
    Object value,            // null for non-variables
    String dataType,         // "Double", "", etc.
    boolean hasChildren      // true for objects/folders
)
```

### Service Layer

- `browseAddressSpace(endpointUrl)` → `List<BrowseTreeNode>` (root children from ObjectsFolder)
- `browseChildren(endpointUrl, parentNodeId)` → `List<BrowseTreeNode>` (lazy load)

### View Layer

- Left: `TreeGrid<BrowseTreeNode>` with hierarchical data provider
- Right: Detail panel with node properties
- Top: Server name + endpoint, Back button

### OPC UA Browse per spec

- Browse `ObjectsFolder` (ns=0;id=85) with `HierarchicalReferences` forward
- For each child: check `NodeClass` from `ReferenceDescription`
- If `NodeClass` is Object or Folder → `hasChildren=true`, expandable
- If `NodeClass` is Variable → `hasChildren=false`, show value on click
- If `NodeClass` is Method → `hasChildren=false`, show method info
