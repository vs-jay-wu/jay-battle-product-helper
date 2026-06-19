// Basic smoke test for the ShopDemo storefront.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:flutter_shop/main.dart';

void main() {
  testWidgets('Storefront renders title and a category bar', (WidgetTester tester) async {
    await tester.pumpWidget(const ShopDemoApp());
    await tester.pump();

    expect(find.text('ShopDemo'), findsOneWidget);
    expect(find.text('All'), findsOneWidget);
    expect(find.byType(ChoiceChip), findsWidgets);
  });
}
