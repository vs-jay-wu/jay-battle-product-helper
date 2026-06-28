import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../shop_store.dart';
import 'order_confirmation_page.dart';

class CheckoutPage extends StatefulWidget {
  const CheckoutPage({super.key});

  @override
  State<CheckoutPage> createState() => _CheckoutPageState();
}

class _CheckoutPageState extends State<CheckoutPage> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  final TextEditingController _name = TextEditingController(text: 'Demo User');
  final TextEditingController _address =
      TextEditingController(text: '123 Demo Street, Sample City');
  final TextEditingController _card =
      TextEditingController(text: '4242 4242 4242 4242');
  final TextEditingController _expiry = TextEditingController(text: '12/29');
  final TextEditingController _cvc = TextEditingController(text: '123');

  bool _processing = false;

  @override
  void dispose() {
    _name.dispose();
    _address.dispose();
    _card.dispose();
    _expiry.dispose();
    _cvc.dispose();
    super.dispose();
  }

  Future<void> _pay() async {
    if (!_formKey.currentState!.validate()) return;
    FocusScope.of(context).unfocus();
    setState(() => _processing = true);

    // Fake payment processing — no real gateway is contacted.
    await Future<void>.delayed(const Duration(milliseconds: 1400));
    if (!mounted) return;

    final ShopStore store = ShopScope.of(context);
    final String digits = _card.text.replaceAll(RegExp(r'\D'), '');
    final String last4 =
        digits.length >= 4 ? digits.substring(digits.length - 4) : '0000';

    final Order order = store.placeOrder(
      fullName: _name.text.trim(),
      address: _address.text.trim(),
      cardLast4: last4,
    );

    if (!mounted) return;
    Navigator.of(context).pushReplacement(
      MaterialPageRoute<void>(
        builder: (_) => OrderConfirmationPage(order: order),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final ColorScheme scheme = Theme.of(context).colorScheme;
    final TextTheme text = Theme.of(context).textTheme;
    final ShopStore store = ShopScope.of(context);

    return Scaffold(
      backgroundColor: scheme.surfaceContainerLowest,
      appBar: AppBar(
        backgroundColor: scheme.surface,
        title: const Text('Checkout'),
      ),
      body: AbsorbPointer(
        absorbing: _processing,
        child: Form(
          key: _formKey,
          child: ListView(
            padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
            children: <Widget>[
              _SectionTitle('Shipping address', text: text),
              const SizedBox(height: 12),
              _Field(
                controller: _name,
                label: 'Full name',
                icon: Icons.person_outline,
              ),
              const SizedBox(height: 12),
              _Field(
                controller: _address,
                label: 'Address',
                icon: Icons.home_outlined,
                maxLines: 2,
              ),
              const SizedBox(height: 24),
              _SectionTitle('Payment', text: text),
              const SizedBox(height: 6),
              Text('This is a demo — no real card is charged.',
                  style: TextStyle(color: scheme.outline, fontSize: 12)),
              const SizedBox(height: 12),
              _Field(
                controller: _card,
                label: 'Card number',
                icon: Icons.credit_card,
                keyboardType: TextInputType.number,
                inputFormatters: <TextInputFormatter>[
                  FilteringTextInputFormatter.allow(RegExp(r'[0-9 ]')),
                  LengthLimitingTextInputFormatter(19),
                ],
                validator: (String? v) {
                  final String digits = (v ?? '').replaceAll(RegExp(r'\D'), '');
                  if (digits.length < 12) return 'Enter a valid card number';
                  return null;
                },
              ),
              const SizedBox(height: 12),
              Row(
                children: <Widget>[
                  Expanded(
                    child: _Field(
                      controller: _expiry,
                      label: 'Expiry (MM/YY)',
                      icon: Icons.calendar_month_outlined,
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: _Field(
                      controller: _cvc,
                      label: 'CVC',
                      icon: Icons.lock_outline,
                      keyboardType: TextInputType.number,
                      inputFormatters: <TextInputFormatter>[
                        FilteringTextInputFormatter.digitsOnly,
                        LengthLimitingTextInputFormatter(4),
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 24),
              _OrderSummaryCard(store: store, scheme: scheme, text: text),
            ],
          ),
        ),
      ),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 12),
          child: FilledButton(
            onPressed: _processing ? null : _pay,
            style: FilledButton.styleFrom(minimumSize: const Size(0, 54)),
            child: _processing
                ? const SizedBox(
                    height: 22,
                    width: 22,
                    child: CircularProgressIndicator(
                        strokeWidth: 2.5, color: Colors.white),
                  )
                : Text('Pay ${formatPrice(store.total)}'),
          ),
        ),
      ),
    );
  }
}

class _SectionTitle extends StatelessWidget {
  const _SectionTitle(this.title, {required this.text});

  final String title;
  final TextTheme text;

  @override
  Widget build(BuildContext context) {
    return Text(title,
        style: text.titleMedium?.copyWith(fontWeight: FontWeight.w700));
  }
}

class _Field extends StatelessWidget {
  const _Field({
    required this.controller,
    required this.label,
    required this.icon,
    this.keyboardType,
    this.inputFormatters,
    this.validator,
    this.maxLines = 1,
  });

  final TextEditingController controller;
  final String label;
  final IconData icon;
  final TextInputType? keyboardType;
  final List<TextInputFormatter>? inputFormatters;
  final String? Function(String?)? validator;
  final int maxLines;

  @override
  Widget build(BuildContext context) {
    return TextFormField(
      controller: controller,
      keyboardType: keyboardType,
      inputFormatters: inputFormatters,
      maxLines: maxLines,
      validator: validator ??
          (String? v) =>
              (v == null || v.trim().isEmpty) ? 'Required' : null,
      decoration: InputDecoration(
        labelText: label,
        prefixIcon: Icon(icon),
        border: const OutlineInputBorder(),
      ),
    );
  }
}

class _OrderSummaryCard extends StatelessWidget {
  const _OrderSummaryCard({
    required this.store,
    required this.scheme,
    required this.text,
  });

  final ShopStore store;
  final ColorScheme scheme;
  final TextTheme text;

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: scheme.surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: scheme.outlineVariant),
      ),
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Text('Order summary',
              style: text.titleMedium?.copyWith(fontWeight: FontWeight.w700)),
          const SizedBox(height: 12),
          for (final CartEntry e in store.entries) ...<Widget>[
            Row(
              children: <Widget>[
                Expanded(
                  child: Text('${e.quantity} × ${e.product.name}',
                      maxLines: 1, overflow: TextOverflow.ellipsis),
                ),
                const SizedBox(width: 8),
                Text(formatPrice(e.lineTotal)),
              ],
            ),
            const SizedBox(height: 8),
          ],
          const Divider(),
          _row('Subtotal', store.subtotal),
          _row(store.shipping == 0 ? 'Shipping (free)' : 'Shipping',
              store.shipping),
          _row('Tax (8%)', store.tax),
          const Divider(),
          _row('Total', store.total, emphasize: true),
        ],
      ),
    );
  }

  Widget _row(String label, double value, {bool emphasize = false}) {
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
