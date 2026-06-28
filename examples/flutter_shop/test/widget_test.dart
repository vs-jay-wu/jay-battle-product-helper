// Basic smoke tests for the ShopDemo app.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:flutter_shop/main.dart';

void main() {
  testWidgets('Boots to the login screen', (WidgetTester tester) async {
    await tester.pumpWidget(const ShopDemoApp());
    await tester.pump();

    expect(find.text('Welcome to ShopDemo'), findsOneWidget);
    expect(find.text('Sign in'), findsOneWidget);
  });

  testWidgets('Signing in reveals the storefront', (WidgetTester tester) async {
    await tester.pumpWidget(const ShopDemoApp());
    await tester.pump();

    await tester.tap(find.text('Sign in'));
    await tester.pumpAndSettle(); // let the fake auth delay elapse + swap pages

    expect(find.text('ShopDemo'), findsOneWidget);
    expect(find.text('All'), findsOneWidget);
    expect(find.byType(ChoiceChip), findsWidgets);
  });
}
