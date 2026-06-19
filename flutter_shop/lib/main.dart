import 'package:flutter/material.dart';

import 'dev_docking.dart';
import 'dev_inspector.dart';
import 'products.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await initWindowManager(); // debug desktop only
  registerDevInspector(); // debug-only Designer Shell inspector bridge (no-op in release)
  registerDevDocking(); // debug-only Designer Shell window-docking bridge
  runApp(const ShopDemoApp());
}

class ShopDemoApp extends StatelessWidget {
  const ShopDemoApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'ShopDemo',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF4848F0)),
        useMaterial3: true,
      ),
      home: const StorefrontPage(),
      builder: (BuildContext context, Widget? child) => _DesignModeChrome(child: child!),
    );
  }
}

/// In design mode, an overlay absorbs taps, hit-tests the *content* below it
/// (selecting the widget + reporting to the Designer Shell), and highlights the
/// selection. Out of design mode it is fully transparent and non-interactive.
class _DesignModeChrome extends StatefulWidget {
  const _DesignModeChrome({required this.child});

  final Widget child;

  @override
  State<_DesignModeChrome> createState() => _DesignModeChromeState();
}

class _DesignModeChromeState extends State<_DesignModeChrome> {
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
            // Absorb taps and route them to the content hit-test.
            Positioned.fill(
              child: Listener(
                behavior: HitTestBehavior.opaque,
                onPointerDown: _onTapDown,
                child: const SizedBox.expand(),
              ),
            ),
            // Selection highlight.
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
            // Design-mode frame + label.
            Positioned.fill(
              child: IgnorePointer(
                child: Container(
                  decoration: BoxDecoration(border: Border.all(color: const Color(0xFF4848F0), width: 3)),
                  alignment: Alignment.topCenter,
                  child: Container(
                    margin: const EdgeInsets.only(top: 8),
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: const Color(0xFF4848F0),
                      borderRadius: BorderRadius.circular(20),
                    ),
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

class StorefrontPage extends StatefulWidget {
  const StorefrontPage({super.key});

  @override
  State<StorefrontPage> createState() => _StorefrontPageState();
}

class _StorefrontPageState extends State<StorefrontPage> {
  String _category = 'All';
  final Map<String, int> _cart = <String, int>{};

  int get _cartCount => _cart.values.fold(0, (int sum, int n) => sum + n);

  List<Product> get _visible => _category == 'All'
      ? kProducts
      : kProducts.where((Product p) => p.category == _category).toList();

  void _addToCart(Product p) {
    setState(() => _cart[p.id] = (_cart[p.id] ?? 0) + 1);
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(
        SnackBar(
          content: Text('Added "${p.name}" to cart'),
          duration: const Duration(milliseconds: 900),
          behavior: SnackBarBehavior.floating,
        ),
      );
  }

  @override
  Widget build(BuildContext context) {
    final ColorScheme scheme = Theme.of(context).colorScheme;
    return Scaffold(
      backgroundColor: scheme.surfaceContainerLowest,
      appBar: AppBar(
        backgroundColor: scheme.surface,
        title: Row(
          children: <Widget>[
            Icon(Icons.shopping_bag_rounded, color: scheme.primary),
            const SizedBox(width: 8),
            const Text('ShopDemo', style: TextStyle(fontWeight: FontWeight.w700)),
          ],
        ),
        actions: <Widget>[
          _CartButton(count: _cartCount),
          const SizedBox(width: 8),
        ],
      ),
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          _CategoryBar(
            categories: kCategories,
            selected: _category,
            onSelected: (String c) => setState(() => _category = c),
          ),
          Expanded(
            child: GridView.builder(
              padding: const EdgeInsets.fromLTRB(16, 4, 16, 24),
              gridDelegate: const SliverGridDelegateWithMaxCrossAxisExtent(
                maxCrossAxisExtent: 280,
                mainAxisExtent: 296,
                crossAxisSpacing: 16,
                mainAxisSpacing: 16,
              ),
              itemCount: _visible.length,
              itemBuilder: (BuildContext context, int index) {
                final Product p = _visible[index];
                return ProductCard(product: p, onAdd: () => _addToCart(p));
              },
            ),
          ),
        ],
      ),
    );
  }
}

class _CartButton extends StatelessWidget {
  const _CartButton({required this.count});

  final int count;

  @override
  Widget build(BuildContext context) {
    final ColorScheme scheme = Theme.of(context).colorScheme;
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 4),
      child: Stack(
        clipBehavior: Clip.none,
        children: <Widget>[
          IconButton.filledTonal(
            onPressed: () {},
            icon: const Icon(Icons.shopping_cart_outlined),
          ),
          if (count > 0)
            Positioned(
              right: -2,
              top: -2,
              child: Container(
                padding: const EdgeInsets.all(5),
                decoration: BoxDecoration(color: scheme.error, shape: BoxShape.circle),
                constraints: const BoxConstraints(minWidth: 20, minHeight: 20),
                child: Text(
                  '$count',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: scheme.onError, fontSize: 11, fontWeight: FontWeight.bold),
                ),
              ),
            ),
        ],
      ),
    );
  }
}

