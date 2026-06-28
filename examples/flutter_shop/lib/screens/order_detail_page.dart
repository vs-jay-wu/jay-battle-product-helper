import 'package:flutter/material.dart';

import '../shop_store.dart';
import '../widgets.dart';

class OrderDetailPage extends StatelessWidget {
  const OrderDetailPage({super.key, required this.order});

  final Order order;

  @override
  Widget build(BuildContext context) {
    final ColorScheme scheme = Theme.of(context).colorScheme;
    final TextTheme text = Theme.of(context).textTheme;

    return Scaffold(
      backgroundColor: scheme.surfaceContainerLowest,
      appBar: AppBar(
        backgroundColor: scheme.surface,
        title: Text(order.id),
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
        children: <Widget>[
          Container(
            decoration: BoxDecoration(
              color: scheme.surface,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: scheme.outlineVariant),
            ),
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                _kv('Placed', formatDateTime(order.placedAt), scheme),
                const SizedBox(height: 8),
                _kv('Ship to', order.fullName, scheme),
                const SizedBox(height: 4),
                Text(order.address, style: TextStyle(color: scheme.onSurfaceVariant)),
                const SizedBox(height: 8),
                _kv('Payment', 'Card ending ${order.cardLast4}', scheme),
              ],
            ),
          ),
          const SizedBox(height: 20),
          Text('Items (${order.itemCount})',
              style: text.titleMedium?.copyWith(fontWeight: FontWeight.w700)),
          const SizedBox(height: 12),
          for (final OrderLine line in order.lines) ...<Widget>[
            _OrderLineTile(line: line, scheme: scheme),
            const SizedBox(height: 12),
          ],
          const SizedBox(height: 8),
          Container(
            decoration: BoxDecoration(
              color: scheme.surface,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: scheme.outlineVariant),
            ),
            padding: const EdgeInsets.all(16),
            child: Column(
              children: <Widget>[
                _total('Subtotal', order.subtotal, scheme),
                _total(order.shipping == 0 ? 'Shipping (free)' : 'Shipping',
                    order.shipping, scheme),
                _total('Tax', order.tax, scheme),
                const Divider(),
                _total('Total', order.total, scheme, emphasize: true),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _kv(String label, String value, ColorScheme scheme) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: <Widget>[
        Text(label, style: TextStyle(color: scheme.onSurfaceVariant)),
        Flexible(
          child: Text(value,
              textAlign: TextAlign.right,
              style: const TextStyle(fontWeight: FontWeight.w600)),
        ),
      ],
    );
  }

  Widget _total(String label, double value, ColorScheme scheme,
      {bool emphasize = false}) {
    final TextStyle style = emphasize
        ? const TextStyle(fontWeight: FontWeight.w800, fontSize: 16)
        : TextStyle(color: scheme.onSurfaceVariant);
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: <Widget>[
          Text(label, style: style),
          Text(formatPrice(value), style: style),
        ],
      ),
    );
  }
}

class _OrderLineTile extends StatelessWidget {
  const _OrderLineTile({required this.line, required this.scheme});

  final OrderLine line;
  final ColorScheme scheme;

  @override
  Widget build(BuildContext context) {
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
              width: 56,
              height: 56,
              child: ShopImage(url: line.imageUrl),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(line.name,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(fontWeight: FontWeight.w600)),
                const SizedBox(height: 2),
                Text('${line.quantity} × ${formatPrice(line.price)}',
                    style: TextStyle(color: scheme.outline, fontSize: 13)),
              ],
            ),
          ),
          Text(formatPrice(line.lineTotal),
              style: TextStyle(
                  fontWeight: FontWeight.w700, color: scheme.primary)),
        ],
      ),
    );
  }
}
