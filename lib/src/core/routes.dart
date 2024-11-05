// lib/src/core/routes.dart

import 'package:flutter/cupertino.dart';
import 'package:flutter/foundation.dart';
import 'package:chaoffice/src/screens/home_screen.dart';
import 'package:chaoffice/src/screens/dashboard_screen.dart';
import 'package:chaoffice/src/screens/login_screen.dart';
import 'package:chaoffice/src/screens/plugin_screen.dart';
import 'package:chaoffice/src/screens/plugin_list_screen.dart';

class AppRoutes {
  static const String login = '/login';
  static const String home = '/';
  static const String dashboard = '/dashboard';
  static const String plugin = '/plugin';
  static const String pluginList = '/plugin-list';

  static Route<dynamic> generateRoute(RouteSettings settings) {
    if (kDebugMode) {
      print("Generating route for: ${settings.name}");
    }

    switch (settings.name) {
      case login:
        return CupertinoPageRoute(builder: (_) {
          if (kDebugMode) {
            print("Building LoginScreen");
          }
          return const LoginScreen();
        });

      case home:
        return CupertinoPageRoute(builder: (_) {
          if (kDebugMode) {
            print("Building HomeScreen");
          }
          return const HomeScreen();
        });

      case dashboard:
        return CupertinoPageRoute(builder: (_) {
          if (kDebugMode) {
            print("Building DashboardScreen");
          }
          return const DashboardScreen();
        });

      case pluginList:
        return CupertinoPageRoute(builder: (_) {
          if (kDebugMode) {
            print("Building PluginListScreen");
          }
          return const PluginListScreen();
        });

      case plugin:
        final args = settings.arguments as Map<String, dynamic>?;
        final pluginName = args?['pluginName'] as String? ?? '';
        return CupertinoPageRoute(builder: (_) {
          if (kDebugMode) {
            print("Building PluginScreen for: $pluginName");
          }
          return PluginScreen(pluginName: pluginName);
        });

      default:
        if (kDebugMode) {
          print("Route not found: ${settings.name}");
        }
        return CupertinoPageRoute(
          builder: (_) => CupertinoPageScaffold(
            navigationBar: const CupertinoNavigationBar(
              middle: Text('Not Found'),
            ),
            child: Center(
              child: Text('Route ${settings.name} not found'),
            ),
          ),
        );
    }
  }
}
