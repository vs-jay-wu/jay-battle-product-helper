/// Designer Shell bridge for Flutter apps.
///
/// Any Flutter app can connect to the Designer Shell by:
///  1. `await DesignerBridge.init();` then `DesignerBridge.register();` in main()
///     (before / around `runApp`), and
///  2. wrapping its content with [DesignModeScope] (e.g. via `MaterialApp.builder`).
///
/// All of it is debug-only and a no-op in release builds.
library;

import 'package:flutter/foundation.dart';

import 'src/design_node.dart';
import 'src/designer_docking.dart';
import 'src/designer_inspector.dart';

export 'src/design_mode_scope.dart' show DesignModeScope;
export 'src/design_node.dart' show DesignNode;
export 'src/designer_inspector.dart' show kDesignMode;

/// Entry points for wiring a Flutter app to the Designer Shell.
abstract final class DesignerBridge {
  /// Initialise the desktop window manager (call in main() before runApp).
  static Future<void> init() async {
    if (!kDebugMode) return;
    await initDesignerWindow();
  }

  /// Register the inspector + docking service extensions the shell drives.
  static void register() {
    if (!kDebugMode) return;
    registerDesignerInspector();
    registerDesignerDocking();
    registerDesignNodeTree();
  }
}
