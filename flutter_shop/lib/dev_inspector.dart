import 'dart:convert';
import 'dart:developer' as developer;

import 'package:flutter/foundation.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/widgets.dart';

/// Debug-only bridge for the Designer Shell. Registers a service extension that
/// hit-tests a point in the live render tree, sets the Flutter inspector
/// selection to the hit widget, and returns its global bounds + type. The shell
/// owns the click (over the docked window) and calls this with the local point;
/// it then reads the full node (incl. creationLocation) via the standard
/// `ext.flutter.inspector.getSelectedSummaryWidget`.
void registerDevInspector() {
  if (!kDebugMode) return;

  developer.registerExtension('ext.shopdemo.selectAt', (String method, Map<String, String> params) async {
    final double x = double.tryParse(params['x'] ?? '') ?? 0;
    final double y = double.tryParse(params['y'] ?? '') ?? 0;
    final Map<String, dynamic> out = _selectAt(Offset(x, y));
    return developer.ServiceExtensionResponse.result(jsonEncode(out));
  });

  // Returns the logical size of the (first) view so the shell can map shell-pixel
  // clicks into app-logical coordinates.
  developer.registerExtension('ext.shopdemo.viewSize', (String method, Map<String, String> params) async {
    final RenderView view = RendererBinding.instance.renderViews.first;
    final Size s = view.size;
    final double dpr = view.flutterView.devicePixelRatio;
    return developer.ServiceExtensionResponse.result(
      jsonEncode(<String, dynamic>{'width': s.width, 'height': s.height, 'dpr': dpr}),
    );
  });
}

Map<String, dynamic> _selectAt(Offset position) {
  final Iterable<RenderView> views = RendererBinding.instance.renderViews;
  if (views.isEmpty) return <String, dynamic>{'found': false, 'reason': 'no-view'};
  final RenderView view = views.first;

  final HitTestResult result = HitTestResult();
  view.hitTest(result, position: position);

  // Walk front-to-back; pick the first hit RenderObject that maps to an Element.
  Element? hitElement;
  for (final HitTestEntry entry in result.path) {
    final Object target = entry.target;
    if (target is RenderObject) {
      final Object? creator = target.debugCreator;
      if (creator is DebugCreator) {
        hitElement = creator.element;
        break;
      }
    }
  }
  if (hitElement == null) return <String, dynamic>{'found': false, 'reason': 'no-element'};

  // Drive the standard Flutter inspector selection (same call the in-app tap uses)
  // so getSelectedSummaryWidget resolves the nearest user widget + creationLocation.
  final bool ok = WidgetInspectorService.instance.setSelection(hitElement, 'shell-inspector');

  final RenderObject? ro = hitElement.renderObject;
  Map<String, dynamic> bounds = <String, dynamic>{};
  if (ro is RenderBox && ro.hasSize) {
    final Offset tl = ro.localToGlobal(Offset.zero);
    bounds = <String, dynamic>{'x': tl.dx, 'y': tl.dy, 'w': ro.size.width, 'h': ro.size.height};
  }

  return <String, dynamic>{
    'found': true,
    'selected': ok,
    'type': hitElement.widget.runtimeType.toString(),
    ...bounds,
  };
}
