import 'package:flutter/material.dart';

import 'acorde_colors.dart';

ThemeData acordeTheme(Brightness brightness) {
  final dark = brightness == Brightness.dark;
  final scheme =
      ColorScheme.fromSeed(
        seedColor: AcordeColors.verdeProfundo,
        brightness: brightness,
      ).copyWith(
        primary: dark ? AcordeColors.menta : AcordeColors.verdeMedio,
        onPrimary: dark ? AcordeColors.verdeProfundo : AcordeColors.branco,
        surface: dark ? AcordeColors.preto : AcordeColors.offWhite,
        onSurface: dark ? AcordeColors.offWhite : AcordeColors.grafite,
        onSurfaceVariant: dark ? AcordeColors.borda : AcordeColors.cinza,
        secondaryContainer: dark
            ? AcordeColors.verdeProfundo
            : AcordeColors.verdeClaro,
        onSecondaryContainer: dark
            ? AcordeColors.offWhite
            : AcordeColors.verdeProfundo,
        outlineVariant: dark ? AcordeColors.verdeMedio : AcordeColors.borda,
      );
  final canvas = dark
      ? AcordeColors.preto
      : Color.lerp(AcordeColors.offWhite, AcordeColors.verdeClaro, .55)!;
  final surface = dark ? scheme.surfaceContainerLow : AcordeColors.branco;
  final base = ThemeData(colorScheme: scheme, fontFamily: 'Inter');
  final heading = TextStyle(
    color: scheme.onSurface,
    fontFamily: 'Manrope',
    fontWeight: FontWeight.w800,
    fontVariations: [FontVariation.weight(800)],
    height: 1.2,
  );
  final outline = OutlineInputBorder(
    borderRadius: BorderRadius.circular(10),
    borderSide: BorderSide(color: scheme.outline),
  );
  return base.copyWith(
    scaffoldBackgroundColor: canvas,
    appBarTheme: AppBarThemeData(
      backgroundColor: canvas,
      surfaceTintColor: Colors.transparent,
      titleTextStyle: heading.copyWith(fontSize: 19),
    ),
    textTheme: base.textTheme.copyWith(
      headlineMedium: heading.copyWith(fontSize: 28),
      headlineSmall: heading.copyWith(fontSize: 23),
      titleLarge: heading.copyWith(fontSize: 19),
      titleMedium: heading.copyWith(fontSize: 16),
      bodyMedium: base.textTheme.bodyMedium?.copyWith(height: 1.5),
    ),
    inputDecorationTheme: InputDecorationThemeData(
      filled: true,
      fillColor: surface,
      border: outline,
      enabledBorder: outline,
      contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 15),
      floatingLabelBehavior: FloatingLabelBehavior.always,
    ),
    filledButtonTheme: FilledButtonThemeData(
      style: FilledButton.styleFrom(
        minimumSize: const Size(0, 48),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
        textStyle: const TextStyle(
          fontWeight: FontWeight.w700,
          fontVariations: [FontVariation.weight(700)],
        ),
      ),
    ),
    cardTheme: CardThemeData(
      color: surface,
      surfaceTintColor: Colors.transparent,
      elevation: 0,
      margin: EdgeInsets.zero,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(14),
        side: BorderSide(color: scheme.outlineVariant),
      ),
    ),
    navigationBarTheme: NavigationBarThemeData(
      backgroundColor: surface,
      surfaceTintColor: Colors.transparent,
      indicatorColor: scheme.secondaryContainer,
      height: 66,
    ),
  );
}
