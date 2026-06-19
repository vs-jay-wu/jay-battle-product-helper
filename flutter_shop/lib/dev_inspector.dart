import 'dart:convert';
import 'dart:developer' as developer;

import 'package:flutter/foundation.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/widgets.dart';

/// True while the Designer Shell has put the app in "design mode": the design
/// overlay absorbs taps and selects the widget under the pointer instead of
/// letting the app act.
final ValueNotifier<bool> kDesignMode = ValueNotifier<bool>(false);

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

  // Same call the in-app inspector tap uses; lets getSelectedSummaryWidget resolve
  // the nearest user widget + creationLocation.
  // ignore: invalid_use_of_protected_member
  WidgetInspectorService.instance.setSelection(hitElement, 'shell-inspector');

  final RenderObject? ro = hitElement.renderObject;
  Map<String, dynamic> bounds = <String, dynamic>{};
  if (ro is RenderBox && ro.hasSize) {
    final Offset tl = ro.localToGlobal(Offset.zero);
    bounds = <String, dynamic>{'x': tl.dx, 'y': tl.dy, 'w': ro.size.width, 'h': ro.size.height};
  }

  return <String, dynamic>{'found': true, 'type': hitElement.widget.runtimeType.toString(), ...bounds};
}
