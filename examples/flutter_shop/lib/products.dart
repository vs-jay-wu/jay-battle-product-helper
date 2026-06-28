/// Demo product catalog for the fake e-commerce app (test / demo only).
class Product {
  const Product({
    required this.id,
    required this.name,
    required this.category,
    required this.price,
    required this.rating,
    required this.reviews,
    required this.description,
  });

  final String id;
  final String name;
  final String category;
  final double price;
  final double rating;
  final int reviews;
  final String description;

  /// Deterministic placeholder photo per product (real image, falls back to a
  /// colored tile if offline — see ProductCard.errorBuilder).
  String get imageUrl => 'https://picsum.photos/seed/shop-$id/600/600';

  /// A small gallery used by the detail page (all deterministic per product).
  List<String> get gallery => <String>[
        imageUrl,
        'https://picsum.photos/seed/shop-$id-b/600/600',
        'https://picsum.photos/seed/shop-$id-c/600/600',
        'https://picsum.photos/seed/shop-$id-d/600/600',
      ];
}

const List<String> kCategories = <String>[
  'All',
  'Audio',
  'Wearables',
  'Home',
  'Accessories',
];

const List<Product> kProducts = <Product>[
  Product(
    id: '1',
    name: 'Aurora Wireless Headphones',
    category: 'Audio',
    price: 129.0,
    rating: 4.6,
    reviews: 842,
    description:
        'Over-ear wireless headphones with adaptive noise cancellation, plush '
        'memory-foam earcups and up to 30 hours of playback on a single charge.',
  ),
  Product(
    id: '2',
    name: 'Pulse Bluetooth Speaker',
    category: 'Audio',
    price: 79.0,
    rating: 4.3,
    reviews: 511,
    description:
        'Pocket-sized speaker with surprisingly big, room-filling sound, '
        'IPX7 water resistance and a 12-hour battery for all-day listening.',
  ),
  Product(
    id: '3',
    name: 'Nimbus Smart Watch',
    category: 'Wearables',
    price: 199.0,
    rating: 4.8,
    reviews: 1290,
    description:
        'A bright always-on AMOLED display, 24/7 heart-rate and sleep tracking, '
        'and 50+ workout modes wrapped in a lightweight aluminium case.',
  ),
  Product(
    id: '4',
    name: 'Trek Fitness Band',
    category: 'Wearables',
    price: 59.0,
    rating: 4.1,
    reviews: 376,
    description:
        'A slim activity band that counts steps, calories and sleep, with a '
        'week-long battery and a comfortable sweat-proof strap.',
  ),
  Product(
    id: '5',
    name: 'Ember Smart Mug',
    category: 'Home',
    price: 89.0,
    rating: 4.5,
    reviews: 654,
    description:
        'Keeps your coffee or tea at the exact temperature you choose, with app '
        'control and a charging coaster that tops it up between sips.',
  ),
  Product(
    id: '6',
    name: 'Lumen Desk Lamp',
    category: 'Home',
    price: 45.0,
    rating: 4.4,
    reviews: 289,
    description:
        'Adjustable LED desk lamp with five colour temperatures, stepless '
        'dimming and a USB-C port to keep your phone charged while you work.',
  ),
  Product(
    id: '7',
    name: 'Carry Everyday Backpack',
    category: 'Accessories',
    price: 69.0,
    rating: 4.7,
    reviews: 932,
    description:
        'A water-resistant 20L daypack with a padded laptop sleeve, hidden '
        'security pocket and comfortable airflow straps for the daily commute.',
  ),
  Product(
    id: '8',
    name: 'Vault Leather Wallet',
    category: 'Accessories',
    price: 39.0,
    rating: 4.2,
    reviews: 207,
    description:
        'Slim full-grain leather wallet with RFID-blocking card slots and a '
        'quick-access pull tab that ages beautifully over time.',
  ),
  Product(
    id: '9',
    name: 'Echo Noise-Cancel Earbuds',
    category: 'Audio',
    price: 149.0,
    rating: 4.9,
    reviews: 1583,
    description:
        'True-wireless earbuds with hybrid active noise cancellation, a secure '
        'in-ear fit and a compact case that delivers 28 hours of total battery.',
  ),
  Product(
    id: '10',
    name: 'Glide Wireless Mouse',
    category: 'Accessories',
    price: 29.0,
    rating: 4.0,
    reviews: 164,
    description:
        'A silent-click wireless mouse with a precise optical sensor, ergonomic '
        'contour and months of use on a single AA battery.',
  ),
  Product(
    id: '11',
    name: 'Halo Ring Light',
    category: 'Home',
    price: 55.0,
    rating: 4.3,
    reviews: 418,
    description:
        'A 10-inch LED ring light with adjustable brightness and colour, phone '
        'clamp and tripod — perfect for calls, streaming and creator content.',
  ),
  Product(
    id: '12',
    name: 'Stride Running Watch',
    category: 'Wearables',
    price: 179.0,
    rating: 4.6,
    reviews: 705,
    description:
        'A GPS running watch with route tracking, pace and cadence metrics and '
        'a rugged, lightweight build that survives every training session.',
  ),
];

/// Fast lookup by id, used by the cart / order screens.
final Map<String, Product> kProductsById = <String, Product>{
  for (final Product p in kProducts) p.id: p,
};
