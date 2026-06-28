import 'package:designer_shell_bridge/designer_shell_bridge.dart';
import 'package:flutter/material.dart';

import 'products.dart';
import 'screens/login_page.dart';
import 'screens/product_detail_page.dart';
import 'shop_store.dart';
import 'widgets.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await DesignerBridge.init(); // Designer Shell bridge (debug-only, no-op in release)
  DesignerBridge.register();
  runApp(const ShopDemoApp());
}

class ShopDemoApp extends StatefulWidget {
  const ShopDemoApp({super.key});

  @override
  State<ShopDemoApp> createState() => _ShopDemoAppState();
}

class _ShopDemoAppState extends State<ShopDemoApp> {
  final ShopStore _store = ShopStore();

  @override
  void dispose() {
    _store.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return ShopScope(
      store: _store,
      child: MaterialApp(
        title: 'ShopDemo',
        debugShowCheckedModeBanner: false,
        theme: ThemeData(
          colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF4848F0)),
          useMaterial3: true,
        ),
        home: const AuthGate(),
        builder: (BuildContext context, Widget? child) => DesignModeScope(child: child!),
      ),
    );
  }
}

/// Shows the login screen until the user signs in, then the storefront. Because
/// it reads the store via [ShopScope], it rebuilds automatically on login and
/// logout — no manual navigation needed.
class AuthGate extends StatelessWidget {
  const AuthGate({super.key});

  @override
  Widget build(BuildContext context) {
    final bool loggedIn = ShopScope.of(context).isLoggedIn;
    return loggedIn ? const StorefrontPage() : const LoginPage();
  }
}

class StorefrontPage extends StatefulWidget {
  const StorefrontPage({super.key});

  @override
  State<StorefrontPage> createState() => _StorefrontPageState();
}

class _StorefrontPageState extends State<StorefrontPage> {
  String _category = 'All';

  List<Product> get _visible => _category == 'All'
      ? kProducts
      : kProducts.where((Product p) => p.category == _category).toList();

  void _addToCart(Product p) {
    ShopScope.of(context).add(p);
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

  void _openDetail(Product p) {
    Navigator.of(context).push(
      MaterialPageRoute<void>(builder: (_) => ProductDetailPage(product: p)),
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
          const AccountButton(),
          const CartButton(),
          const SizedBox(width: 8),
        ],
      ),
      body: DesignNode(
        name: 'Storefront',
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            DesignNode(
              name: 'CategoryBar',
              child: _CategoryBar(
                categories: kCategories,
                selected: _category,
                onSelected: (String c) => setState(() => _category = c),
              ),
            ),
            Expanded(
              child: DesignNode(
                name: 'ProductGrid',
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
                    return DesignNode(
                      name: 'ProductCard',
                      id: 'product-${p.id}',
                      child: ProductCard(
                        product: p,
                        onAdd: () => _addToCart(p),
                        onTap: () => _openDetail(p),
                      ),
                    );
                  },
                ),
              ),
            ),
          ],
        ),
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
  const ProductCard({
    super.key,
    required this.product,
    required this.onAdd,
    required this.onTap,
  });

  final Product product;
  final VoidCallback onAdd;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final ColorScheme scheme = Theme.of(context).colorScheme;
    return Material(
      color: scheme.surface,
      borderRadius: BorderRadius.circular(16),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Ink(
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: scheme.outlineVariant),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              Expanded(
                child: Stack(
                  fit: StackFit.expand,
                  children: <Widget>[
                    ShopImage(url: product.imageUrl),
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
        ),
      ),
    );
  }
}
