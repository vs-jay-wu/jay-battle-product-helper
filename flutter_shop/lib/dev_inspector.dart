import 'dart:convert';
import 'dart:developer' as developer;

import 'package:flutter/foundation.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/widgets.dart';

/// True while the Designer Shell has put the app in "design mode": the design
/// overlay absorbs taps and selects the widget under the pointer instead of
/// letting the app act.
final ValueNotifier<bool> kDesignMode = ValueNotifier<bool>(false);

// Figma-style click-to-drill: repeated taps on the same hit stack step inward.
List<String> _lastStack = <String>[];
int _drillIndex = 0;

/// Debug-only bridge for the Designer Shell.
void registerDevInspector() {
  if (!kDebugMode) return;

  developer.registerExtension('ext.shopdemo.setDesignMode', (String m, Map<String, String> params) async {
    kDesignMode.value = (params['on'] ?? 'true') == 'true';
    return developer.ServiceExtensionResponse.result(jsonEncode(<String, dynamic>{'on': kDesignMode.value}));
  });

  developer.registerExtension('ext.shopdemo.selectAt', (String m, Map<String, String> params) async {
    final double x = double.tryParse(params['x'] ?? '') ?? 0;
    final double y = double.tryParse(params['y'] ?? '') ?? 0;
    return developer.ServiceExtensionResponse.result(jsonEncode(selectAt(Offset(x, y))));
  });

  developer.registerExtension('ext.shopdemo.viewSize', (String m, Map<String, String> params) async {
    final RenderView view = RendererBinding.instance.renderViews.first;
    final Size s = view.size;
    final double dpr = view.flutterView.devicePixelRatio;
    return developer.ServiceExtensionResponse.result(
      jsonEncode(<String, dynamic>{'width': s.width, 'height': s.height, 'dpr': dpr}),
    );
  });
}

/// Hit-test the design overlay's tap, set the inspector selection, post a
/// `shopdemo:selection` event for the shell, and return the hit (incl. bounds
/// for the in-app highlight).
Map<String, dynamic> selectAndReport(Offset globalPosition, RenderBox content) {
  final Map<String, dynamic> res = selectAt(globalPosition, content: content);
  if (res['found'] == true) developer.postEvent('shopdemo:selection', res);
  return res;
}

/// Hit-test [globalPosition], set the inspector selection to the hit element,
/// and return its type + global bounds. When [content] is given, hit-test that
/// subtree only (so the design overlay above it is ignored).
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

  // Build the stack of laid-out widgets under the point, dedup by bounds.
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
  // Outermost (largest) first; repeated taps on the same stack step inward.
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
  WidgetInspectorService.instance.setSelection(sel.key, 'shell-inspector');
  final Rect r = sel.value;
  return <String, dynamic>{
    'found': true,
    'type': sel.key.widget.runtimeType.toString(),
    'x': r.left, 'y': r.top, 'w': r.width, 'h': r.height,
  };
}
