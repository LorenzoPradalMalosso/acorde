import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:acorde_mobile/main.dart';

void main() {
  testWidgets('shows accessible sign-in form before a session exists', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(const AcordeApp());

    expect(find.text('Acorde 2.0'), findsOneWidget);
    expect(find.text('Entrar'), findsNWidgets(2));
    expect(find.widgetWithText(FilledButton, 'Entrar'), findsOneWidget);
    expect(find.byType(TextFormField), findsNWidgets(2));
    expect(find.text('E-mail'), findsOneWidget);
    expect(find.text('Senha'), findsOneWidget);
  });

  testWidgets('allows switching between light and dark themes', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(const AcordeApp());

    expect(
      tester.widget<MaterialApp>(find.byType(MaterialApp)).themeMode,
      ThemeMode.light,
    );

    await tester.tap(find.byTooltip('Ativar tema escuro'));
    await tester.pumpAndSettle();

    expect(
      tester.widget<MaterialApp>(find.byType(MaterialApp)).themeMode,
      ThemeMode.dark,
    );
    expect(find.byTooltip('Ativar tema claro'), findsOneWidget);
  });
}
