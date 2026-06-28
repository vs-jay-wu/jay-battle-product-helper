import 'package:flutter/material.dart';

import 'products.dart';

/// Format a dollar amount the way the shop screens expect (two decimals).
String formatPrice(double value) => '\$${value.toStringAsFixed(2)}';

const List<String> _kMonths = <String>[
  'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
  'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec',
];

/// Lightweight date/time formatter (avoids pulling in the intl package).
String formatDateTime(DateTime dt) {
  final String month = _kMonths[dt.month - 1];
  final String hour = dt.hour.toString().padLeft(2, '0');
  final String minute = dt.minute.toString().padLeft(2, '0');
  return '$month ${dt.day}, ${dt.year} · $hour:$minute';
}

/// A single line in the cart, resolved against the live product catalog.
class CartEntry {
  const CartEntry(this.product, this.quantity);

  final Product product;
  final int quantity;

  double get lineTotal => product.price * quantity;
}

/// A snapshot of one product at the time an order was placed. Orders keep their
/// own copy so they stay stable even if the catalog were to change.
class OrderLine {
  const OrderLine({
    required this.productId,
    required this.name,
    required this.price,
    required this.quantity,
    required this.imageUrl,
  });

  final String productId;
  final String name;
  final double price;
  final int quantity;
  final String imageUrl;

  double get lineTotal => price * quantity;
}

/// A placed (fake) order. Lives only in memory for the lifetime of the app.
class Order {
  const Order({
    required this.id,
    required this.lines,
    required this.placedAt,
    required this.subtotal,
    required this.shipping,
    required this.tax,
    required this.total,
    required this.fullName,
    required this.address,
    required this.cardLast4,
  });

  final String id;
  final List<OrderLine> lines;
  final DateTime placedAt;
  final double subtotal;
  final double shipping;
  final double tax;
  final double total;
  final String fullName;
  final String address;
  final String cardLast4;

  int get itemCount => lines.fold(0, (int sum, OrderLine l) => sum + l.quantity);
}

/// In-memory store for the cart and order history. No persistence — everything
/// is lost on restart, which is intentional for this demo.
class ShopStore extends ChangeNotifier {
  final Map<String, int> _cart = <String, int>{};
  final List<Order> _orders = <Order>[];
  int _orderSeq = 0;
  String? _userName;
  String? _userEmail;

  // ---- Auth (fake) ---------------------------------------------------------

  bool get isLoggedIn => _userEmail != null;
  String? get userName => _userName;
  String? get userEmail => _userEmail;

  /// Fake sign-in — accepts anything and just records the session in memory.
  void login({required String name, required String email}) {
    _userName = name;
    _userEmail = email;
    notifyListeners();
  }

  /// Sign out and end the session: clears the in-memory cart. Order history is
  /// kept for the lifetime of the app so a re-login still shows past orders.
  void logout() {
    _userName = null;
    _userEmail = null;
    _cart.clear();
    notifyListeners();
  }

  // ---- Cart ----------------------------------------------------------------

  int get itemCount => _cart.values.fold(0, (int sum, int n) => sum + n);

  bool get isCartEmpty => _cart.isEmpty;

  int quantityOf(String productId) => _cart[productId] ?? 0;

  List<CartEntry> get entries {
    final List<CartEntry> list = <CartEntry>[];
    for (final MapEntry<String, int> e in _cart.entries) {
      final Product? p = kProductsById[e.key];
      if (p != null) list.add(CartEntry(p, e.value));
    }
    return list;
  }

  double get subtotal =>
      entries.fold(0.0, (double sum, CartEntry e) => sum + e.lineTotal);

  /// Free shipping over $150, otherwise a flat fee. Zero when the cart is empty.
  double get shipping => isCartEmpty ? 0.0 : (subtotal >= 150 ? 0.0 : 9.99);

  double get tax => subtotal * 0.08;

  double get total => subtotal + shipping + tax;

  void add(Product product, [int quantity = 1]) {
    _cart[product.id] = (_cart[product.id] ?? 0) + quantity;
    notifyListeners();
  }

  void setQuantity(String productId, int quantity) {
    if (quantity <= 0) {
      _cart.remove(productId);
    } else {
      _cart[productId] = quantity;
    }
    notifyListeners();
  }

  void increment(String productId) =>
      setQuantity(productId, quantityOf(productId) + 1);

  void decrement(String productId) =>
      setQuantity(productId, quantityOf(productId) - 1);

  void remove(String productId) {
    _cart.remove(productId);
    notifyListeners();
  }

  void clearCart() {
    _cart.clear();
    notifyListeners();
  }

  // ---- Orders --------------------------------------------------------------

  List<Order> get orders => List<Order>.unmodifiable(_orders);

  /// Turn the current cart into an order, clear the cart, and return it.
  Order placeOrder({
    required String fullName,
    required String address,
    required String cardLast4,
  }) {
    final List<OrderLine> lines = entries
        .map((CartEntry e) => OrderLine(
              productId: e.product.id,
              name: e.product.name,
              price: e.product.price,
              quantity: e.quantity,
              imageUrl: e.product.imageUrl,
            ))
        .toList();

    _orderSeq++;
    final Order order = Order(
      id: 'ORD-${(1000 + _orderSeq)}',
      lines: lines,
      placedAt: DateTime.now(),
      subtotal: subtotal,
      shipping: shipping,
      tax: tax,
      total: total,
      fullName: fullName,
      address: address,
      cardLast4: cardLast4,
    );
    _orders.insert(0, order);
    _cart.clear();
    notifyListeners();
    return order;
  }
}

/// Exposes the [ShopStore] to the widget tree. Because it is an
/// [InheritedNotifier], widgets that read it via [ShopScope.of] rebuild
/// automatically whenever the store calls `notifyListeners()`.
class ShopScope extends InheritedNotifier<ShopStore> {
  const ShopScope({
    super.key,
    required ShopStore store,
    required super.child,
  }) : super(notifier: store);

  static ShopStore of(BuildContext context) {
    final ShopScope? scope =
        context.dependOnInheritedWidgetOfExactType<ShopScope>();
    assert(scope?.notifier != null, 'No ShopScope found in context');
    return scope!.notifier!;
  }
}
