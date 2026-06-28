import 'package:flutter/material.dart';

import '../shop_store.dart';
import '../widgets.dart';
import 'orders_page.dart';

class AccountPage extends StatelessWidget {
  const AccountPage({super.key});

  Future<void> _confirmLogout(BuildContext context) async {
    final bool? ok = await showDialog<bool>(
      context: context,
      builder: (BuildContext context) => AlertDialog(
        title: const Text('Sign out?'),
        content: const Text('You can sign back in anytime — any email works.'),
        actions: <Widget>[
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('Sign out'),
          ),
        ],
      ),
    );
    if (ok != true || !context.mounted) return;
    ShopScope.of(context).logout();
    // Return to the root; the AuthGate then shows the login screen.
    Navigator.of(context).popUntil((Route<dynamic> r) => r.isFirst);
  }

  @override
  Widget build(BuildContext context) {
    final ColorScheme scheme = Theme.of(context).colorScheme;
    final TextTheme text = Theme.of(context).textTheme;
    final ShopStore store = ShopScope.of(context);
    final String name = store.userName ?? 'Guest';
    final String email = store.userEmail ?? '';

    return Scaffold(
      backgroundColor: scheme.surfaceContainerLowest,
      appBar: AppBar(
        backgroundColor: scheme.surface,
        title: const Text('Account'),
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
            child: Row(
              children: <Widget>[
                InitialsAvatar(name: name, radius: 28),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      Text(name,
                          style: text.titleMedium
                              ?.copyWith(fontWeight: FontWeight.w700)),
                      const SizedBox(height: 2),
                      Text(email,
                          style: TextStyle(color: scheme.onSurfaceVariant)),
                    ],
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),
          Container(
            decoration: BoxDecoration(
              color: scheme.surface,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: scheme.outlineVariant),
            ),
            clipBehavior: Clip.antiAlias,
            child: Column(
              children: <Widget>[
                ListTile(
                  leading: const Icon(Icons.receipt_long_outlined),
                  title: const Text('Order history'),
                  subtitle: Text('${store.orders.length} order(s)'),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => Navigator.of(context).push(
                    MaterialPageRoute<void>(builder: (_) => const OrdersPage()),
                  ),
                ),
                const Divider(height: 1),
                const ListTile(
                  leading: Icon(Icons.favorite_border),
                  title: Text('Wishlist'),
                  subtitle: Text('Coming soon'),
                  enabled: false,
                ),
              ],
            ),
          ),
          const SizedBox(height: 24),
          OutlinedButton.icon(
            onPressed: () => _confirmLogout(context),
            icon: Icon(Icons.logout, color: scheme.error),
            label: Text('Sign out', style: TextStyle(color: scheme.error)),
            style: OutlinedButton.styleFrom(
              minimumSize: const Size(0, 52),
              side: BorderSide(color: scheme.error),
            ),
          ),
        ],
      ),
    );
  }
}
