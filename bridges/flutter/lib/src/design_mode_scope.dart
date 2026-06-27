import 'package:flutter/material.dart';

import 'designer_inspector.dart';

/// Wrap your app's content with this (typically via `MaterialApp.builder`).
/// In design mode it overlays the content, absorbs taps, hit-tests the content
/// below, reports the selection to the Designer Shell, and highlights it.
/// Out of design mode it is fully transparent and non-interactive.
class DesignModeScope extends StatefulWidget {
  const DesignModeScope({super.key, required this.child});

  final Widget child;

  @override
  State<DesignModeScope> createState() => _DesignModeScopeState();
}

class _DesignModeScopeState extends State<DesignModeScope> {
  final GlobalKey _contentKey = GlobalKey();
  Rect? _highlight;

  void _onTapDown(PointerDownEvent e) {
    final RenderObject? ro = _contentKey.currentContext?.findRenderObject();
    if (ro is! RenderBox) return;
    final Map<String, dynamic> res = selectAndReport(e.position, ro);
    if (res['found'] != true) return;
    final num? x = res['x'] as num?, y = res['y'] as num?, w = res['w'] as num?, h = res['h'] as num?;
    setState(() {
      _highlight = (x != null && y != null && w != null && h != null)
          ? Rect.fromLTWH(x.toDouble(), y.toDouble(), w.toDouble(), h.toDouble())
          : null;
    });
  }

  @override
  Widget build(BuildContext context) {
    return ValueListenableBuilder<bool>(
      valueListenable: kDesignMode,
      builder: (BuildContext context, bool on, Widget? _) {
        if (!on) return KeyedSubtree(key: _contentKey, child: widget.child);
        return Stack(
          children: <Widget>[
            KeyedSubtree(key: _contentKey, child: widget.child),
            Positioned.fill(
              child: Listener(
                behavior: HitTestBehavior.opaque,
                onPointerDown: _onTapDown,
                child: const SizedBox.expand(),
              ),
            ),
            if (_highlight != null)
              Positioned(
                left: _highlight!.left,
                top: _highlight!.top,
                width: _highlight!.width,
                height: _highlight!.height,
                child: IgnorePointer(
                  child: Container(
                    decoration: BoxDecoration(
                      border: Border.all(color: const Color(0xFF4848F0), width: 2),
                      color: const Color(0x224848F0),
                    ),
                  ),
                ),
              ),
            Positioned.fill(
              child: IgnorePointer(
                child: Container(
                  decoration: BoxDecoration(border: Border.all(color: const Color(0xFF4848F0), width: 3)),
                  alignment: Alignment.topCenter,
                  child: Container(
                    margin: const EdgeInsets.only(top: 8),
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(color: const Color(0xFF4848F0), borderRadius: BorderRadius.circular(20)),
                    child: const Text('設計模式 · 點元件以選取',
                        style: TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.w600)),
                  ),
                ),
              ),
            ),
          ],
        );
      },
    );
  }
}
