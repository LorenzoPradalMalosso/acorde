import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:acorde_mobile/main.dart';
import 'package:acorde_mobile/theme/acorde_colors.dart';

void main() {
  testWidgets('shows accessible sign-in form before a session exists', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(const AcordeApp());

    expect(find.bySemanticsLabel('Acorde 2.0'), findsOneWidget);
    expect(find.text('Entre na sua conta'), findsOneWidget);
    expect(find.widgetWithText(FilledButton, 'Entrar'), findsOneWidget);
    expect(find.byType(TextFormField), findsNWidgets(2));
    expect(find.text('E-mail'), findsOneWidget);
    expect(find.text('Senha'), findsOneWidget);

    await tester.tap(find.byTooltip('Mostrar senha'));
    await tester.pump();
    expect(find.byTooltip('Ocultar senha'), findsOneWidget);
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

  testWidgets('uses a readable heading color for each theme', (tester) async {
    await tester.pumpWidget(const AcordeApp());
    final app = tester.widget<MaterialApp>(find.byType(MaterialApp));

    for (final (theme, foreground) in [
      (app.theme!, AcordeColors.grafite),
      (app.darkTheme!, AcordeColors.offWhite),
    ]) {
      expect(theme.textTheme.headlineMedium?.color, foreground);
      expect(theme.textTheme.headlineSmall?.color, foreground);
      expect(theme.textTheme.titleLarge?.color, foreground);
      expect(theme.textTheme.titleMedium?.color, foreground);
    }
  });
}
