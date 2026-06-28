import 'dart:convert';
import 'dart:developer' as developer;

import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';

/// Live registry of mounted [DesignNode]s. Debug-only; the Designer Shell builds
/// its clean, app-authored structure tree from this instead of the raw widget
/// inspector tree.
final Set<DesignNodeState> kDesignNodes = <DesignNodeState>{};

/// Global-coordinate bounds of the currently selected node, drawn as the design
/// highlight box. Driven both by in-app taps and by shell-initiated selection.
final ValueNotifier<Rect?> kDesignHighlight = ValueNotifier<Rect?>(null);

int _designSeq = 0;

/// Carries the enclosing [DesignNode]'s id down the tree so a nested node finds
/// its parent automatically — the hierarchy falls out of widget nesting.
class _DesignParent extends InheritedWidget {
  const _DesignParent({required this.id, required super.child});

  final String id;

  static String? of(BuildContext context) =>
      context.dependOnInheritedWidgetOfExactType<_DesignParent>()?.id;

  @override
  bool updateShouldNotify(_DesignParent oldWidget) => oldWidget.id != id;
}

/// Wrap a widget you want the Designer Shell to treat as one named node in the
/// structure tree. Nesting [DesignNode]s builds the hierarchy. Only what you
/// wrap appears — keep it to meaningful components (screens, sections, reusable
/// widgets, key controls). Debug-only; in release it just returns [child].
class DesignNode extends StatefulWidget {
  const DesignNode({super.key, required this.name, this.id, required this.child});

  /// Human label shown in the shell's structure tree.
  final String name;

  /// Optional stable id (auto-generated when omitted). Set it when the identity
  /// must survive reordering.
  final String? id;

  final Widget child;

  @override
  State<DesignNode> createState() => DesignNodeState();
}

class DesignNodeState extends State<DesignNode> {
  late final String nodeId = widget.id ?? 'dn${_designSeq++}';
  String? parentId;

  @override
  void initState() {
    super.initState();
    if (kDebugMode) kDesignNodes.add(this);
  }

  @override
  void dispose() {
    kDesignNodes.remove(this);
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (!kDebugMode) return widget.child;
    parentId = _DesignParent.of(context);
    return _DesignParent(id: nodeId, child: widget.child);
  }

  /// Global-coordinate bounds of this node (its child's render box), or null.
  Rect? bounds() {
    final RenderObject? ro = mounted ? context.findRenderObject() : null;
    if (ro is RenderBox && ro.attached && ro.hasSize) {
      return ro.localToGlobal(Offset.zero) & ro.size;
    }
    return null;
  }
}

/// Smallest mounted design node whose bounds contain [global] (the deepest hit).
DesignNodeState? hitTestDesignNode(Offset global) {
  DesignNodeState? best;
  double bestArea = double.infinity;
  for (final DesignNodeState s in kDesignNodes) {
    final Rect? r = s.bounds();
    if (r != null && r.contains(global)) {
      final double area = r.width * r.height;
      if (area < bestArea) {
        bestArea = area;
        best = s;
      }
    }
  }
  return best;
}

/// Registers `ext.designer.getDesignTree`, which returns the app-authored node
/// tree as `{ "roots": [ {id, name, type, x, y, w, h, children: [...] } ] }`.
void registerDesignNodeTree() {
  if (!kDebugMode) return;
  developer.registerExtension('ext.designer.getDesignTree',
      (String m, Map<String, String> params) async {
    return developer.ServiceExtensionResponse.result(jsonEncode(_buildDesignTree()));
  });

  // Select a design node by id (used when the shell picks a row in the clean
  // tree): highlight it in the inspector and report it back to the shell.
  developer.registerExtension('ext.designer.selectNode',
      (String m, Map<String, String> params) async {
    final String? id = params['id'];
    DesignNodeState? found;
    for (final DesignNodeState s in kDesignNodes) {
      if (s.nodeId == id && s.mounted) {
        found = s;
        break;
      }
    }
    if (found == null) {
      return developer.ServiceExtensionResponse.result(jsonEncode(<String, dynamic>{'found': false}));
    }
    final Element el = found.context as Element;
    // ignore: invalid_use_of_protected_member
    WidgetInspectorService.instance.setSelection(el, 'designer-shell');
    final Rect? r = found.bounds();
    kDesignHighlight.value = r; // draw the highlight box in the app
    final Map<String, dynamic> res = <String, dynamic>{
      'found': true,
      'node': found.nodeId,
      'name': found.widget.name,
      'type': found.widget.child.runtimeType.toString(),
      if (r != null)
        ...<String, dynamic>{'x': r.left, 'y': r.top, 'w': r.width, 'h': r.height},
    };
    developer.postEvent('designer:selection', res);
    return developer.ServiceExtensionResponse.result(jsonEncode(res));
  });
}

Map<String, dynamic> _buildDesignTree() {
  final List<DesignNodeState> states =
      kDesignNodes.where((DesignNodeState s) => s.mounted).toList();
  final Set<String> ids = <String>{for (final DesignNodeState s in states) s.nodeId};
  // Re-parent orphans (parent disposed but child not yet) to the root.
  final Map<String?, List<DesignNodeState>> childrenOf =
      <String?, List<DesignNodeState>>{};
  for (final DesignNodeState s in states) {
    final String? parent =
        (s.parentId != null && ids.contains(s.parentId)) ? s.parentId : null;
    childrenOf.putIfAbsent(parent, () => <DesignNodeState>[]).add(s);
  }

  void sortByBounds(List<DesignNodeState> list) {
    list.sort((DesignNodeState a, DesignNodeState b) {
      final Rect? ra = a.bounds();
      final Rect? rb = b.bounds();
      if (ra == null || rb == null) return 0;
      final int dy = ra.top.compareTo(rb.top);
      return dy != 0 ? dy : ra.left.compareTo(rb.left);
    });
  }

  Map<String, dynamic> toJson(DesignNodeState s) {
    final Rect? r = s.bounds();
    final List<DesignNodeState> kids = childrenOf[s.nodeId] ?? <DesignNodeState>[];
    sortByBounds(kids);
    return <String, dynamic>{
      'id': s.nodeId,
      'name': s.widget.name,
      'type': s.widget.child.runtimeType.toString(),
      if (r != null)
        ...<String, dynamic>{'x': r.left, 'y': r.top, 'w': r.width, 'h': r.height},
      'children': kids.map(toJson).toList(),
    };
  }

  final List<DesignNodeState> roots = childrenOf[null] ?? <DesignNodeState>[];
  sortByBounds(roots);
  return <String, dynamic>{'roots': roots.map(toJson).toList()};
}
