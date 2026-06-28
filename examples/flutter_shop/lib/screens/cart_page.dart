import 'package:flutter/material.dart';

import '../shop_store.dart';
import '../widgets.dart';
import 'checkout_page.dart';

class CartPage extends StatelessWidget {
  const CartPage({super.key});

  @override
  Widget build(BuildContext context) {
    final ColorScheme scheme = Theme.of(context).colorScheme;
    final ShopStore store = ShopScope.of(context);
    final List<CartEntry> entries = store.entries;

    return Scaffold(
      backgroundColor: scheme.surfaceContainerLowest,
      appBar: AppBar(
        backgroundColor: scheme.surface,
        title: const Text('Your Cart'),
        actions: <Widget>[
          if (entries.isNotEmpty)
            TextButton(
              onPressed: () => store.clearCart(),
              child: const Text('Clear'),
            ),
          const SizedBox(width: 8),
        ],
      ),
      body: entries.isEmpty
          ? _EmptyCart(scheme: scheme)
          : ListView.separated(
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
              itemCount: entries.length,
              separatorBuilder: (_, _) => const SizedBox(height: 12),
              itemBuilder: (BuildContext context, int index) {
                return _CartTile(entry: entries[index], store: store);
              },
            ),
      bottomNavigationBar:
          entries.isEmpty ? null : _CartSummaryBar(store: store),
    );
  }
}

class _EmptyCart extends StatelessWidget {
  const _EmptyCart({required this.scheme});

  final ColorScheme scheme;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: <Widget>[
          Icon(Icons.shopping_cart_outlined, size: 72, color: scheme.outline),
          const SizedBox(height: 16),
          Text('Your cart is empty',
              style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 4),
          Text('Browse the store and add something you like.',
              style: TextStyle(color: scheme.outline)),
          const SizedBox(height: 20),
          FilledButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Continue shopping'),
          ),
        ],
      ),
    );
  }
}

class _CartTile extends StatelessWidget {
  const _CartTile({required this.entry, required this.store});

  final CartEntry entry;
  final ShopStore store;

  @override
  Widget build(BuildContext context) {
    final ColorScheme scheme = Theme.of(context).colorScheme;
    return Container(
      decoration: BoxDecoration(
        color: scheme.surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: scheme.outlineVariant),
      ),
      padding: const EdgeInsets.all(10),
      child: Row(
        children: <Widget>[
          ClipRRect(
            borderRadius: BorderRadius.circular(12),
            child: SizedBox(
              width: 72,
              height: 72,
              child: ShopImage(url: entry.product.imageUrl),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(entry.product.name,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(fontWeight: FontWeight.w600)),
                const SizedBox(height: 2),
                Text(formatPrice(entry.product.price),
                    style: TextStyle(color: scheme.outline, fontSize: 13)),
                const SizedBox(height: 8),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: <Widget>[
                    QuantityStepper(
                      quantity: entry.quantity,
                      onDecrement: () => store.decrement(entry.product.id),
                      onIncrement: () => store.increment(entry.product.id),
                    ),
                    Text(formatPrice(entry.lineTotal),
                        style: TextStyle(
                          fontWeight: FontWeight.w700,
                          color: scheme.primary,
                        )),
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

class _CartSummaryBar extends StatelessWidget {
  const _CartSummaryBar({required this.store});

  final ShopStore store;

  @override
  Widget build(BuildContext context) {
    final ColorScheme scheme = Theme.of(context).colorScheme;
    return Material(
      color: scheme.surface,
      elevation: 8,
      child: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: <Widget>[
              _SummaryRow(label: 'Subtotal', value: store.subtotal),
              const SizedBox(height: 4),
              _SummaryRow(
                label: store.shipping == 0 ? 'Shipping (free)' : 'Shipping',
                value: store.shipping,
              ),
              const SizedBox(height: 4),
              _SummaryRow(label: 'Tax (8%)', value: store.tax),
              const Divider(height: 20),
              _SummaryRow(label: 'Total', value: store.total, emphasize: true),
              const SizedBox(height: 12),
              SizedBox(
                width: double.infinity,
                child: FilledButton(
                  onPressed: () => Navigator.of(context).push(
                    MaterialPageRoute<void>(
                      builder: (_) => const CheckoutPage(),
                    ),
                  ),
                  style: FilledButton.styleFrom(minimumSize: const Size(0, 52)),
                  child: Text('Checkout · ${formatPrice(store.total)}'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _SummaryRow extends StatelessWidget {
  const _SummaryRow({
    required this.label,
    required this.value,
    this.emphasize = false,
  });

  final String label;
  final double value;
  final bool emphasize;

  @override
  Widget build(BuildContext context) {
    final ColorScheme scheme = Theme.of(context).colorScheme;
    final TextStyle? style = emphasize
        ? Theme.of(context)
            .textTheme
            .titleMedium
            ?.copyWith(fontWeight: FontWeight.w800)
        : TextStyle(color: scheme.onSurfaceVariant);
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: <Widget>[
        Text(label, style: style),
        Text(formatPrice(value), style: style),
      ],
    );
  }
}
