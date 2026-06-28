import 'package:flutter/material.dart';

import '../products.dart';
import '../shop_store.dart';
import '../widgets.dart';
import 'cart_page.dart';

class ProductDetailPage extends StatefulWidget {
  const ProductDetailPage({super.key, required this.product});

  final Product product;

  @override
  State<ProductDetailPage> createState() => _ProductDetailPageState();
}

class _ProductDetailPageState extends State<ProductDetailPage> {
  int _selectedImage = 0;
  int _quantity = 1;

  Product get _product => widget.product;

  void _addToCart({bool thenOpenCart = false}) {
    final ShopStore store = ShopScope.of(context);
    store.add(_product, _quantity);
    if (thenOpenCart) {
      Navigator.of(context).push(
        MaterialPageRoute<void>(builder: (_) => const CartPage()),
      );
      return;
    }
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(
        SnackBar(
          content: Text('Added $_quantity × "${_product.name}" to cart'),
          duration: const Duration(milliseconds: 1100),
          behavior: SnackBarBehavior.floating,
        ),
      );
  }

  @override
  Widget build(BuildContext context) {
    final ColorScheme scheme = Theme.of(context).colorScheme;
    final TextTheme text = Theme.of(context).textTheme;
    final List<String> gallery = _product.gallery;

    return Scaffold(
      backgroundColor: scheme.surfaceContainerLowest,
      appBar: AppBar(
        backgroundColor: scheme.surface,
        title: const Text('Details'),
        actions: const <Widget>[CartButton(), SizedBox(width: 8)],
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 16, 16, 32),
        children: <Widget>[
          AspectRatio(
            aspectRatio: 1,
            child: ClipRRect(
              borderRadius: BorderRadius.circular(20),
              child: ShopImage(url: gallery[_selectedImage]),
            ),
          ),
          const SizedBox(height: 12),
          SizedBox(
            height: 72,
            child: ListView.separated(
              scrollDirection: Axis.horizontal,
              itemCount: gallery.length,
              separatorBuilder: (_, _) => const SizedBox(width: 10),
              itemBuilder: (BuildContext context, int index) {
                final bool selected = index == _selectedImage;
                return GestureDetector(
                  onTap: () => setState(() => _selectedImage = index),
                  child: Container(
                    width: 72,
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(
                        color: selected ? scheme.primary : scheme.outlineVariant,
                        width: selected ? 2 : 1,
                      ),
                    ),
                    clipBehavior: Clip.antiAlias,
                    child: ShopImage(url: gallery[index]),
                  ),
                );
              },
            ),
          ),
          const SizedBox(height: 20),
          Text(_product.category.toUpperCase(),
              style: text.labelSmall?.copyWith(
                color: scheme.primary,
                letterSpacing: 1,
                fontWeight: FontWeight.w700,
              )),
          const SizedBox(height: 6),
          Text(_product.name, style: text.headlineSmall?.copyWith(fontWeight: FontWeight.w700)),
          const SizedBox(height: 10),
          Row(
            children: <Widget>[
              const Icon(Icons.star_rounded, size: 20, color: Color(0xFFF5A623)),
              const SizedBox(width: 4),
              Text('${_product.rating}',
                  style: const TextStyle(fontWeight: FontWeight.w700)),
              const SizedBox(width: 6),
              Text('(${_product.reviews} reviews)',
                  style: TextStyle(color: scheme.outline)),
            ],
          ),
          const SizedBox(height: 16),
          Text(formatPrice(_product.price),
              style: text.headlineMedium?.copyWith(
                color: scheme.primary,
                fontWeight: FontWeight.w800,
              )),
          const SizedBox(height: 20),
          Text('Description',
              style: text.titleMedium?.copyWith(fontWeight: FontWeight.w700)),
          const SizedBox(height: 8),
          Text(_product.description,
              style: text.bodyMedium?.copyWith(height: 1.5, color: scheme.onSurfaceVariant)),
          const SizedBox(height: 24),
          Row(
            children: <Widget>[
              Text('Quantity',
                  style: text.titleMedium?.copyWith(fontWeight: FontWeight.w700)),
              const Spacer(),
              QuantityStepper(
                quantity: _quantity,
                onDecrement: () =>
                    setState(() => _quantity = _quantity > 1 ? _quantity - 1 : 1),
                onIncrement: () => setState(() => _quantity++),
              ),
            ],
          ),
        ],
      ),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 12),
          child: Row(
            children: <Widget>[
              Expanded(
                child: OutlinedButton(
                  onPressed: () => _addToCart(),
                  style: OutlinedButton.styleFrom(
                    minimumSize: const Size(0, 52),
                  ),
                  child: const Text('Add to cart'),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: FilledButton(
                  onPressed: () => _addToCart(thenOpenCart: true),
                  style: FilledButton.styleFrom(
                    minimumSize: const Size(0, 52),
                  ),
                  child: const Text('Buy now'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
