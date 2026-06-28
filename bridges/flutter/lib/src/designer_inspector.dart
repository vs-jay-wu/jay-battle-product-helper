import 'dart:convert';
import 'dart:developer' as developer;

import 'package:flutter/foundation.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/widgets.dart';

import 'design_node.dart';

/// True while the Designer Shell has put the app in "design mode": a tap selects
/// the widget under the pointer (and reports it to the shell) instead of acting.
final ValueNotifier<bool> kDesignMode = ValueNotifier<bool>(false);

// Figma-style click-to-drill: repeated taps on the same hit stack step inward.
List<String> _lastStack = <String>[];
int _drillIndex = 0;

/// Registers the inspector service extensions the shell drives.
void registerDesignerInspector() {
  if (!kDebugMode) return;

  developer.registerExtension('ext.designer.setDesignMode', (String m, Map<String, String> params) async {
    kDesignMode.value = (params['on'] ?? 'true') == 'true';
    if (!kDesignMode.value) kDesignHighlight.value = null;
    return developer.ServiceExtensionResponse.result(jsonEncode(<String, dynamic>{'on': kDesignMode.value}));
  });

  developer.registerExtension('ext.designer.selectAt', (String m, Map<String, String> params) async {
    final double x = double.tryParse(params['x'] ?? '') ?? 0;
    final double y = double.tryParse(params['y'] ?? '') ?? 0;
    return developer.ServiceExtensionResponse.result(jsonEncode(selectAt(Offset(x, y))));
  });

  developer.registerExtension('ext.designer.viewSize', (String m, Map<String, String> params) async {
    final RenderView view = RendererBinding.instance.renderViews.first;
    final Size s = view.size;
    final double dpr = view.flutterView.devicePixelRatio;
    return developer.ServiceExtensionResponse.result(
      jsonEncode(<String, dynamic>{'width': s.width, 'height': s.height, 'dpr': dpr}),
    );
  });
}

/// Hit-test [globalPosition], set the inspector selection, post a
/// `designer:selection` event for the shell, and return the hit.
Map<String, dynamic> selectAndReport(Offset globalPosition, RenderBox content) {
  // Prefer an app-authored design node under the pointer (clean, named); fall
  // back to the raw render-tree hit-test when nothing is tagged there.
  final DesignNodeState? dn = hitTestDesignNode(globalPosition);
  if (dn != null) {
    final Element el = dn.context as Element;
    // ignore: invalid_use_of_protected_member
    WidgetInspectorService.instance.setSelection(el, 'designer-shell');
    final Rect? r = dn.bounds();
    kDesignHighlight.value = r;
    final Map<String, dynamic> res = <String, dynamic>{
      'found': true,
      'node': dn.nodeId,
      'name': dn.widget.name,
      'type': dn.widget.child.runtimeType.toString(),
      if (r != null)
        ...<String, dynamic>{'x': r.left, 'y': r.top, 'w': r.width, 'h': r.height},
    };
    developer.postEvent('designer:selection', res);
    return res;
  }
  final Map<String, dynamic> res = selectAt(globalPosition, content: content);
  if (res['found'] == true) {
    kDesignHighlight.value = _rectFromRes(res);
    developer.postEvent('designer:selection', res);
  }
  return res;
}

/// Build a [Rect] from a selection result's x/y/w/h (null if absent).
Rect? _rectFromRes(Map<String, dynamic> res) {
  final Object? x = res['x'], y = res['y'], w = res['w'], h = res['h'];
  if (x is num && y is num && w is num && h is num) {
    return Rect.fromLTWH(x.toDouble(), y.toDouble(), w.toDouble(), h.toDouble());
  }
  return null;
}

/// Hit-test [globalPosition] and select the widget under it. When [content] is
/// given, hit-test that subtree only (so the design overlay above it is ignored).
/// Repeated taps at the same spot drill inward through the widget stack.
Map<String, dynamic> selectAt(Offset globalPosition, {RenderBox? content}) {
  final HitTestResult result;
  if (content != null && content.hasSize) {
    final BoxHitTestResult boxResult = BoxHitTestResult();
    content.hitTest(boxResult, position: content.globalToLocal(globalPosition));
    result = boxResult;
  } else {
    final Iterable<RenderView> views = RendererBinding.instance.renderViews;
    if (views.isEmpty) return <String, dynamic>{'found': false, 'reason': 'no-view'};
    final HitTestResult r = HitTestResult();
    views.first.hitTest(r, position: globalPosition);
    result = r;
  }

  final List<MapEntry<Element, Rect>> stack = <MapEntry<Element, Rect>>[];
  final Set<String> seen = <String>{};
  for (final HitTestEntry entry in result.path) {
    final Object target = entry.target;
    if (target is RenderBox && target.attached && target.hasSize) {
      final Object? creator = target.debugCreator;
      if (creator is DebugCreator) {
        final Offset tl = target.localToGlobal(Offset.zero);
        final Rect r = tl & target.size;
        final String key = '${r.left.round()},${r.top.round()},${r.width.round()},${r.height.round()}';
        if (seen.add(key)) stack.add(MapEntry<Element, Rect>(creator.element, r));
      }
    }
  }
  if (stack.isEmpty) {
    _lastStack = <String>[];
    _drillIndex = 0;
    return <String, dynamic>{'found': false, 'reason': 'no-element'};
  }
  stack.sort((MapEntry<Element, Rect> a, MapEntry<Element, Rect> b) =>
      (b.value.width * b.value.height).compareTo(a.value.width * a.value.height));
  final List<String> keys = stack
      .map((MapEntry<Element, Rect> e) =>
          '${e.value.left.round()},${e.value.top.round()},${e.value.width.round()},${e.value.height.round()}')
      .toList();
  _drillIndex = listEquals(keys, _lastStack) ? (_drillIndex + 1) % stack.length : 0;
  _lastStack = keys;
  final MapEntry<Element, Rect> sel = stack[_drillIndex];

  // ignore: invalid_use_of_protected_member
  WidgetInspectorService.instance.setSelection(sel.key, 'designer-shell');
  final Rect r = sel.value;
  return <String, dynamic>{
    'found': true,
    'type': sel.key.widget.runtimeType.toString(),
    'x': r.left, 'y': r.top, 'w': r.width, 'h': r.height,
  };
}
