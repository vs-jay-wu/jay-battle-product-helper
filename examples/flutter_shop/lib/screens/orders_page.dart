import 'package:flutter/material.dart';

import '../shop_store.dart';
import '../widgets.dart';
import 'order_detail_page.dart';

class OrdersPage extends StatelessWidget {
  const OrdersPage({super.key});

  @override
  Widget build(BuildContext context) {
    final ColorScheme scheme = Theme.of(context).colorScheme;
    final ShopStore store = ShopScope.of(context);
    final List<Order> orders = store.orders;

    return Scaffold(
      backgroundColor: scheme.surfaceContainerLowest,
      appBar: AppBar(
        backgroundColor: scheme.surface,
        title: const Text('Order History'),
      ),
      body: orders.isEmpty
          ? _EmptyOrders(scheme: scheme)
          : ListView.separated(
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
              itemCount: orders.length,
              separatorBuilder: (_, _) => const SizedBox(height: 12),
              itemBuilder: (BuildContext context, int index) {
                return _OrderCard(order: orders[index]);
              },
            ),
    );
  }
}

class _EmptyOrders extends StatelessWidget {
  const _EmptyOrders({required this.scheme});

  final ColorScheme scheme;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: <Widget>[
          Icon(Icons.receipt_long_outlined, size: 72, color: scheme.outline),
          const SizedBox(height: 16),
          Text('No orders yet',
              style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 4),
          Text('Your placed orders will show up here.',
              style: TextStyle(color: scheme.outline)),
        ],
      ),
    );
  }
}

class _OrderCard extends StatelessWidget {
  const _OrderCard({required this.order});

  final Order order;

  @override
  Widget build(BuildContext context) {
    final ColorScheme scheme = Theme.of(context).colorScheme;
    final List<String> thumbs =
        order.lines.take(3).map((OrderLine l) => l.imageUrl).toList();

    return InkWell(
      borderRadius: BorderRadius.circular(16),
      onTap: () => Navigator.of(context).push(
        MaterialPageRoute<void>(
          builder: (_) => OrderDetailPage(order: order),
        ),
      ),
      child: Container(
        decoration: BoxDecoration(
          color: scheme.surface,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: scheme.outlineVariant),
        ),
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            Row(
              children: <Widget>[
                Text(order.id,
                    style: const TextStyle(fontWeight: FontWeight.w700)),
                const Spacer(),
                Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                  decoration: BoxDecoration(
                    color: scheme.primaryContainer,
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: Text('Confirmed',
                      style: TextStyle(
                        color: scheme.onPrimaryContainer,
                        fontSize: 12,
                        fontWeight: FontWeight.w600,
                      )),
                ),
              ],
            ),
            const SizedBox(height: 4),
            Text(formatDateTime(order.placedAt),
                style: TextStyle(color: scheme.outline, fontSize: 13)),
            const SizedBox(height: 12),
            Row(
              children: <Widget>[
                for (final String url in thumbs) ...<Widget>[
                  ClipRRect(
                    borderRadius: BorderRadius.circular(10),
                    child: SizedBox(
                      width: 48,
                      height: 48,
                      child: ShopImage(url: url),
                    ),
                  ),
                  const SizedBox(width: 8),
                ],
                const Spacer(),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.end,
                  children: <Widget>[
                    Text('${order.itemCount} item(s)',
                        style: TextStyle(color: scheme.outline, fontSize: 12)),
                    const SizedBox(height: 2),
                    Text(formatPrice(order.total),
                        style: TextStyle(
                          fontWeight: FontWeight.w800,
                          fontSize: 16,
                          color: scheme.primary,
                        )),
                  ],
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
