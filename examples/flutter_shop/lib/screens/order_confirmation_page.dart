import 'package:flutter/material.dart';

import '../shop_store.dart';
import 'order_detail_page.dart';
import 'orders_page.dart';

/// Shown after a successful (fake) payment. Replaces the checkout route so the
/// back button returns to the store rather than the payment form.
class OrderConfirmationPage extends StatelessWidget {
  const OrderConfirmationPage({super.key, required this.order});

  final Order order;

  @override
  Widget build(BuildContext context) {
    final ColorScheme scheme = Theme.of(context).colorScheme;
    final TextTheme text = Theme.of(context).textTheme;

    return Scaffold(
      backgroundColor: scheme.surfaceContainerLowest,
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            children: <Widget>[
              const Spacer(),
              Container(
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  color: scheme.primaryContainer,
                  shape: BoxShape.circle,
                ),
                child: Icon(Icons.check_rounded,
                    size: 64, color: scheme.onPrimaryContainer),
              ),
              const SizedBox(height: 24),
              Text('Order confirmed!',
                  style: text.headlineSmall?.copyWith(fontWeight: FontWeight.w800)),
              const SizedBox(height: 8),
              Text(
                'Thanks, ${order.fullName}. Your order is on its way.',
                textAlign: TextAlign.center,
                style: TextStyle(color: scheme.onSurfaceVariant),
              ),
              const SizedBox(height: 24),
              Container(
                width: double.infinity,
                decoration: BoxDecoration(
                  color: scheme.surface,
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(color: scheme.outlineVariant),
                ),
                padding: const EdgeInsets.all(16),
                child: Column(
                  children: <Widget>[
                    _row('Order', order.id),
                    const SizedBox(height: 8),
                    _row('Placed', formatDateTime(order.placedAt)),
                    const SizedBox(height: 8),
                    _row('Items', '${order.itemCount}'),
                    const SizedBox(height: 8),
                    _row('Paid', formatPrice(order.total), emphasize: true),
                  ],
                ),
              ),
              const Spacer(),
              SizedBox(
                width: double.infinity,
                child: FilledButton(
                  onPressed: () => Navigator.of(context).push(
                    MaterialPageRoute<void>(
                      builder: (_) => OrderDetailPage(order: order),
                    ),
                  ),
                  style: FilledButton.styleFrom(minimumSize: const Size(0, 52)),
                  child: const Text('View order details'),
                ),
              ),
              const SizedBox(height: 10),
              SizedBox(
                width: double.infinity,
                child: OutlinedButton(
                  onPressed: () {
                    Navigator.of(context).push(
                      MaterialPageRoute<void>(
                        builder: (_) => const OrdersPage(),
                      ),
                    );
                  },
                  style: OutlinedButton.styleFrom(minimumSize: const Size(0, 52)),
                  child: const Text('Order history'),
                ),
              ),
              const SizedBox(height: 10),
              SizedBox(
                width: double.infinity,
                child: TextButton(
                  onPressed: () =>
                      Navigator.of(context).popUntil((Route<dynamic> r) => r.isFirst),
                  child: const Text('Continue shopping'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _row(String label, String value, {bool emphasize = false}) {
    return Builder(builder: (BuildContext context) {
      final ColorScheme scheme = Theme.of(context).colorScheme;
      final TextStyle valueStyle = emphasize
          ? const TextStyle(fontWeight: FontWeight.w800, fontSize: 16)
          : const TextStyle(fontWeight: FontWeight.w600);
      return Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: <Widget>[
          Text(label, style: TextStyle(color: scheme.onSurfaceVariant)),
          Text(value, style: valueStyle),
        ],
      );
    });
  }
}
