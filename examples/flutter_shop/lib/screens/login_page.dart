import 'package:flutter/material.dart';

import '../shop_store.dart';

/// Derive a friendly display name from an email's local part.
String nameFromEmail(String email) {
  final String local = email.split('@').first;
  final Iterable<String> parts =
      local.split(RegExp(r'[._\-+]+')).where((String s) => s.isNotEmpty);
  if (parts.isEmpty) return 'Shopper';
  return parts
      .map((String p) => p[0].toUpperCase() + p.substring(1))
      .join(' ');
}

/// Fake sign-in screen. Any email + password is accepted.
class LoginPage extends StatefulWidget {
  const LoginPage({super.key});

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  final TextEditingController _email =
      TextEditingController(text: 'demo@shop.app');
  final TextEditingController _password =
      TextEditingController(text: '123456');
  bool _obscure = true;
  bool _busy = false;

  @override
  void dispose() {
    _email.dispose();
    _password.dispose();
    super.dispose();
  }

  Future<void> _signIn() async {
    if (!_formKey.currentState!.validate()) return;
    FocusScope.of(context).unfocus();
    setState(() => _busy = true);

    // Fake authentication — no real backend is contacted.
    await Future<void>.delayed(const Duration(milliseconds: 1100));
    if (!mounted) return;

    final String email = _email.text.trim();
    // The AuthGate watches the store and swaps to the storefront on login.
    ShopScope.of(context).login(name: nameFromEmail(email), email: email);
  }

  @override
  Widget build(BuildContext context) {
    final ColorScheme scheme = Theme.of(context).colorScheme;
    final TextTheme text = Theme.of(context).textTheme;

    return Scaffold(
      backgroundColor: scheme.surfaceContainerLowest,
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 420),
              child: Form(
                key: _formKey,
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: <Widget>[
                    Container(
                      width: 72,
                      height: 72,
                      alignment: Alignment.center,
                      decoration: BoxDecoration(
                        color: scheme.primaryContainer,
                        borderRadius: BorderRadius.circular(20),
                      ),
                      child: Icon(Icons.shopping_bag_rounded,
                          size: 38, color: scheme.onPrimaryContainer),
                    ),
                    const SizedBox(height: 20),
                    Text('Welcome to ShopDemo',
                        textAlign: TextAlign.center,
                        style: text.headlineSmall
                            ?.copyWith(fontWeight: FontWeight.w800)),
                    const SizedBox(height: 6),
                    Text('Sign in to start shopping',
                        textAlign: TextAlign.center,
                        style: TextStyle(color: scheme.onSurfaceVariant)),
                    const SizedBox(height: 28),
                    TextFormField(
                      controller: _email,
                      keyboardType: TextInputType.emailAddress,
                      autocorrect: false,
                      decoration: const InputDecoration(
                        labelText: 'Email',
                        prefixIcon: Icon(Icons.mail_outline),
                        border: OutlineInputBorder(),
                      ),
                      validator: (String? v) {
                        final String s = (v ?? '').trim();
                        if (s.isEmpty) return 'Enter your email';
                        if (!s.contains('@')) return 'Enter a valid email';
                        return null;
                      },
                    ),
                    const SizedBox(height: 14),
                    TextFormField(
                      controller: _password,
                      obscureText: _obscure,
                      decoration: InputDecoration(
                        labelText: 'Password',
                        prefixIcon: const Icon(Icons.lock_outline),
                        border: const OutlineInputBorder(),
                        suffixIcon: IconButton(
                          onPressed: () =>
                              setState(() => _obscure = !_obscure),
                          icon: Icon(_obscure
                              ? Icons.visibility_outlined
                              : Icons.visibility_off_outlined),
                        ),
                      ),
                      validator: (String? v) =>
                          (v == null || v.isEmpty) ? 'Enter your password' : null,
                    ),
                    const SizedBox(height: 24),
                    FilledButton(
                      onPressed: _busy ? null : _signIn,
                      style: FilledButton.styleFrom(
                          minimumSize: const Size(0, 52)),
                      child: _busy
                          ? const SizedBox(
                              height: 22,
                              width: 22,
                              child: CircularProgressIndicator(
                                  strokeWidth: 2.5, color: Colors.white),
                            )
                          : const Text('Sign in'),
                    ),
                    const SizedBox(height: 16),
                    Text(
                      'This is a demo — any email and password will work.',
                      textAlign: TextAlign.center,
                      style: TextStyle(color: scheme.outline, fontSize: 12),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
