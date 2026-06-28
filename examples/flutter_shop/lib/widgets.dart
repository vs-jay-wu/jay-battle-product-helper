import 'package:flutter/material.dart';

import 'screens/cart_page.dart';
import 'shop_store.dart';

/// A network image with the same loading / error treatment used across the app.
class ShopImage extends StatelessWidget {
  const ShopImage({super.key, required this.url, this.fit = BoxFit.cover});

  final String url;
  final BoxFit fit;

  @override
  Widget build(BuildContext context) {
    final ColorScheme scheme = Theme.of(context).colorScheme;
    return Image.network(
      url,
      fit: fit,
      loadingBuilder:
          (BuildContext context, Widget child, ImageChunkEvent? progress) {
        if (progress == null) return child;
        return Container(
          color: scheme.surfaceContainerHighest,
          child: const Center(child: CircularProgressIndicator(strokeWidth: 2)),
        );
      },
      errorBuilder: (_, _, _) => Container(
        color: scheme.surfaceContainerHighest,
        child: Icon(Icons.image_outlined, color: scheme.outline, size: 40),
      ),
    );
  }
}

/// App-bar cart icon with a live item-count badge. Tapping opens the cart.
class CartButton extends StatelessWidget {
  const CartButton({super.key});

  @override
  Widget build(BuildContext context) {
    final ColorScheme scheme = Theme.of(context).colorScheme;
    final int count = ShopScope.of(context).itemCount;
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 4),
      child: Stack(
        clipBehavior: Clip.none,
        children: <Widget>[
          IconButton.filledTonal(
            onPressed: () => Navigator.of(context).push(
              MaterialPageRoute<void>(
                builder: (_) => const CartPage(),
              ),
            ),
            icon: const Icon(Icons.shopping_cart_outlined),
          ),
          if (count > 0)
            Positioned(
              right: -2,
              top: -2,
              child: Container(
                padding: const EdgeInsets.all(5),
                decoration:
                    BoxDecoration(color: scheme.error, shape: BoxShape.circle),
                constraints: const BoxConstraints(minWidth: 20, minHeight: 20),
                child: Text(
                  '$count',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    color: scheme.onError,
                    fontSize: 11,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }
}

/// Compact +/- stepper used in the cart.
class QuantityStepper extends StatelessWidget {
  const QuantityStepper({
    super.key,
    required this.quantity,
    required this.onDecrement,
    required this.onIncrement,
  });

  final int quantity;
  final VoidCallback onDecrement;
  final VoidCallback onIncrement;

  @override
  Widget build(BuildContext context) {
    final ColorScheme scheme = Theme.of(context).colorScheme;
    return Container(
      decoration: BoxDecoration(
        border: Border.all(color: scheme.outlineVariant),
        borderRadius: BorderRadius.circular(24),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: <Widget>[
          _StepIcon(
            icon: quantity > 1 ? Icons.remove : Icons.delete_outline,
            onTap: onDecrement,
          ),
          SizedBox(
            width: 28,
            child: Text(
              '$quantity',
              textAlign: TextAlign.center,
              style: const TextStyle(fontWeight: FontWeight.w700),
            ),
          ),
          _StepIcon(icon: Icons.add, onTap: onIncrement),
        ],
      ),
    );
  }
}

class _StepIcon extends StatelessWidget {
  const _StepIcon({required this.icon, required this.onTap});

  final IconData icon;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkResponse(
      onTap: onTap,
      radius: 20,
      child: Padding(
        padding: const EdgeInsets.all(8),
        child: Icon(icon, size: 18),
      ),
    );
  }
}