class _CategoryBar extends StatelessWidget {
  const _CategoryBar({
    required this.categories,
    required this.selected,
    required this.onSelected,
  });

  final List<String> categories;
  final String selected;
  final ValueChanged<String> onSelected;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 56,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        itemCount: categories.length,
        separatorBuilder: (_, _) => const SizedBox(width: 8),
        itemBuilder: (BuildContext context, int index) {
          final String c = categories[index];
          return ChoiceChip(
            label: Text(c),
            selected: c == selected,
            onSelected: (_) => onSelected(c),
          );
        },
      ),
    );
  }
}

class ProductCard extends StatelessWidget {
  const ProductCard({super.key, required this.product, required this.onAdd});

  final Product product;
  final VoidCallback onAdd;

  @override
  Widget build(BuildContext context) {
    final ColorScheme scheme = Theme.of(context).colorScheme;
    return Container(
      decoration: BoxDecoration(
        color: scheme.surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: scheme.outlineVariant),
      ),
      clipBehavior: Clip.antiAlias,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Expanded(
            child: Stack(
              fit: StackFit.expand,
              children: <Widget>[
                Image.network(
                  product.imageUrl,
                  fit: BoxFit.cover,
                  loadingBuilder: (BuildContext context, Widget child, ImageChunkEvent? p) {
                    if (p == null) return child;
                    return Container(
                      color: scheme.surfaceContainerHighest,
                      child: const Center(child: CircularProgressIndicator(strokeWidth: 2)),
                    );
                  },
                  errorBuilder: (_, _, _) => Container(
                    color: scheme.surfaceContainerHighest,
                    child: Icon(Icons.image_outlined, color: scheme.outline, size: 40),
                  ),
                ),
                Positioned(
                  left: 8,
                  top: 8,
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                    decoration: BoxDecoration(
                      color: scheme.surface.withValues(alpha: 0.9),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: <Widget>[
                        const Icon(Icons.star_rounded, size: 14, color: Color(0xFFF5A623)),
                        const SizedBox(width: 2),
                        Text('${product.rating}', style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600)),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(12, 10, 12, 12),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(
                  product.name,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 14),
                ),
                const SizedBox(height: 2),
                Text(product.category, style: TextStyle(color: scheme.outline, fontSize: 12)),
                const SizedBox(height: 8),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: <Widget>[
                    Text(
                      '\$${product.price.toStringAsFixed(0)}',
                      style: TextStyle(fontWeight: FontWeight.w700, fontSize: 16, color: scheme.primary),
                    ),
                    FilledButton(
                      onPressed: onAdd,
                      style: FilledButton.styleFrom(
                        padding: const EdgeInsets.symmetric(horizontal: 14),
                        minimumSize: const Size(0, 36),
                      ),
                      child: const Text('Add'),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
