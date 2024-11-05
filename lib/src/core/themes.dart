// lib/core/themes.dart

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

class AppThemes {
  static const Color _primaryColor = CupertinoColors.systemBlue;

  static const CupertinoThemeData lightTheme = CupertinoThemeData(
    brightness: Brightness.light,
    primaryColor: _primaryColor,
    primaryContrastingColor: CupertinoColors.white,
    scaffoldBackgroundColor: CupertinoColors.systemBackground,
    barBackgroundColor: CupertinoColors.systemBackground,
    textTheme: CupertinoTextThemeData(
      textStyle: TextStyle(inherit: false, color: CupertinoColors.black),
      actionTextStyle: TextStyle(inherit: false, color: _primaryColor),
      navTitleTextStyle: TextStyle(
        inherit: false,
        color: CupertinoColors.black,
        fontSize: 18,
        fontWeight: FontWeight.bold,
      ),
    ),
  );

  static final CupertinoThemeData darkTheme = CupertinoThemeData(
    brightness: Brightness.dark,
    primaryColor: _primaryColor,
    primaryContrastingColor: CupertinoColors.black,
    scaffoldBackgroundColor: CupertinoColors.systemBackground.darkColor,
    barBackgroundColor: CupertinoColors.systemBackground.darkColor,
    textTheme: const CupertinoTextThemeData(
      textStyle: TextStyle(inherit: false, color: CupertinoColors.white),
      actionTextStyle: TextStyle(inherit: false, color: _primaryColor),
      navTitleTextStyle: TextStyle(
        inherit: false,
        color: CupertinoColors.white,
        fontSize: 18,
        fontWeight: FontWeight.bold,
      ),
    ),
  );

  static CupertinoThemeData getTheme(Brightness brightness) {
    return brightness == Brightness.light ? lightTheme : darkTheme;
  }

  static TextStyle getTextStyle(context) {
    return getTheme(Theme.of(context).brightness).textTheme.textStyle;
  }
}
