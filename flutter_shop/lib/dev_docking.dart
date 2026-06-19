import 'dart:convert';
import 'dart:developer' as developer;
import 'dart:io' show Platform;
import 'dart:ui' show Rect;

import 'package:flutter/foundation.dart';
import 'package:window_manager/window_manager.dart';

bool get _enabled => kDebugMode && !kIsWeb && (Platform.isMacOS || Platform.isWindows || Platform.isLinux);

/// Must run in main() before runApp (desktop debug only).
Future<void> initWindowManager() async {
  if (!_enabled) return;
  await windowManager.ensureInitialized();
}

/// Debug-only window-docking bridge for the Designer Shell. The shell drives
/// these to embed this app's native window into its canvas region.
void registerDevDocking() {
  if (!_enabled) return;

  // Make the window frameless + always-on-top so it can sit flush inside the
  // shell's canvas region (on=false restores a normal window).
  developer.registerExtension('ext.shopdemo.dockMode', (String m, Map<String, String> params) async {
    final bool on = (params['on'] ?? 'true') == 'true';
    if (on) {
      await windowManager.setAsFrameless();
      await windowManager.setHasShadow(false);
      await windowManager.setAlwaysOnTop(true);
      await windowManager.setSkipTaskbar(true);
    } else {
      await windowManager.setAlwaysOnTop(false);
      await windowManager.setSkipTaskbar(false);
      await windowManager.setHasShadow(true);
    }
    return developer.ServiceExtensionResponse.result(jsonEncode(<String, dynamic>{'ok': true}));
  });

  // Position the window at a screen rect (logical pixels, top-left origin).
  developer.registerExtension('ext.shopdemo.setBounds', (String m, Map<String, String> params) async {
    final double x = double.tryParse(params['x'] ?? '') ?? 0;
    final double y = double.tryParse(params['y'] ?? '') ?? 0;
    final double w = double.tryParse(params['w'] ?? '') ?? 400;
    final double h = double.tryParse(params['h'] ?? '') ?? 300;
    await windowManager.setBounds(Rect.fromLTWH(x, y, w, h));
    return developer.ServiceExtensionResponse.result(jsonEncode(<String, dynamic>{'ok': true}));
  });

  // Read back the current bounds (for the shell to verify docking).
  developer.registerExtension('ext.shopdemo.getBounds', (String m, Map<String, String> params) async {
    final Rect b = await windowManager.getBounds();
    return developer.ServiceExtensionResponse.result(
      jsonEncode(<String, dynamic>{'x': b.left, 'y': b.top, 'w': b.width, 'h': b.height}),
    );
  });
}
